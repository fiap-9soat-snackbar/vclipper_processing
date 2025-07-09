package com.vclipper.processing.application.common;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the ProcessingError record class to verify all factory methods
 * and the record's behavior.
 */
public class ProcessingErrorTest {

    @Test
    public void testBasicRecordFunctionality() {
        // Arrange
        String errorCode = "TEST_ERROR";
        String message = "Test message";
        Exception cause = new RuntimeException("Test cause");
        
        // Act
        ProcessingError error = new ProcessingError(errorCode, message, cause);
        
        // Assert
        assertEquals(errorCode, error.errorCode());
        assertEquals(message, error.message());
        assertSame(cause, error.cause());
    }
    
    @Test
    public void testInvalidRequest() {
        // Arrange
        String message = "Invalid request message";
        
        // Act
        ProcessingError error = ProcessingError.invalidRequest(message);
        
        // Assert
        assertEquals("INVALID_REQUEST", error.errorCode());
        assertEquals(message, error.message());
        assertNull(error.cause());
    }
    
    @Test
    public void testInvalidFormat() {
        // Arrange
        String message = "Invalid format message";
        
        // Act
        ProcessingError error = ProcessingError.invalidFormat(message);
        
        // Assert
        assertEquals("INVALID_FORMAT", error.errorCode());
        assertEquals(message, error.message());
        assertNull(error.cause());
    }
    
    @Test
    public void testProcessingFailed() {
        // Arrange
        String message = "Processing failed message";
        Exception cause = new RuntimeException("Test cause");
        
        // Act
        ProcessingError error1 = ProcessingError.processingFailed(message);
        ProcessingError error2 = ProcessingError.processingFailed(message, cause);
        
        // Assert
        assertEquals("PROCESSING_FAILED", error1.errorCode());
        assertEquals(message, error1.message());
        assertNull(error1.cause());
        
        assertEquals("PROCESSING_FAILED", error2.errorCode());
        assertEquals(message, error2.message());
        assertSame(cause, error2.cause());
    }
    
    @Test
    public void testDatabaseError() {
        // Arrange
        String message = "Database error message";
        Exception cause = new RuntimeException("Test cause");
        
        // Act
        ProcessingError error1 = ProcessingError.databaseError(message);
        ProcessingError error2 = ProcessingError.databaseError(message, cause);
        
        // Assert
        assertEquals("DATABASE_ERROR", error1.errorCode());
        assertEquals(message, error1.message());
        assertNull(error1.cause());
        
        assertEquals("DATABASE_ERROR", error2.errorCode());
        assertEquals(message, error2.message());
        assertSame(cause, error2.cause());
    }
    
    @Test
    public void testNotFound() {
        // Arrange
        String message = "Not found message";
        
        // Act
        ProcessingError error = ProcessingError.notFound(message);
        
        // Assert
        assertEquals("NOT_FOUND", error.errorCode());
        assertEquals(message, error.message());
        assertNull(error.cause());
    }
    
    @Test
    public void testResultProcessingFailed() {
        // Arrange
        String message = "Result processing failed message";
        Exception cause = new RuntimeException("Test cause");
        
        // Act
        ProcessingError error1 = ProcessingError.resultProcessingFailed(message);
        ProcessingError error2 = ProcessingError.resultProcessingFailed(message, cause);
        
        // Assert
        assertEquals("RESULT_PROCESSING_FAILED", error1.errorCode());
        assertEquals(message, error1.message());
        assertNull(error1.cause());
        
        assertEquals("RESULT_PROCESSING_FAILED", error2.errorCode());
        assertEquals(message, error2.message());
        assertSame(cause, error2.cause());
    }
    
    @Test
    public void testStatusUpdateFailed() {
        // Arrange
        String message = "Status update failed message";
        Exception cause = new RuntimeException("Test cause");
        
        // Act
        ProcessingError error1 = ProcessingError.statusUpdateFailed(message);
        ProcessingError error2 = ProcessingError.statusUpdateFailed(message, cause);
        
        // Assert
        assertEquals("STATUS_UPDATE_FAILED", error1.errorCode());
        assertEquals(message, error1.message());
        assertNull(error1.cause());
        
        assertEquals("STATUS_UPDATE_FAILED", error2.errorCode());
        assertEquals(message, error2.message());
        assertSame(cause, error2.cause());
    }
    
    @Test
    public void testFileCopyFailed() {
        // Arrange
        String message = "File copy failed message";
        Exception cause = new RuntimeException("Test cause");
        
        // Act
        ProcessingError error1 = ProcessingError.fileCopyFailed(message);
        ProcessingError error2 = ProcessingError.fileCopyFailed(message, cause);
        
        // Assert
        assertEquals("FILE_COPY_FAILED", error1.errorCode());
        assertEquals(message, error1.message());
        assertNull(error1.cause());
        
        assertEquals("FILE_COPY_FAILED", error2.errorCode());
        assertEquals(message, error2.message());
        assertSame(cause, error2.cause());
    }
    
    @Test
    public void testEquals() {
        // Arrange
        ProcessingError error1 = new ProcessingError("CODE", "message", null);
        ProcessingError error2 = new ProcessingError("CODE", "message", null);
        ProcessingError error3 = new ProcessingError("DIFFERENT", "message", null);
        
        // Assert
        assertEquals(error1, error2);
        assertNotEquals(error1, error3);
    }
    
    @Test
    public void testHashCode() {
        // Arrange
        ProcessingError error1 = new ProcessingError("CODE", "message", null);
        ProcessingError error2 = new ProcessingError("CODE", "message", null);
        
        // Assert
        assertEquals(error1.hashCode(), error2.hashCode());
    }
}
