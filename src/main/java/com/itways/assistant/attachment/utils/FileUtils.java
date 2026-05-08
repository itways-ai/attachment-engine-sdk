package com.itways.assistant.attachment.utils;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

public class FileUtils {

	public static String detectContentType(String fileName) {
		String contentType = "application/octet-stream";
		try {
			String probed = Files.probeContentType(Path.of(fileName));
			if (probed != null) {
				contentType = probed;
			}
		} catch (Exception ignored) {
		}
		return contentType;
	}

	public static MultipartFile toMultipartFile(byte[] bytes, String fileName) {
		String safeName = safeFileName(fileName);
		String contentType = detectContentType(safeName);
		return new ByteArrayMultipartFile(bytes, safeName, safeName, contentType);
	}

	public static String safeFileName(String name) {
		if (name == null || name.isBlank()) {
			return "file_" + System.currentTimeMillis();
		}
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex > 0) {
			String baseName = name.substring(0, dotIndex);
			String extension = name.substring(dotIndex + 1); // without the dot
			String safeBase = baseName.replaceAll("[^a-zA-Z0-9\\-_]", "_");
			String safeExt = extension.replaceAll("[^a-zA-Z0-9]", "");
			return safeBase + "." + safeExt;
		}
		// No extension — sanitize the whole name
		return name.replaceAll("[^a-zA-Z0-9\\-_]", "_");
	}
}
