package com.itways.assistant.attachment.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@ComponentScan("com.itways.assistant.attachment")
public class UploadAutoConfiguration {

	private final CloudFlareR2Config cloudflareR2Config;

	public UploadAutoConfiguration(CloudFlareR2Config cloudflareR2Config) {
		this.cloudflareR2Config = cloudflareR2Config;
	}

	@PostConstruct
	public void print() {
		log.info("✅ Attachment SDK configuration initialized");
	}

	@Bean
	public AmazonS3 amazonS3() {
		String endpoint = String.format("https://%s.r2.cloudflarestorage.com", cloudflareR2Config.getAccountId());
		BasicAWSCredentials credentials = new BasicAWSCredentials(cloudflareR2Config.getAccessKey(),
				cloudflareR2Config.getSecretKey());

		return AmazonS3ClientBuilder.standard()
				.withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, "auto"))
				.withCredentials(new AWSStaticCredentialsProvider(credentials)).withPathStyleAccessEnabled(true)
				.build();
	}

}
