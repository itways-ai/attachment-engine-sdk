package com.itways.assistant.attachment;

import org.springframework.context.annotation.Import;

import com.itways.assistant.attachment.config.UploadAutoConfiguration;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(UploadAutoConfiguration.class)
public @interface EnableAttachment {
}
