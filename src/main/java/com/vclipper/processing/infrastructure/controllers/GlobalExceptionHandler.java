package com.vclipper.processing.infrastructure.controllers;

import com.vclipper.processing.domain.exceptions.InvalidVideoFormatException;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import com.vclipper.processing.domain.exceptions.VideoProcessingException;
import com.vclipper.processing.domain.exceptions.VideoUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers
 * Provides consistent error responses across the application
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    /**
     * Handle video not found exceptions
     */
    @ExceptionHandler(VideoNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleVideoNotFoundException(VideoNotFoundException e) {
        logger.warn("Video not found: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("VIDEO_NOT_FOUND", e.getMessage(), LocalDateTime.now()));
    }
    
    /**
     * Handle invalid video format exceptions
     */
    @ExceptionHandler(InvalidVideoFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidVideoFormatException(InvalidVideoFormatException e) {
        logger.warn("Invalid video format: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("INVALID_VIDEO_FORMAT", e.getMessage(), LocalDateTime.now()));
    }
    
    /**
     * Handle video upload exceptions
     */
    @ExceptionHandler(VideoUploadException.class)
    public ResponseEntity<ErrorResponse> handleVideoUploadException(VideoUploadException e) {
        logger.warn("Video upload error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("VIDEO_UPLOAD_ERROR", e.getMessage(), LocalDateTime.now()));
    }
    
    /**
     * Handle general video processing exceptions
     */
    @ExceptionHandler(VideoProcessingException.class)
    public ResponseEntity<ErrorResponse> handleVideoProcessingException(VideoProcessingException e) {
        logger.error("Video processing error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("VIDEO_PROCESSING_ERROR", e.getMessage(), LocalDateTime.now()));
    }
    
    /**
     * Handle file size exceeded exceptions
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        logger.warn("File size exceeded: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ErrorResponse("FILE_TOO_LARGE", "Uploaded file exceeds maximum allowed size", LocalDateTime.now()));
    }
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        logger.warn("Validation error: {}", e.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ValidationErrorResponse("VALIDATION_ERROR", "Request validation failed", errors, LocalDateTime.now()));
    }
    
    /**
     * Handle unexpected exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception e) {
        logger.error("Unexpected error: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("INTERNAL_SERVER_ERROR", "An unexpected error occurred", LocalDateTime.now()));
    }
    
    /**
     * Standard error response
     */
    public record ErrorResponse(
        String errorCode,
        String message,
        LocalDateTime timestamp
    ) {}
    
    /**
     * Validation error response with field details
     */
    public record ValidationErrorResponse(
        String errorCode,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
    ) {}
}
