package com.itways.assistant.upload_engine_sdk.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cloudflare.r2")
@Data
public class CloudFlareR2Config {
    private String accessKey;
    private String secretKey;
    private String accountId;
    private String publicDomain;
    private String bucket;
    private String publicBaseUrl;
}