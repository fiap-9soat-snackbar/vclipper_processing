package com.vclipper.processing.infrastructure.config;

import com.vclipper.processing.application.ports.*;
import com.vclipper.processing.application.usecases.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for application use cases
 * This is where we wire up the clean architecture components with Spring
 */
@Configuration
@EnableConfigurationProperties(ProcessingProperties.class)
public class UseCaseConfiguration {
    
    @Bean
    public SubmitVideoProcessingUseCase submitVideoProcessingUseCase(
            VideoRepositoryPort videoRepository,
            FileStoragePort fileStorage,
            MessageQueuePort messageQueue,
            NotificationPort notification,
            MimeTypeDetectionPort mimeTypeDetection,
            ProcessingProperties properties) {
        return new SubmitVideoProcessingUseCase(
            videoRepository,
            fileStorage,
            messageQueue,
            notification,
            mimeTypeDetection,
            properties.video().maxSizeBytes()
        );
    }
    
    @Bean
    public GetProcessingStatusUseCase getProcessingStatusUseCase(
            VideoRepositoryPort videoRepository) {
        return new GetProcessingStatusUseCase(videoRepository);
    }
    
    @Bean
    public ListUserVideosUseCase listUserVideosUseCase(
            VideoRepositoryPort videoRepository) {
        return new ListUserVideosUseCase(videoRepository);
    }
    
    @Bean
    public GetVideoDownloadUrlUseCase getVideoDownloadUrlUseCase(
            VideoRepositoryPort videoRepository,
            FileStoragePort fileStorage,
            ProcessingProperties properties) {
        return new GetVideoDownloadUrlUseCase(
            videoRepository,
            fileStorage,
            properties.download().urlExpirationMinutes()
        );
    }
    
    @Bean
    public UpdateProcessingStatusUseCase updateProcessingStatusUseCase(
            VideoRepositoryPort videoRepository,
            NotificationPort notification) {
        return new UpdateProcessingStatusUseCase(videoRepository, notification);
    }
}
