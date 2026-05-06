package com.itways.assistant.attachment.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.itways.assistant.attachment.config.CloudFlareR2Config;
import com.itways.assistant.attachment.dto.UploadResponse;
import com.itways.assistant.attachment.service.AttachmentService;
import com.itways.assistant.attachment.utils.FileUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CloudflareAttachmentService implements AttachmentService {

	private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

	private final AmazonS3 amazonS3;
	private final CloudFlareR2Config config;

	@Override
	public UploadResponse upload(String fileName, byte[] bytes) {
		if (bytes == null || bytes.length == 0) {
			throw new IllegalArgumentException("File content must not be empty");
		}
		if (bytes.length > 50 * 1024 * 1024) { // 50 MB hard limit
			throw new IllegalArgumentException("File size exceeds the maximum allowed limit of 50MB");
		}
		String safeFileName = FileUtils.safeFileName(fileName);
		String contentType = FileUtils.detectContentType(safeFileName);

		try {
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(bytes.length);
			metadata.setContentType(contentType);
			amazonS3.putObject(new PutObjectRequest(
					config.getBucket(),
					safeFileName,
					new java.io.ByteArrayInputStream(bytes),
					metadata));
			String publicUrl = buildPublicUrl(safeFileName);
			return new UploadResponse(safeFileName, publicUrl, true, "Uploaded successfully");
		} catch (Exception e) {
			throw new RuntimeException("Upload failed: " + e.getMessage(), e);
		}
	}

	@Override
	public String get(String url) {
		try {
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
			HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new Exception("Failed to fetch content from storage");
			}
			return response.body();
		} catch (Exception e) {
			throw new RuntimeException("Error fetching content from storage: " + e.getMessage(), e);
		}
	}

	private String buildPublicUrl(String fileName) {
		// Prefer publicBaseUrl if present, fallback to publicDomain
		if (config.getPublicBaseUrl() != null && !config.getPublicBaseUrl().isBlank()) {
			return config.getPublicBaseUrl() + "/" + fileName;
		}
		if (config.getPublicDomain() != null && !config.getPublicDomain().isBlank()) {
			String domain = config.getPublicDomain();
			if (domain.startsWith("http://") || domain.startsWith("https://")) {
				return domain + "/" + fileName;
			}
			return "https://" + domain + "/" + fileName;
		}

		throw new IllegalArgumentException("No publicBaseUrl or publicDomain configured for Cloudflare R2");
	}

}