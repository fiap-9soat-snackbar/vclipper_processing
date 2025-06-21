package com.vclipper.processing.infrastructure.controllers.dto;

import com.vclipper.processing.application.usecases.GetProcessingStatusUseCase;
import com.vclipper.processing.domain.entity.ProcessingStatus;

import java.time.LocalDateTime;

/**
 * DTO for processing status response
 * Returns current processing status and details
 */
public record ProcessingStatusResponse(
    String videoId,
    String userId,
    ProcessingStatus status,
    String originalFilename,
    double fileSizeMB,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    String errorMessage,
    boolean isDownloadReady,
    boolean canRetry
) {
    /**
     * Create response from use case result
     */
    public static ProcessingStatusResponse from(GetProcessingStatusUseCase.ProcessingStatusResponse useCaseResponse) {
        return new ProcessingStatusResponse(
            useCaseResponse.videoId(),
            useCaseResponse.userId(),
            useCaseResponse.status(),
            useCaseResponse.originalFilename(),
            useCaseResponse.fileSizeMB(),
            useCaseResponse.createdAt(),
            useCaseResponse.updatedAt(),
            useCaseResponse.errorMessage(),
            useCaseResponse.isDownloadReady(),
            useCaseResponse.canRetry()
        );
    }
}
