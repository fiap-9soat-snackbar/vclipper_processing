package com.vclipper.processing.infrastructure.controllers;

import com.vclipper.processing.application.usecases.GetProcessingStatusUseCase;
import com.vclipper.processing.application.usecases.ListUserVideosUseCase;
import com.vclipper.processing.application.usecases.SubmitVideoProcessingUseCase;
import com.vclipper.processing.infrastructure.controllers.dto.ProcessingStatusResponse;
import com.vclipper.processing.infrastructure.controllers.dto.VideoListResponse;
import com.vclipper.processing.infrastructure.controllers.dto.VideoUploadRequest;
import com.vclipper.processing.infrastructure.controllers.dto.VideoUploadResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * REST controller for video processing operations
 * Handles video upload, status tracking, and listing
 */
@RestController
@RequestMapping("/api/videos")
public class VideoProcessingController {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoProcessingController.class);
    
    private final SubmitVideoProcessingUseCase submitVideoProcessingUseCase;
    private final GetProcessingStatusUseCase getProcessingStatusUseCase;
    private final ListUserVideosUseCase listUserVideosUseCase;
    
    public VideoProcessingController(SubmitVideoProcessingUseCase submitVideoProcessingUseCase,
                                   GetProcessingStatusUseCase getProcessingStatusUseCase,
                                   ListUserVideosUseCase listUserVideosUseCase) {
        this.submitVideoProcessingUseCase = submitVideoProcessingUseCase;
        this.getProcessingStatusUseCase = getProcessingStatusUseCase;
        this.listUserVideosUseCase = listUserVideosUseCase;
    }
    
    /**
     * Upload video for processing
     */
    @PostMapping("/upload")
    public ResponseEntity<VideoUploadResponse> uploadVideo(@Valid @ModelAttribute VideoUploadRequest request) {
        logger.info("Received video upload request for user: {}, filename: {}", 
            request.userId(), request.getOriginalFilename());
        
        try {
            // Validate file
            if (!request.isFileValid()) {
                return ResponseEntity.badRequest()
                    .body(VideoUploadResponse.failure(request.userId(), request.getOriginalFilename(), 
                        "Invalid file: file is empty or has no name"));
            }
            
            // Create use case submission
            SubmitVideoProcessingUseCase.VideoProcessingSubmission submission = 
                new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                    request.userId(),
                    request.file().getInputStream(),
                    request.getOriginalFilename(),
                    request.getContentType(),
                    request.getFileSizeBytes()
                );
            
            // Execute use case
            SubmitVideoProcessingUseCase.VideoProcessingResponse response = 
                submitVideoProcessingUseCase.execute(submission);
            
            if (response.success()) {
                logger.info("Video upload successful for user: {}, videoId: {}", 
                    request.userId(), response.videoId());
                
                return ResponseEntity.status(HttpStatus.CREATED)
                    .body(VideoUploadResponse.success(
                        response.videoId(),
                        request.userId(),
                        request.getOriginalFilename(),
                        response.status(),
                        response.message(),
                        java.time.LocalDateTime.now()
                    ));
            } else {
                logger.warn("Video upload failed for user: {}, error: {}", 
                    request.userId(), response.message());
                
                return ResponseEntity.badRequest()
                    .body(VideoUploadResponse.failure(request.userId(), request.getOriginalFilename(), 
                        response.message()));
            }
            
        } catch (IOException e) {
            logger.error("Error reading uploaded file for user: {}", request.userId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VideoUploadResponse.failure(request.userId(), request.getOriginalFilename(), 
                    "Error processing uploaded file"));
        } catch (Exception e) {
            logger.error("Unexpected error during video upload for user: {}", request.userId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(VideoUploadResponse.failure(request.userId(), request.getOriginalFilename(), 
                    "Internal server error"));
        }
    }
    
    /**
     * Get processing status for a video
     */
    @GetMapping("/{videoId}/status")
    public ResponseEntity<ProcessingStatusResponse> getProcessingStatus(
            @PathVariable String videoId,
            @RequestParam String userId) {
        
        logger.info("Getting processing status for videoId: {}, userId: {}", videoId, userId);
        
        try {
            GetProcessingStatusUseCase.ProcessingStatusResponse response = 
                getProcessingStatusUseCase.execute(videoId, userId);
            
            return ResponseEntity.ok(ProcessingStatusResponse.from(response));
            
        } catch (Exception e) {
            logger.error("Error getting processing status for videoId: {}, userId: {}", videoId, userId, e);
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * List all videos for a user
     */
    @GetMapping
    public ResponseEntity<VideoListResponse> listUserVideos(@RequestParam String userId) {
        logger.info("Listing videos for userId: {}", userId);
        
        try {
            var videoSummaries = listUserVideosUseCase.execute(userId);
            
            return ResponseEntity.ok(VideoListResponse.from(userId, videoSummaries));
            
        } catch (Exception e) {
            logger.error("Error listing videos for userId: {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
