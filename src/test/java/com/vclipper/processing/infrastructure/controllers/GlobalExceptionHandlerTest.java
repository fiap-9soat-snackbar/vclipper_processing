package com.vclipper.processing.infrastructure.controllers;

import com.vclipper.processing.domain.exceptions.VideoProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }



    @Test
    void handleVideoProcessingException() {
        // Arrange
        String errorMessage = "Error during video processing";
        VideoProcessingException exception = new VideoProcessingException(errorMessage);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            exceptionHandler.handleVideoProcessingException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("VIDEO_PROCESSING_ERROR", errorResponse.errorCode());
        assertEquals(errorMessage, errorResponse.message());
        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void handleMaxUploadSizeExceededException() {
        // Arrange
        MaxUploadSizeExceededException exception = new MaxUploadSizeExceededException(10 * 1024 * 1024);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            exceptionHandler.handleMaxUploadSizeExceededException(exception);

        // Assert
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("FILE_TOO_LARGE", errorResponse.errorCode());
        assertEquals("Uploaded file exceeds maximum allowed size", errorResponse.message());
        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void handleMissingServletRequestParameterException() {
        // Arrange
        String paramName = "videoId";
        String paramType = "String";
        MissingServletRequestParameterException exception =
            new MissingServletRequestParameterException(paramName, paramType);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            exceptionHandler.handleMissingServletRequestParameterException(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("MISSING_PARAMETER", errorResponse.errorCode());
        assertEquals("Missing required parameter 'videoId' of type 'String'", errorResponse.message());
        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void handleMissingRequestHeaderException() {
        // Arrange
        String headerName = "Authorization";
        MissingRequestHeaderException exception = mock(MissingRequestHeaderException.class);
        when(exception.getHeaderName()).thenReturn(headerName);

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            exceptionHandler.handleMissingRequestHeaderException(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("MISSING_HEADER", errorResponse.errorCode());
        assertEquals("Missing required header 'Authorization'", errorResponse.message());
        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void handleValidationExceptions() {
        // Arrange
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("videoRequest", "title", "Title is required");
        FieldError fieldError2 = new FieldError("videoRequest", "description", "Description too long");

        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrorCount()).thenReturn(2);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        // Act
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
            exceptionHandler.handleValidationExceptions(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GlobalExceptionHandler.ValidationErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("VALIDATION_ERROR", errorResponse.errorCode());
        assertEquals("Request validation failed", errorResponse.message());

        Map<String, String> fieldErrors = errorResponse.fieldErrors();
        assertEquals(2, fieldErrors.size());
        assertEquals("Title is required", fieldErrors.get("title"));
        assertEquals("Description too long", fieldErrors.get("description"));

        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void handleConstraintViolationException() {
        // Arrange
        Set<ConstraintViolation<?>> violations = new HashSet<>();
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);

        when(violation1.getPropertyPath()).thenReturn(new TestPropertyPath("startTime"));
        when(violation1.getMessage()).thenReturn("Start time must be greater than 0");

        when(violation2.getPropertyPath()).thenReturn(new TestPropertyPath("endTime"));
        when(violation2.getMessage()).thenReturn("End time must be greater than start time");

        violations.add(violation1);
        violations.add(violation2);

        ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);

        // Act
        ResponseEntity<GlobalExceptionHandler.ValidationErrorResponse> response =
            exceptionHandler.handleConstraintViolationException(exception);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        GlobalExceptionHandler.ValidationErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("VALIDATION_ERROR", errorResponse.errorCode());
        assertEquals("Request validation failed", errorResponse.message());

        Map<String, String> fieldErrors = errorResponse.fieldErrors();
        assertEquals(2, fieldErrors.size());
        assertEquals("Start time must be greater than 0", fieldErrors.get("startTime"));
        assertEquals("End time must be greater than start time", fieldErrors.get("endTime"));

        assertNotNull(errorResponse.timestamp());
    }

    @Test
    void handleGenericException() {
        // Arrange
        Exception exception = new RuntimeException("Unexpected error occurred");

        // Act
        ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
            exceptionHandler.handleGenericException(exception);

        // Assert
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        GlobalExceptionHandler.ErrorResponse errorResponse = response.getBody();
        assertNotNull(errorResponse);
        assertEquals("INTERNAL_SERVER_ERROR", errorResponse.errorCode());
        assertEquals("An unexpected error occurred", errorResponse.message());
        assertNotNull(errorResponse.timestamp());
    }

    // Helper class to mock PropertyPath interface
    private static class TestPropertyPath implements jakarta.validation.Path {
        private final String propertyName;

        public TestPropertyPath(String propertyName) {
            this.propertyName = propertyName;
        }

        @Override
        public String toString() {
            return propertyName;
        }

        @Override
        public Iterator<Node> iterator() {
            throw new UnsupportedOperationException("Not implemented for test");
        }
    }
}
