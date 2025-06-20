package com.vclipper.processing.domain.entity;

/**
 * Value object representing video processing status with business rules
 */
public record ProcessingStatus(String value, String description, boolean isTerminal) {
    
    // Predefined status constants
    public static final ProcessingStatus PENDING = new ProcessingStatus(
        "PENDING", 
        "Video uploaded and queued for processing", 
        false
    );
    
    public static final ProcessingStatus PROCESSING = new ProcessingStatus(
        "PROCESSING", 
        "Video is currently being processed", 
        false
    );
    
    public static final ProcessingStatus COMPLETED = new ProcessingStatus(
        "COMPLETED", 
        "Video processing completed successfully", 
        true
    );
    
    public static final ProcessingStatus FAILED = new ProcessingStatus(
        "FAILED", 
        "Video processing failed", 
        true
    );
    
    /**
     * Business rule: validates if transition to new status is allowed
     */
    public boolean canTransitionTo(ProcessingStatus newStatus) {
        return switch (this.value) {
            case "PENDING" -> newStatus.equals(PROCESSING) || newStatus.equals(FAILED);
            case "PROCESSING" -> newStatus.equals(COMPLETED) || newStatus.equals(FAILED);
            case "COMPLETED", "FAILED" -> false; // Terminal states
            default -> false;
        };
    }
    
    /**
     * Factory method for creating status from string (useful for deserialization)
     */
    public static ProcessingStatus fromValue(String value) {
        return switch (value.toUpperCase()) {
            case "PENDING" -> PENDING;
            case "PROCESSING" -> PROCESSING;
            case "COMPLETED" -> COMPLETED;
            case "FAILED" -> FAILED;
            default -> throw new IllegalArgumentException("Invalid processing status: " + value);
        };
    }
    
    /**
     * Check if this status represents a completed workflow (success or failure)
     */
    public boolean isFinished() {
        return isTerminal;
    }
    
    /**
     * Check if processing can be retried from this status
     */
    public boolean canRetry() {
        return this.equals(FAILED);
    }
}
