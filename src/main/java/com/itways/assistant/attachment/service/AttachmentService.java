package com.itways.assistant.attachment.service;

import com.itways.assistant.attachment.dto.UploadResponse;

public interface AttachmentService {

	<T> UploadResponse upload(String fileName, byte[] bytes) throws Exception;

	String get(String url) throws Exception;
}
