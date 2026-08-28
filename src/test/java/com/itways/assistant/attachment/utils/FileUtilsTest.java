package com.itways.assistant.attachment.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * safeFileName is the last line between a caller-supplied name and the R2
 * object key: whatever survives it goes verbatim into the bucket and into
 * the public URL. detectContentType leans on the OS MIME table
 * (Files.probeContentType), so only its octet-stream fallback is asserted
 * here — known-extension answers vary by machine.
 */
@DisplayName("FileUtils")
class FileUtilsTest {

    @Nested
    @DisplayName("safeFileName")
    class SafeFileName {

        @Test
        @DisplayName("replaces everything outside [a-zA-Z0-9-_] in the base name and keeps the extension")
        void sanitizesBaseName() {
            assertThat(FileUtils.safeFileName("my report (final).pdf")).isEqualTo("my_report__final_.pdf");
            assertThat(FileUtils.safeFileName("Q3–2026 čísla.xlsx")).matches("[a-zA-Z0-9\\-_]+\\.xlsx");
            assertThat(FileUtils.safeFileName("clean-name_1.PDF")).isEqualTo("clean-name_1.PDF");
        }

        @Test
        @DisplayName("strips non-alphanumerics from the extension itself")
        void sanitizesExtension() {
            assertThat(FileUtils.safeFileName("report.p df")).isEqualTo("report.pdf");
        }

        @Test
        @DisplayName("a name without an extension is sanitized whole")
        void noExtension() {
            assertThat(FileUtils.safeFileName("read me!")).isEqualTo("read_me_");
            // A leading-dot name has dotIndex 0, so it takes the whole-name
            // branch and the dot itself is replaced.
            assertThat(FileUtils.safeFileName(".gitignore")).isEqualTo("_gitignore");
        }

        @Test
        @DisplayName("path traversal attempts come out with no separators or dots left")
        void traversalNeutralized() {
            String safe = FileUtils.safeFileName("../../etc/passwd");

            assertThat(safe).doesNotContain("/").doesNotContain("\\").doesNotContain("..");
            assertThat(safe).matches("[a-zA-Z0-9\\-_]+\\.[a-zA-Z0-9]*");
        }

        @Test
        @DisplayName("null or blank names fall back to a file_<millis> placeholder")
        void blankFallback() {
            assertThat(FileUtils.safeFileName(null)).startsWith("file_").matches("file_\\d+");
            assertThat(FileUtils.safeFileName("   ")).startsWith("file_").matches("file_\\d+");
        }

        @Test
        @DisplayName("two uploads of the same file name produce the same key — the second overwrites the first")
        void noUniquenessForNamedFiles() {
            // NOTE: possible defect — sanitization is deterministic and nothing
            // appends a uuid/timestamp for non-blank names, so two tenants (or
            // the same tenant twice) uploading "report.pdf" write the same R2
            // object key and the second upload silently replaces the first —
            // while old links keep serving the new content. Only the blank-name
            // fallback gets a unique-ish millis suffix. Pinned via equal outputs.
            assertThat(FileUtils.safeFileName("report.pdf"))
                    .isEqualTo(FileUtils.safeFileName("report.pdf"));
        }
    }

    @Nested
    @DisplayName("detectContentType")
    class DetectContentType {

        @Test
        @DisplayName("an unknown extension falls back to application/octet-stream")
        void unknownExtensionFallsBack() {
            assertThat(FileUtils.detectContentType("data.zzqx-unknown")).isEqualTo("application/octet-stream");
            assertThat(FileUtils.detectContentType("no_extension_at_all")).isEqualTo("application/octet-stream");
        }
    }

    @Nested
    @DisplayName("toMultipartFile and ByteArrayMultipartFile")
    class MultipartContract {

        @Test
        @DisplayName("wraps the bytes under the sanitized name with size and content intact")
        void wrapsBytes() throws Exception {
            byte[] bytes = "hello attachment".getBytes(StandardCharsets.UTF_8);

            MultipartFile file = FileUtils.toMultipartFile(bytes, "my file.txt");

            assertThat(file.getName()).isEqualTo("my_file.txt");
            assertThat(file.getOriginalFilename()).isEqualTo("my_file.txt");
            assertThat(file.getSize()).isEqualTo(bytes.length);
            assertThat(file.getBytes()).isEqualTo(bytes);
            assertThat(file.isEmpty()).isFalse();
            assertThat(file.getInputStream().readAllBytes()).isEqualTo(bytes);
        }

        @Test
        @DisplayName("empty content reports isEmpty and size zero")
        void emptyContent() {
            MultipartFile file = FileUtils.toMultipartFile(new byte[0], "empty.txt");

            assertThat(file.isEmpty()).isTrue();
            assertThat(file.getSize()).isZero();
        }

        @Test
        @DisplayName("a null content type defaults to application/octet-stream")
        void nullContentTypeDefaults() {
            ByteArrayMultipartFile file = new ByteArrayMultipartFile(new byte[] { 1 }, "f", "f", null);

            assertThat(file.getContentType()).isEqualTo("application/octet-stream");
        }
    }
}
