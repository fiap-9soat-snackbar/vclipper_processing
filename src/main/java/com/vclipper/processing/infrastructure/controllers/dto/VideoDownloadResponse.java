package com.vclipper.processing.infrastructure.controllers.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.vclipper.processing.application.usecases.GetVideoDownloadUrlUseCase;

import java.time.LocalDateTime;

/**
 * Response DTO for video download URL requests
 * Contains the presigned download URL and metadata
 */
public record VideoDownloadResponse(
    @JsonProperty("videoId")
    String videoId,
    
    @JsonProperty("originalFilename") 
    String originalFilename,
    
    @JsonProperty("downloadUrl")
    String downloadUrl,
    
    @JsonProperty("expirationMinutes")
    int expirationMinutes,
    
    @JsonProperty("expiresAt")
    LocalDateTime expiresAt,
    
    @JsonProperty("message")
    String message,
    
    @JsonProperty("success")
    boolean success
) {
    
    /**
     * Create response from use case result
     */
    public static VideoDownloadResponse from(GetVideoDownloadUrlUseCase.DownloadUrlResponse useCaseResponse) {
        return new VideoDownloadResponse(
            useCaseResponse.videoId(),
            useCaseResponse.originalFilename(),
            useCaseResponse.downloadUrl(),
            useCaseResponse.expirationMinutes(),
            LocalDateTime.now().plusMinutes(useCaseResponse.expirationMinutes()),
            "Download URL generated successfully",
            true
        );
    }
    
    /**
     * Create error response
     */
    public static VideoDownloadResponse error(String videoId, String message) {
        return new VideoDownloadResponse(
            videoId,
            null,
            null,
            0,
            null,
            message,
            false
        );
    }
    
    /**
     * Create error response with error code
     */
    public static VideoDownloadResponse error(String videoId, String errorCode, String message) {
        return new VideoDownloadResponse(
            videoId,
            null,
            null,
            0,
            null,
            message,
            false
        );
    }
}
