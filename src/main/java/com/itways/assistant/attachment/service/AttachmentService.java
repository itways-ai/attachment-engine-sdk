package com.itways.assistant.attachment.service;

import com.itways.assistant.attachment.dto.UploadResponse;

public interface AttachmentService {

	UploadResponse upload(String fileName, byte[] bytes);

	String get(String url);
}
