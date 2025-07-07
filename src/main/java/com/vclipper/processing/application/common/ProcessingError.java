package com.vclipper.processing.application.common;

/**
 * Application layer error representation for processing operations.
 * 
 * Follows the same pattern as vclipping's ProcessingError for consistency.
 * Used in Result pattern for type-safe error handling.
 * 
 * @param errorCode Standardized error code for categorization
 * @param message Human-readable error message
 * @param cause Optional underlying cause (for debugging)
 */
public record ProcessingError(
    String errorCode,
    String message,
    Throwable cause
) {
    
    // Factory methods for common error types
    
    public static ProcessingError invalidRequest(String message) {
        return new ProcessingError("INVALID_REQUEST", message, null);
    }
    
    public static ProcessingError invalidFormat(String message) {
        return new ProcessingError("INVALID_FORMAT", message, null);
    }
    
    public static ProcessingError processingFailed(String message) {
        return new ProcessingError("PROCESSING_FAILED", message, null);
    }
    
    public static ProcessingError processingFailed(String message, Throwable cause) {
        return new ProcessingError("PROCESSING_FAILED", message, cause);
    }
    
    public static ProcessingError databaseError(String message) {
        return new ProcessingError("DATABASE_ERROR", message, null);
    }
    
    public static ProcessingError databaseError(String message, Throwable cause) {
        return new ProcessingError("DATABASE_ERROR", message, cause);
    }
    
    public static ProcessingError notFound(String message) {
        return new ProcessingError("NOT_FOUND", message, null);
    }
    
    public static ProcessingError resultProcessingFailed(String message) {
        return new ProcessingError("RESULT_PROCESSING_FAILED", message, null);
    }
    
    public static ProcessingError resultProcessingFailed(String message, Throwable cause) {
        return new ProcessingError("RESULT_PROCESSING_FAILED", message, cause);
    }
    
    public static ProcessingError statusUpdateFailed(String message) {
        return new ProcessingError("STATUS_UPDATE_FAILED", message, null);
    }
    
    public static ProcessingError statusUpdateFailed(String message, Throwable cause) {
        return new ProcessingError("STATUS_UPDATE_FAILED", message, cause);
    }
    
    public static ProcessingError fileCopyFailed(String message) {
        return new ProcessingError("FILE_COPY_FAILED", message, null);
    }
    
    public static ProcessingError fileCopyFailed(String message, Throwable cause) {
        return new ProcessingError("FILE_COPY_FAILED", message, cause);
    }
}
