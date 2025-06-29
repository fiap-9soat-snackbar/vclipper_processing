package com.vclipper.processing.infrastructure.controllers.dto;

import com.vclipper.processing.domain.entity.ProcessingStatus;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

/**
 * Request DTO for updating video processing status
 * Used by vclipping service to mark videos as completed
 */
public record VideoStatusUpdateRequest(
    @NotNull(message = "Status is required")
    ProcessingStatus status,
    
    String processedFileS3Key,
    
    LocalDateTime processingCompletedAt,
    
    String errorMessage
) {
    
    /**
     * Factory method for successful completion
     */
    public static VideoStatusUpdateRequest completed(String processedFileS3Key) {
        return new VideoStatusUpdateRequest(
            ProcessingStatus.COMPLETED,
            processedFileS3Key,
            LocalDateTime.now(),
            null
        );
    }
    
    /**
     * Factory method for processing failure
     */
    public static VideoStatusUpdateRequest failed(String errorMessage) {
        return new VideoStatusUpdateRequest(
            ProcessingStatus.FAILED,
            null,
            LocalDateTime.now(),
            errorMessage
        );
    }
    
    /**
     * Validation for business rules
     */
    public boolean isValid() {
        if (status == ProcessingStatus.COMPLETED) {
            return processedFileS3Key != null && !processedFileS3Key.trim().isEmpty();
        }
        if (status == ProcessingStatus.FAILED) {
            return errorMessage != null && !errorMessage.trim().isEmpty();
        }
        return true;
    }
}
