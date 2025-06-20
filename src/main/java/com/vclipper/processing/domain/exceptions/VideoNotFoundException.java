package com.vclipper.processing.domain.exceptions;

/**
 * Exception thrown when requested video is not found
 */
public class VideoNotFoundException extends VideoProcessingException {
    
    public VideoNotFoundException(String videoId) {
        super("Video not found with ID: " + videoId);
    }
    
    public VideoNotFoundException(String videoId, Throwable cause) {
        super("Video not found with ID: " + videoId, cause);
    }
}
