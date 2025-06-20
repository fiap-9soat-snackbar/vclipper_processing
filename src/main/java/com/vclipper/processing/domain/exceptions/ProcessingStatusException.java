package com.vclipper.processing.domain.exceptions;

/**
 * Exception thrown when invalid status transitions are attempted
 */
public class ProcessingStatusException extends VideoProcessingException {
    
    public ProcessingStatusException(String currentStatus, String newStatus) {
        super(String.format("Invalid status transition from %s to %s", currentStatus, newStatus));
    }
    
    public ProcessingStatusException(String message) {
        super("Processing status error: " + message);
    }
    
    public ProcessingStatusException(String message, Throwable cause) {
        super("Processing status error: " + message, cause);
    }
}
