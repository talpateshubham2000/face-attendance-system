package com.attendance.facesystem.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient bean used by FaceRecognitionClient to talk to the
 * Python FastAPI microservice (face detection / recognition).
 */
@Configuration
public class WebClientConfig {

    @Value("${python.service.base-url}")
    private String pythonServiceBaseUrl;

    @Bean
    public WebClient pythonWebClient() {
        return WebClient.builder()
                .baseUrl(pythonServiceBaseUrl)
                .codecs(clientCodecConfigurer ->
                        clientCodecConfigurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10MB for base64 images
                .build();
    }
}
