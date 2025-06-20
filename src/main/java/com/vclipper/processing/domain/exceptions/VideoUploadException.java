package com.vclipper.processing.domain.exceptions;

/**
 * Exception thrown when video upload fails
 */
public class VideoUploadException extends VideoProcessingException {
    
    public VideoUploadException(String message) {
        super("Video upload failed: " + message);
    }
    
    public VideoUploadException(String message, Throwable cause) {
        super("Video upload failed: " + message, cause);
    }
}
