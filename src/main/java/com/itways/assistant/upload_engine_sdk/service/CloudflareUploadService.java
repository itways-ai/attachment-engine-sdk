package com.itways.assistant.upload_engine_sdk.service;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.itways.assistant.upload_engine_sdk.config.CloudFlareR2Config;
import com.itways.assistant.upload_engine_sdk.dto.UploadResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CloudflareUploadService implements UploadService {

    private final AmazonS3 amazonS3;
    private final CloudFlareR2Config config;

    @Override
    public UploadResponse upload(MultipartFile file, String folder) {
        String fileName = folder + "/" + file.getOriginalFilename();
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            amazonS3.putObject(new PutObjectRequest(
                    config.getBucket(),
                    fileName,
                    file.getInputStream(),
                    metadata).withCannedAcl(CannedAccessControlList.PublicRead));
            String url = amazonS3.getUrl(config.getBucket(), fileName).toString();
            return new UploadResponse(fileName, url, true, "Uploaded successfully");
        } catch (Exception e) {
            return new UploadResponse(fileName, null, false, "Upload failed: " + e.getMessage());
        }
    }
}
