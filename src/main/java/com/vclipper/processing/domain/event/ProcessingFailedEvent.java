package com.vclipper.processing.domain.event;

/**
 * Domain event fired when video processing fails
 */
public class ProcessingFailedEvent extends DomainEvent {
    private final String userId;
    private final String filename;
    private final String errorMessage;
    
    public ProcessingFailedEvent(String videoId, String userId, String filename, String errorMessage) {
        super(videoId);
        this.userId = userId;
        this.filename = filename;
        this.errorMessage = errorMessage;
    }
    
    @Override
    public String getEventType() {
        return "PROCESSING_FAILED";
    }
    
    public String getUserId() { return userId; }
    public String getFilename() { return filename; }
    public String getErrorMessage() { return errorMessage; }
}
