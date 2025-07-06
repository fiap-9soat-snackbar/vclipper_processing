package com.vclipper.processing.infrastructure.controllers;

import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.common.VideoUploadError;
import com.vclipper.processing.application.usecases.GetProcessingStatusUseCase;
import com.vclipper.processing.application.usecases.GetVideoDownloadUrlUseCase;
import com.vclipper.processing.application.usecases.ListUserVideosUseCase;
import com.vclipper.processing.application.usecases.SubmitVideoProcessingUseCase;
import com.vclipper.processing.application.usecases.UpdateVideoStatusUseCase;
import com.vclipper.processing.domain.exceptions.InvalidVideoFormatException;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import com.vclipper.processing.domain.exceptions.VideoProcessingException;
import com.vclipper.processing.domain.exceptions.VideoUploadException;
import com.vclipper.processing.infrastructure.controllers.dto.ProcessingStatusResponse;
import com.vclipper.processing.infrastructure.controllers.dto.VideoDownloadResponse;
import com.vclipper.processing.infrastructure.controllers.dto.VideoListResponse;
import com.vclipper.processing.infrastructure.controllers.dto.VideoStatusUpdateRequest;
import com.vclipper.processing.infrastructure.controllers.dto.VideoStatusUpdateResponse;
import com.vclipper.processing.infrastructure.controllers.dto.VideoUploadRequest;
import com.vclipper.processing.infrastructure.controllers.dto.VideoUploadResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST controller for video processing operations
 * Handles video upload, status tracking, and listing
 */
@RestController
@RequestMapping("/api/videos")
@Validated
public class VideoProcessingController {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingController.class);
    
    private final SubmitVideoProcessingUseCase submitVideoProcessingUseCase;
    private final GetProcessingStatusUseCase getProcessingStatusUseCase;
    private final GetVideoDownloadUrlUseCase getVideoDownloadUrlUseCase;
    private final ListUserVideosUseCase listUserVideosUseCase;
    private final UpdateVideoStatusUseCase updateVideoStatusUseCase;
    
    public VideoProcessingController(SubmitVideoProcessingUseCase submitVideoProcessingUseCase,
                                   GetProcessingStatusUseCase getProcessingStatusUseCase,
                                   GetVideoDownloadUrlUseCase getVideoDownloadUrlUseCase,
                                   ListUserVideosUseCase listUserVideosUseCase,
                                   UpdateVideoStatusUseCase updateVideoStatusUseCase) {
        this.submitVideoProcessingUseCase = submitVideoProcessingUseCase;
        this.getProcessingStatusUseCase = getProcessingStatusUseCase;
        this.getVideoDownloadUrlUseCase = getVideoDownloadUrlUseCase;
        this.listUserVideosUseCase = listUserVideosUseCase;
        this.updateVideoStatusUseCase = updateVideoStatusUseCase;
    }
    
    /**
     * Upload video for processing
     */
    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @Valid @ModelAttribute VideoUploadRequest request,
            @RequestHeader("X-User-Id") @NotBlank(message = "User ID cannot be empty") String userId) {
        logger.info("Received video upload request for user: {}, filename: {}", 
            userId, request.getOriginalFilename());
        
        try {
            // Validate file
            if (!request.isFileValid()) {
                return ResponseEntity.badRequest()
                    .body(VideoUploadResponse.failure(userId, request.getOriginalFilename(), 
                        "Invalid file: file is empty or has no name"));
            }
            
            // Create use case submission
            SubmitVideoProcessingUseCase.VideoProcessingSubmission submission = 
                new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                    userId,
                    request.file().getInputStream(),
                    request.getOriginalFilename(),
                    request.getContentType(),
                    request.getFileSizeBytes()
                );
            
            // Execute use case with Result pattern
            Result<SubmitVideoProcessingUseCase.VideoProcessingResponse, VideoUploadError> result = 
                submitVideoProcessingUseCase.execute(submission);
            
            if (result.isSuccess()) {
                SubmitVideoProcessingUseCase.VideoProcessingResponse response = result.getValue().get();
                logger.info("Video upload successful for user: {}, videoId: {}", 
                    userId, response.videoId());
                
                return ResponseEntity.status(HttpStatus.CREATED)
                    .body(VideoUploadResponse.success(
                        response.videoId(),
                        userId,
                        request.getOriginalFilename(),
                        response.status(),
                        response.message(),
                        java.time.LocalDateTime.now()
                    ));
            } else {
                VideoUploadError error = result.getError().get();
                logger.warn("Video upload validation failed for user: {}, error: {}", 
                    userId, error.message());
                
                return ResponseEntity.badRequest()
                    .body(VideoUploadResponse.failure(userId, request.getOriginalFilename(), 
                        error.message()));
            }
            
        } catch (IOException e) {
            logger.error("IO error reading uploaded file for user: {} - {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VideoUploadResponse.failure(userId, request.getOriginalFilename(), 
                    "Error processing uploaded file"));
        } catch (VideoNotFoundException e) {
            // Let domain exceptions bubble up to GlobalExceptionHandler
            logger.debug("VideoNotFoundException during video upload for user: {} - re-throwing to GlobalExceptionHandler", userId);
            throw e;
        } catch (InvalidVideoFormatException e) {
            // Let domain exceptions bubble up to GlobalExceptionHandler
            logger.debug("InvalidVideoFormatException during video upload for user: {} - re-throwing to GlobalExceptionHandler", userId);
            throw e;
        } catch (VideoUploadException e) {
            // Let domain exceptions bubble up to GlobalExceptionHandler
            logger.debug("VideoUploadException during video upload for user: {} - re-throwing to GlobalExceptionHandler", userId);
            throw e;
        } catch (VideoProcessingException e) {
            // Let domain exceptions bubble up to GlobalExceptionHandler
            logger.debug("VideoProcessingException during video upload for user: {} - re-throwing to GlobalExceptionHandler", userId);
            throw e;
        } catch (Exception e) {
            // Only catch unexpected system errors (non-domain exceptions)
            logger.error("Unexpected system error during video upload for user: {} - {}", userId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VideoUploadResponse.failure(userId, request.getOriginalFilename(), 
                    "Internal server error"));
        }
    }
    
    /**
     * Get processing status for a video
     */
    @GetMapping("/{videoId}/status")
    public ResponseEntity<ProcessingStatusResponse> getProcessingStatus(
            @PathVariable String videoId,
            @RequestHeader("X-User-Id") @NotBlank(message = "User ID cannot be empty") String userId) {
        
        logger.info("Getting processing status for videoId: {}, userId: {}", videoId, userId);
        
        // Let domain exceptions (VideoNotFoundException) bubble up to GlobalExceptionHandler
        GetProcessingStatusUseCase.ProcessingStatusResponse response = 
            getProcessingStatusUseCase.execute(videoId, userId);
        
        return ResponseEntity.ok(ProcessingStatusResponse.from(response));
    }
    
    /**
     * List all videos for a user
     */
    @GetMapping
    public ResponseEntity<VideoListResponse> listUserVideos(@RequestHeader("X-User-Id") @NotBlank(message = "User ID cannot be empty") String userId) {
        logger.info("Listing videos for userId: {}", userId);
        
        // Let any exceptions bubble up to GlobalExceptionHandler
        var videoSummaries = listUserVideosUseCase.execute(userId);
        
        return ResponseEntity.ok(VideoListResponse.from(userId, videoSummaries));
    }
    
    /**
     * Get download URL for a processed video
     */
    @GetMapping("/{videoId}/download")
    public ResponseEntity<VideoDownloadResponse> getVideoDownloadUrl(
            @PathVariable String videoId,
            @RequestHeader("X-User-Id") @NotBlank(message = "User ID cannot be empty") String userId) {
        logger.info("Getting download URL for videoId: {}, userId: {}", videoId, userId);
        
        try {
            // Execute use case to get download URL (now returns Result)
            var result = getVideoDownloadUrlUseCase.execute(videoId, userId);
            
            if (result.isSuccess()) {
                logger.info("Successfully generated download URL for video: {}", videoId);
                return ResponseEntity.ok(VideoDownloadResponse.from(result.getValue().get()));
            } else {
                // Handle business error (video not ready) - this is expected behavior
                var error = result.getError().get();
                logger.info("Video not ready for download: videoId={}, status={}, operation={}", 
                    error.videoId(), error.currentStatus().value(), error.operation());
                
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(VideoDownloadResponse.error(
                        error.videoId(),
                        "VIDEO_NOT_READY",
                        error.message()
                    ));
            }
            
        } catch (VideoNotFoundException e) {
            // Security boundary - don't log details, let GlobalExceptionHandler handle
            logger.debug("Video not found or access denied: videoId={}, userId={}", videoId, userId);
            throw e;
        } catch (Exception e) {
            // Only log unexpected exceptions as errors
            logger.error("Unexpected error generating download URL for videoId: {}, userId: {}", videoId, userId, e);
            throw e;
        }
    }
    
    /**
     * Update video processing status (called by vclipping service)
     */
    @PutMapping("/{videoId}/status")
    public ResponseEntity<VideoStatusUpdateResponse> updateVideoStatus(
            @PathVariable String videoId,
            @RequestHeader("X-User-Id") @NotBlank(message = "User ID cannot be empty") String userId,
            @Valid @RequestBody VideoStatusUpdateRequest request) {
        logger.info("Updating video status: videoId={}, userId={}, newStatus={}", videoId, userId, request.status().value());
        
        try {
            // Validate request
            if (!request.isValid()) {
                logger.warn("Invalid status update request for videoId: {}", videoId);
                return ResponseEntity.badRequest()
                    .body(VideoStatusUpdateResponse.failure(videoId, "unknown", null, 
                        "Invalid request: " + getValidationMessage(request)));
            }
            
            // Execute use case
            UpdateVideoStatusUseCase.VideoStatusUpdateResponse response = updateVideoStatusUseCase.execute(
                videoId,
                request.status(),
                request.processedFileS3Key(),
                request.errorMessage()
            );
            
            logger.info("Successfully updated video status: videoId={}, previousStatus={}, newStatus={}", 
                videoId, response.previousStatus().value(), response.newStatus().value());
            
            return ResponseEntity.ok(VideoStatusUpdateResponse.success(
                response.videoId(),
                response.userId(),
                response.previousStatus(),
                response.newStatus(),
                response.processedFileS3Key(),
                response.updatedAt()
            ));
            
        } catch (VideoNotFoundException e) {
            // Let domain exceptions bubble up to GlobalExceptionHandler
            logger.debug("VideoNotFoundException during status update for videoId: {} - re-throwing to GlobalExceptionHandler", videoId);
            throw e;
        } catch (IllegalStateException e) {
            // Invalid status transition - business rule violation
            logger.warn("Invalid status transition for videoId: {} - {}", videoId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(VideoStatusUpdateResponse.failure(videoId, "unknown", null, e.getMessage()));
        } catch (IllegalArgumentException e) {
            // Invalid arguments - validation error
            logger.warn("Invalid arguments for status update videoId: {} - {}", videoId, e.getMessage());
            return ResponseEntity.badRequest()
                .body(VideoStatusUpdateResponse.failure(videoId, "unknown", null, e.getMessage()));
        } catch (Exception e) {
            // Only catch unexpected system errors
            logger.error("Unexpected system error during status update for videoId: {} - {}", videoId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VideoStatusUpdateResponse.failure(videoId, "unknown", null, "Internal server error"));
        }
    }
    
    /**
     * Helper method to generate validation error messages
     */
    private String getValidationMessage(VideoStatusUpdateRequest request) {
        if (request.status() == null) {
            return "Status is required";
        }
        if (request.status().equals(com.vclipper.processing.domain.entity.ProcessingStatus.COMPLETED) 
            && (request.processedFileS3Key() == null || request.processedFileS3Key().trim().isEmpty())) {
            return "Processed file S3 key is required for COMPLETED status";
        }
        if (request.status().equals(com.vclipper.processing.domain.entity.ProcessingStatus.FAILED) 
            && (request.errorMessage() == null || request.errorMessage().trim().isEmpty())) {
            return "Error message is required for FAILED status";
        }
        return "Invalid request";
    }
}
