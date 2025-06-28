package com.vclipper.processing.domain.exceptions;

import com.vclipper.processing.domain.entity.ProcessingStatus;

/**
 * Exception thrown when attempting to perform an operation on a video
 * that is not in the required state (e.g., downloading a video that's still processing)
 * 
 * This represents a business rule violation, not a system error.
 */
public class VideoNotReadyException extends VideoProcessingException {
    
    private final String videoId;
    private final ProcessingStatus currentStatus;
    private final String operation;
    
    public VideoNotReadyException(String videoId, ProcessingStatus currentStatus, String operation) {
        super(String.format("Video %s is not ready for %s. Current status: %s", 
              videoId, operation, currentStatus.value()));
        this.videoId = videoId;
        this.currentStatus = currentStatus;
        this.operation = operation;
    }
    
    public String getVideoId() {
        return videoId;
    }
    
    public ProcessingStatus getCurrentStatus() {
        return currentStatus;
    }
    
    public String getOperation() {
        return operation;
    }
}
