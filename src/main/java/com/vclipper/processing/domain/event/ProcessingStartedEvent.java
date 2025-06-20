package com.vclipper.processing.domain.event;

/**
 * Domain event fired when video processing starts
 */
public class ProcessingStartedEvent extends DomainEvent {
    private final String userId;
    private final String filename;
    
    public ProcessingStartedEvent(String videoId, String userId, String filename) {
        super(videoId);
        this.userId = userId;
        this.filename = filename;
    }
    
    @Override
    public String getEventType() {
        return "PROCESSING_STARTED";
    }
    
    public String getUserId() { return userId; }
    public String getFilename() { return filename; }
}
