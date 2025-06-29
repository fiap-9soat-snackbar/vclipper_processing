package com.vclipper.processing.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web configuration for CORS and other web-related settings
 * Follows Clean Architecture - Infrastructure layer configuration
 * Follows existing backend pattern of environment-based configuration
 */
@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(WebConfiguration.class);

    @Value("${CORS_ALLOWED_ORIGINS}")
    private String allowedOriginsString;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] allowedOrigins = parseAllowedOrigins();
        
        logger.info("🌐 Configuring CORS with allowed origins: {}", String.join(", ", allowedOrigins));

        // Configure CORS for API endpoints
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        // Configure CORS for actuator endpoints (health checks, etc.)
        registry.addMapping("/actuator/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        logger.info("✅ CORS configuration completed");
    }

    /**
     * Parse comma-separated allowed origins from environment variable
     * Fails fast if CORS_ALLOWED_ORIGINS is not configured
     */
    private String[] parseAllowedOrigins() {
        if (allowedOriginsString == null || allowedOriginsString.trim().isEmpty()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS environment variable is required but not configured");
        }

        String[] origins = allowedOriginsString.split(",");
        for (int i = 0; i < origins.length; i++) {
            origins[i] = origins[i].trim();
        }

        return origins;
    }
}
