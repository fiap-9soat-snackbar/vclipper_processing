package com.vclipper.processing.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for video processing
 * Maps values from application.yml to strongly-typed configuration
 */
@ConfigurationProperties(prefix = "vclipper.processing")
public record ProcessingProperties(
    Video video,
    Download download,
    Retry retry
) {
    
    public record Video(
        long maxSizeBytes,
        String allowedFormats
    ) {}
    
    public record Download(
        int urlExpirationMinutes
    ) {}
    
    public record Retry(
        int maxAttempts,
        int initialDelaySeconds,
        int maxDelaySeconds
    ) {}
}
