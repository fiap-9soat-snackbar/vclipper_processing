package com.vclipper.processing.infrastructure.controllers.dto;

import com.vclipper.processing.domain.entity.ProcessingStatus;

import java.time.LocalDateTime;

/**
 * Response DTO for video status update operations
 */
public record VideoStatusUpdateResponse(
    String videoId,
    String userId,
    ProcessingStatus previousStatus,
    ProcessingStatus newStatus,
    String processedFileS3Key,
    LocalDateTime updatedAt,
    String message,
    boolean success
) {
    
    /**
     * Factory method for successful status update
     */
    public static VideoStatusUpdateResponse success(
            String videoId,
            String userId,
            ProcessingStatus previousStatus,
            ProcessingStatus newStatus,
            String processedFileS3Key,
            LocalDateTime updatedAt) {
        
        String message = String.format("Video %s status updated from %s to %s", 
            videoId, previousStatus.value(), newStatus.value());
            
        return new VideoStatusUpdateResponse(
            videoId,
            userId,
            previousStatus,
            newStatus,
            processedFileS3Key,
            updatedAt,
            message,
            true
        );
    }
    
    /**
     * Factory method for failed status update
     */
    public static VideoStatusUpdateResponse failure(
            String videoId,
            String userId,
            ProcessingStatus currentStatus,
            String errorMessage) {
        
        return new VideoStatusUpdateResponse(
            videoId,
            userId,
            currentStatus,
            currentStatus,
            null,
            LocalDateTime.now(),
            errorMessage,
            false
        );
    }
}
