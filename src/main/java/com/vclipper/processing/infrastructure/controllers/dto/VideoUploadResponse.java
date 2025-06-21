package com.vclipper.processing.infrastructure.controllers.dto;

import com.vclipper.processing.domain.entity.ProcessingStatus;

import java.time.LocalDateTime;

/**
 * DTO for video upload response
 * Returns upload confirmation with processing details
 */
public record VideoUploadResponse(
    String videoId,
    String userId,
    String originalFilename,
    ProcessingStatus status,
    String message,
    LocalDateTime uploadedAt,
    boolean success
) {
    /**
     * Create successful upload response
     */
    public static VideoUploadResponse success(String videoId, String userId, String originalFilename, 
                                            ProcessingStatus status, String message, LocalDateTime uploadedAt) {
        return new VideoUploadResponse(videoId, userId, originalFilename, status, message, uploadedAt, true);
    }
    
    /**
     * Create failed upload response
     */
    public static VideoUploadResponse failure(String userId, String originalFilename, String message) {
        return new VideoUploadResponse(null, userId, originalFilename, null, message, LocalDateTime.now(), false);
    }
}
