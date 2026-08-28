package com.itways.assistant.attachment.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.itways.assistant.attachment.config.CloudFlareR2Config;
import com.itways.assistant.attachment.dto.UploadResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The R2 adapter's contract: reject bad payloads before any network call,
 * write the sanitized key with correct metadata, and hand back the public
 * URL the rest of the platform will store forever. The 50MB guard is left
 * unexercised on purpose — triggering it requires allocating a >50MB array
 * in the test JVM for one branch; the null/empty guards pin the same
 * fail-before-upload behavior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CloudflareAttachmentService")
class CloudflareAttachmentServiceTest {

    @Mock
    private AmazonS3 amazonS3;
    @Captor
    private ArgumentCaptor<PutObjectRequest> putRequest;

    private static CloudFlareR2Config configWithBaseUrl() {
        CloudFlareR2Config config = new CloudFlareR2Config();
        config.setBucket("attachments");
        config.setPublicBaseUrl("https://cdn.example.com/files");
        return config;
    }

    @Nested
    @DisplayName("upload")
    class Upload {

        @Test
        @DisplayName("puts the sanitized key into the configured bucket with content-length metadata")
        void happyPath() {
            CloudflareAttachmentService service = new CloudflareAttachmentService(amazonS3, configWithBaseUrl());
            byte[] bytes = "attachment body".getBytes(StandardCharsets.UTF_8);

            UploadResponse response = service.upload("my report.pdf", bytes);

            verify(amazonS3).putObject(putRequest.capture());
            PutObjectRequest request = putRequest.getValue();
            assertThat(request.getBucketName()).isEqualTo("attachments");
            assertThat(request.getKey()).isEqualTo("my_report.pdf");
            assertThat(request.getMetadata().getContentLength()).isEqualTo(bytes.length);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getFileName()).isEqualTo("my_report.pdf");
            assertThat(response.getUrl()).isEqualTo("https://cdn.example.com/files/my_report.pdf");
            assertThat(response.getMessage()).isEqualTo("Uploaded successfully");
        }

        @Test
        @DisplayName("null or empty content is rejected before anything touches the bucket")
        void emptyContentRejected() {
            CloudflareAttachmentService service = new CloudflareAttachmentService(amazonS3, configWithBaseUrl());

            assertThatThrownBy(() -> service.upload("f.txt", null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> service.upload("f.txt", new byte[0]))
                    .isInstanceOf(IllegalArgumentException.class);
            verifyNoInteractions(amazonS3);
        }

        @Test
        @DisplayName("an S3 failure surfaces as a RuntimeException carrying the cause")
        void s3FailureWrapped() {
            CloudflareAttachmentService service = new CloudflareAttachmentService(amazonS3, configWithBaseUrl());
            when(amazonS3.putObject(any(PutObjectRequest.class)))
                    .thenThrow(new RuntimeException("R2 unreachable"));

            assertThatThrownBy(() -> service.upload("f.txt", new byte[] { 1, 2, 3 }))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Upload failed")
                    .hasMessageContaining("R2 unreachable");
        }
    }

    @Nested
    @DisplayName("public URL")
    class PublicUrl {

        @Test
        @DisplayName("falls back to publicDomain, prefixing https:// when the scheme is missing")
        void publicDomainFallback() {
            CloudFlareR2Config config = new CloudFlareR2Config();
            config.setBucket("attachments");
            config.setPublicDomain("cdn.example.com");
            CloudflareAttachmentService service = new CloudflareAttachmentService(amazonS3, config);

            assertThat(service.upload("f.txt", new byte[] { 1 }).getUrl())
                    .isEqualTo("https://cdn.example.com/f.txt");
        }

        @Test
        @DisplayName("a publicDomain that already carries a scheme is used verbatim")
        void schemedDomainVerbatim() {
            CloudFlareR2Config config = new CloudFlareR2Config();
            config.setBucket("attachments");
            config.setPublicDomain("http://localhost:9000");
            CloudflareAttachmentService service = new CloudflareAttachmentService(amazonS3, config);

            assertThat(service.upload("f.txt", new byte[] { 1 }).getUrl())
                    .isEqualTo("http://localhost:9000/f.txt");
        }

        @Test
        @DisplayName("no public URL config fails the upload — after the object was already written")
        void missingConfigFailsAfterWrite() {
            // NOTE: possible defect — buildPublicUrl runs after putObject, inside
            // the same try/catch, so a missing publicBaseUrl/publicDomain throws
            // "Upload failed: No publicBaseUrl..." AFTER the bytes landed in the
            // bucket: every such upload orphans an object in R2. The config check
            // belongs before the put. Pinned as current behavior.
            CloudFlareR2Config config = new CloudFlareR2Config();
            config.setBucket("attachments");
            CloudflareAttachmentService service = new CloudflareAttachmentService(amazonS3, config);

            assertThatThrownBy(() -> service.upload("f.txt", new byte[] { 1 }))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Upload failed")
                    .hasMessageContaining("No publicBaseUrl or publicDomain");
            verify(amazonS3).putObject(any(PutObjectRequest.class));
        }
    }

    @Nested
    @DisplayName("get")
    class Get {

        @Test
        @DisplayName("returns the body of a 200 response")
        void okBody() throws Exception {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            byte[] body = "stored-content".getBytes(StandardCharsets.UTF_8);
            server.createContext("/file.txt", exchange -> {
                exchange.sendResponseHeaders(200, body.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(body);
                }
            });
            server.start();
            try {
                CloudflareAttachmentService service = new CloudflareAttachmentService(amazonS3, configWithBaseUrl());
                String url = "http://localhost:" + server.getAddress().getPort() + "/file.txt";

                assertThat(service.get(url)).isEqualTo("stored-content");
            } finally {
                server.stop(0);
            }
        }

        @Test
        @DisplayName("a non-200 status is a RuntimeException, not a body")
        void non200Rejected() throws Exception {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
            server.createContext("/missing.txt", exchange -> {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
            });
            server.start();
            try {
                CloudflareAttachmentService service = new CloudflareAttachmentService(amazonS3, configWithBaseUrl());
                String url = "http://localhost:" + server.getAddress().getPort() + "/missing.txt";

                assertThatThrownBy(() -> service.get(url))
                        .isInstanceOf(RuntimeException.class)
                        .hasMessageContaining("Error fetching content from storage");
            } finally {
                server.stop(0);
            }
        }
    }
}
