package com.itways.assistant.attachment.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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

	private final AmazonS3 amazonS3;
	private final CloudFlareR2Config config;

	@Override
	public <T> UploadResponse upload(String fileName, byte[] bytes) throws Exception {
		// Wrap bytes into MultipartFile
		MultipartFile file = FileUtils.toMultipartFile(bytes, fileName);

		try {
			ObjectMetadata metadata = new ObjectMetadata();
			metadata.setContentLength(file.getSize());
			metadata.setContentType(file.getContentType());
			amazonS3.putObject(new PutObjectRequest(config.getBucket(), fileName, file.getInputStream(), metadata));
			// TODO return public URL - Profili site
			String publicUrl = buildPublicUrl(fileName);
			return new UploadResponse(fileName, publicUrl, true, "Uploaded successfully");
		} catch (Exception e) {
			throw new Exception("Upload failed: " + e.getMessage());

		}
	}

	@Override
	public String get(String url) throws Exception {
		try {
			HttpClient client = HttpClient.newHttpClient();
			HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			if (response.statusCode() != 200) {
				throw new Exception("Failed to fetch content from storage");
			}
			return response.body();
		} catch (Exception e) {
			throw new Exception("Error fetching template content: " + e.getMessage());
		}
	}

	private String buildPublicUrl(String fileName) {
		// Prefer publicBaseUrl if present, fallback to publicDomain
		if (config.getPublicBaseUrl() != null && !config.getPublicBaseUrl().isBlank()) {
			return config.getPublicBaseUrl() + "/" + fileName;
		}
		if (config.getPublicDomain() != null && !config.getPublicDomain().isBlank()) {
			return "https://" + config.getPublicDomain() + "/" + fileName;
		}

		throw new IllegalArgumentException("No publicBaseUrl or publicDomain configured for Cloudflare R2");
	}

}