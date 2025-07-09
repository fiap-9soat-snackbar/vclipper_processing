package com.vclipper.processing.domain.entity;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class NotificationTypeTest {

    @Test
    @DisplayName("Should create predefined notification types with correct values")
    void shouldCreatePredefinedNotificationTypes() {
        // UPLOAD_CONFIRMED
        assertEquals("UPLOAD_CONFIRMED", NotificationType.UPLOAD_CONFIRMED.type());
        assertEquals("Your video '{videoName}' has been uploaded and queued for processing", 
                NotificationType.UPLOAD_CONFIRMED.messageTemplate());
        assertEquals("Video Upload Confirmed", NotificationType.UPLOAD_CONFIRMED.subject());
        
        // PROCESSING_STARTED
        assertEquals("PROCESSING_STARTED", NotificationType.PROCESSING_STARTED.type());
        assertEquals("Your video '{videoName}' is now being processed. You'll be notified when it's ready", 
                NotificationType.PROCESSING_STARTED.messageTemplate());
        assertEquals("Video Processing Started", NotificationType.PROCESSING_STARTED.subject());
        
        // PROCESSING_COMPLETED
        assertEquals("PROCESSING_COMPLETED", NotificationType.PROCESSING_COMPLETED.type());
        assertEquals("Your video '{videoName}' has been processed successfully. Download is ready: {downloadUrl}", 
                NotificationType.PROCESSING_COMPLETED.messageTemplate());
        assertEquals("Video Processing Completed", NotificationType.PROCESSING_COMPLETED.subject());
        
        // PROCESSING_FAILED
        assertEquals("PROCESSING_FAILED", NotificationType.PROCESSING_FAILED.type());
        assertEquals("Your video '{videoName}' processing failed: {errorMessage}. Please try uploading again", 
                NotificationType.PROCESSING_FAILED.messageTemplate());
        assertEquals("Video Processing Failed", NotificationType.PROCESSING_FAILED.subject());
    }
    
    @ParameterizedTest
    @DisplayName("Should retrieve notification type from string value")
    @ValueSource(strings = {"UPLOAD_CONFIRMED", "PROCESSING_STARTED", "PROCESSING_COMPLETED", "PROCESSING_FAILED"})
    void shouldRetrieveNotificationTypeFromString(String typeString) {
        NotificationType type = NotificationType.fromType(typeString);
        assertEquals(typeString, type.type());
    }
    
    @ParameterizedTest
    @DisplayName("Should retrieve notification type from case-insensitive string")
    @ValueSource(strings = {"upload_confirmed", "Processing_Started", "processing_completed", "PROCESSING_failed"})
    void shouldRetrieveNotificationTypeFromCaseInsensitiveString(String typeString) {
        NotificationType type = NotificationType.fromType(typeString);
        assertEquals(typeString.toUpperCase(), type.type());
    }
    
    @Test
    @DisplayName("Should throw exception for invalid notification type")
    void shouldThrowExceptionForInvalidNotificationType() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> NotificationType.fromType("INVALID_TYPE")
        );
        
        assertEquals("Invalid notification type: INVALID_TYPE", exception.getMessage());
    }
    
    @ParameterizedTest
    @DisplayName("Should format message with provided parameters")
    @MethodSource("messageFormatTestCases")
    void shouldFormatMessageWithProvidedParameters(
            NotificationType type, 
            String videoName, 
            String downloadUrl, 
            String errorMessage, 
            String expectedResult) {
        
        String formattedMessage = type.formatMessage(videoName, downloadUrl, errorMessage);
        assertEquals(expectedResult, formattedMessage);
    }
    
    static Stream<Arguments> messageFormatTestCases() {
        return Stream.of(
            // UPLOAD_CONFIRMED with all parameters
            Arguments.of(
                NotificationType.UPLOAD_CONFIRMED,
                "test_video.mp4",
                "http://example.com/download",
                "Error message",
                "Your video 'test_video.mp4' has been uploaded and queued for processing"
            ),
            
            // PROCESSING_STARTED with null videoName (should use default)
            Arguments.of(
                NotificationType.PROCESSING_STARTED,
                null,
                "http://example.com/download",
                "Error message",
                "Your video 'your video' is now being processed. You'll be notified when it's ready"
            ),
            
            // PROCESSING_COMPLETED with all parameters
            Arguments.of(
                NotificationType.PROCESSING_COMPLETED,
                "vacation.mp4",
                "http://example.com/download/123",
                "Error message",
                "Your video 'vacation.mp4' has been processed successfully. Download is ready: http://example.com/download/123"
            ),
            
            // PROCESSING_COMPLETED with null downloadUrl (should use empty string)
            Arguments.of(
                NotificationType.PROCESSING_COMPLETED,
                "vacation.mp4",
                null,
                "Error message",
                "Your video 'vacation.mp4' has been processed successfully. Download is ready: "
            ),
            
            // PROCESSING_FAILED with all parameters
            Arguments.of(
                NotificationType.PROCESSING_FAILED,
                "corrupted.mp4",
                "http://example.com/download",
                "File is corrupted",
                "Your video 'corrupted.mp4' processing failed: File is corrupted. Please try uploading again"
            ),
            
            // PROCESSING_FAILED with null errorMessage (should use default)
            Arguments.of(
                NotificationType.PROCESSING_FAILED,
                "corrupted.mp4",
                "http://example.com/download",
                null,
                "Your video 'corrupted.mp4' processing failed: Unknown error. Please try uploading again"
            )
        );
    }
}
