package com.itways.assistant.upload_engine_sdk.service;

import com.itways.assistant.upload_engine_sdk.dto.UploadResponse;
import org.springframework.web.multipart.MultipartFile;


public interface UploadService {
    UploadResponse upload(MultipartFile file , String folder) ;
}
