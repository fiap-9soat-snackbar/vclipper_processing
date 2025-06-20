package com.vclipper.processing.domain.event;

/**
 * Domain event fired when video processing completes successfully
 */
public class ProcessingCompletedEvent extends DomainEvent {
    private final String userId;
    private final String filename;
    private final String processedFileReference;
    
    public ProcessingCompletedEvent(String videoId, String userId, String filename, String processedFileReference) {
        super(videoId);
        this.userId = userId;
        this.filename = filename;
        this.processedFileReference = processedFileReference;
    }
    
    @Override
    public String getEventType() {
        return "PROCESSING_COMPLETED";
    }
    
    public String getUserId() { return userId; }
    public String getFilename() { return filename; }
    public String getProcessedFileReference() { return processedFileReference; }
}
