package com.vclipper.processing.domain.event;

import com.vclipper.processing.domain.entity.VideoMetadata;

/**
 * Domain event fired when a video is successfully uploaded
 */
public class VideoUploadedEvent extends DomainEvent {
    private final String userId;
    private final VideoMetadata metadata;
    
    public VideoUploadedEvent(String videoId, String userId, VideoMetadata metadata) {
        super(videoId);
        this.userId = userId;
        this.metadata = metadata;
    }
    
    @Override
    public String getEventType() {
        return "VIDEO_UPLOADED";
    }
    
    public String getUserId() { return userId; }
    public VideoMetadata getMetadata() { return metadata; }
}
