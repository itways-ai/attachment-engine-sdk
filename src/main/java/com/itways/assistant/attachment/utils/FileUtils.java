package com.itways.assistant.attachment.utils;

import java.nio.file.Files;
import java.nio.file.Path;

import org.springframework.web.multipart.MultipartFile;

public class FileUtils {

	public static MultipartFile toMultipartFile(byte[] bytes, String fileName) {
		String safeName = safeFileName(fileName);
		String contentType = "text/plain";
		try {
			contentType = Files.probeContentType(Path.of(safeName));
		} catch (Exception ignored) {
		}

		return new ByteArrayMultipartFile(bytes, safeName, safeName, contentType != null ? contentType : "contentType"

		);
	}

	private static String safeFileName(String name) {
		return name.replaceAll("[^a-zA-Z0-9\\-_]", "_");
	}
}
