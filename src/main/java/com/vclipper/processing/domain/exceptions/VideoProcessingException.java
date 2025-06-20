package com.vclipper.processing.domain.exceptions;

/**
 * Base exception for video processing domain errors
 */
public class VideoProcessingException extends RuntimeException {
    
    public VideoProcessingException(String message) {
        super(message);
    }
    
    public VideoProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
