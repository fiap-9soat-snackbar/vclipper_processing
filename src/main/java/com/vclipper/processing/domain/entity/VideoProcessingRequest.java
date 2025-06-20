package com.vclipper.processing.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Aggregate root representing a video processing request
 * Contains all business logic for video processing workflow
 */
public class VideoProcessingRequest {
    private final String videoId;
    private final String userId;
    private final VideoMetadata metadata;
    private ProcessingStatus status;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String errorMessage;
    private String processedFileReference;
    
    // Constructor for new requests
    public VideoProcessingRequest(String userId, VideoMetadata metadata) {
        this.videoId = UUID.randomUUID().toString();
        this.userId = userId;
        this.metadata = metadata;
        this.status = ProcessingStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
    
    // Constructor for existing requests (from persistence)
    public VideoProcessingRequest(String videoId, String userId, VideoMetadata metadata, 
                                ProcessingStatus status, LocalDateTime createdAt, 
                                LocalDateTime updatedAt, String errorMessage, 
                                String processedFileReference) {
        this.videoId = videoId;
        this.userId = userId;
        this.metadata = metadata;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.errorMessage = errorMessage;
        this.processedFileReference = processedFileReference;
    }
    
    /**
     * Business method: Update processing status with validation
     */
    public void updateStatus(ProcessingStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                String.format("Cannot transition from %s to %s", 
                    this.status.value(), newStatus.value())
            );
        }
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business method: Mark processing as failed with error message
     */
    public void markAsFailed(String errorMessage) {
        this.status = ProcessingStatus.FAILED;
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business method: Mark processing as completed with processed file reference
     */
    public void markAsCompleted(String processedFileReference) {
        this.status = ProcessingStatus.COMPLETED;
        this.processedFileReference = processedFileReference;
        this.updatedAt = LocalDateTime.now();
    }
    
    /**
     * Business method: Start processing
     */
    public void startProcessing() {
        updateStatus(ProcessingStatus.PROCESSING);
    }
    
    /**
     * Business rule: Check if request can be retried
     */
    public boolean canRetry() {
        return status.canRetry();
    }
    
    /**
     * Business rule: Check if processing is finished (success or failure)
     */
    public boolean isFinished() {
        return status.isFinished();
    }
    
    /**
     * Business rule: Check if download is available
     */
    public boolean isDownloadReady() {
        return status.equals(ProcessingStatus.COMPLETED) && processedFileReference != null;
    }
    
    // Getters
    public String getVideoId() { return videoId; }
    public String getUserId() { return userId; }
    public VideoMetadata getMetadata() { return metadata; }
    public ProcessingStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getErrorMessage() { return errorMessage; }
    public String getProcessedFileReference() { return processedFileReference; }
}
