package com.itways.assistant.upload_engine_sdk;

import com.itways.assistant.upload_engine_sdk.config.CloudFlareR2Config;
import com.itways.assistant.upload_engine_sdk.config.UploadAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(UploadAutoConfiguration.class)
public @interface EnableUpload {
}
