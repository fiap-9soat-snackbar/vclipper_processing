package com.vclipper.processing.infrastructure.controllers;

import com.vclipper.processing.domain.exceptions.InvalidVideoFormatException;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import com.vclipper.processing.domain.exceptions.VideoNotReadyException;
import com.vclipper.processing.domain.exceptions.VideoProcessingException;
import com.vclipper.processing.domain.exceptions.VideoUploadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
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
        // Enhanced logging with context - WARN level since it's expected user behavior
        String videoId = extractVideoIdFromMessage(e.getMessage());
        logger.warn("Video not found - videoId: {} - {}", videoId, e.getMessage());
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
     * Handle video not ready exceptions (business rule violations)
     * These are expected conditions, not system errors
     */
    @ExceptionHandler(VideoNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleVideoNotReadyException(VideoNotReadyException e) {
        // Log as info, not error - this is expected business behavior
        logger.info("Video not ready for operation: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ErrorResponse("VIDEO_NOT_READY", e.getMessage(), LocalDateTime.now()));
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
     * Handle missing request parameter exceptions
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        String parameterName = e.getParameterName();
        String parameterType = e.getParameterType();
        logger.warn("Missing required request parameter - name: '{}', type: '{}'", parameterName, parameterType);
        
        String message = String.format("Missing required parameter '%s' of type '%s'", parameterName, parameterType);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("MISSING_PARAMETER", message, LocalDateTime.now()));
    }
    
    /**
     * Handle missing request header exceptions
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        String headerName = e.getHeaderName();
        logger.warn("Missing required request header - name: '{}'", headerName);
        
        String message = String.format("Missing required header '%s'", headerName);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("MISSING_HEADER", message, LocalDateTime.now()));
    }
    
    /**
     * Handle validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidationExceptions(MethodArgumentNotValidException e) {
        // Changed from ERROR to WARN - validation errors are expected user behavior
        int fieldErrorCount = e.getBindingResult().getFieldErrorCount();
        logger.warn("Validation failed - {} field error(s)", fieldErrorCount);
        
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
            // Log each field error for better traceability (DEBUG level for details)
            logger.debug("Validation error - field: '{}', message: '{}'", fieldName, errorMessage);
        });
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ValidationErrorResponse("VALIDATION_ERROR", "Request validation failed", errors, LocalDateTime.now()));
    }
    
    /**
     * Handle constraint violation errors (e.g., @NotBlank on method parameters)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ValidationErrorResponse> handleConstraintViolationException(ConstraintViolationException e) {
        logger.warn("Constraint validation failed - {} violation(s)", e.getConstraintViolations().size());
        
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
            logger.debug("Constraint violation - property: '{}', message: '{}'", propertyPath, message);
        }
        
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
    
    /**
     * Extract video ID from exception message for better logging
     */
    private String extractVideoIdFromMessage(String message) {
        // Extract video ID from message like "Video not found: invalid-id"
        if (message != null && message.contains(": ")) {
            return message.substring(message.lastIndexOf(": ") + 2);
        }
        return "unknown";
    }
}
