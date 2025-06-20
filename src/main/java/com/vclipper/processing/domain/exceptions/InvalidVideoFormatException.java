package com.vclipper.processing.domain.exceptions;

/**
 * Exception thrown when video format is not supported
 */
public class InvalidVideoFormatException extends VideoProcessingException {
    
    public InvalidVideoFormatException(String format) {
        super("Invalid or unsupported video format: " + format);
    }
    
    public InvalidVideoFormatException(String format, Throwable cause) {
        super("Invalid or unsupported video format: " + format, cause);
    }
}
