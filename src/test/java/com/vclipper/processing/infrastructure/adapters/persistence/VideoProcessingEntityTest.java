package com.vclipper.processing.infrastructure.adapters.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class VideoProcessingEntityTest {

    private static final String VIDEO_ID = "video123";
    private static final String USER_ID = "user456";
    private static final String FILENAME = "test_video.mp4";
    private static final long FILE_SIZE = 1024L;
    private static final String FORMAT = "mp4";
    private static final String CONTENT_TYPE = "video/mp4";
    private static final String STORAGE_REF = "storage/video123.mp4";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PROCESSING = "PROCESSING";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_FAILED = "FAILED";
    private static final String STATUS_DESC = "Video processing initiated";

    @Test
    @DisplayName("Default constructor should create empty entity")
    void defaultConstructorShouldCreateEmptyEntity() {
        VideoProcessingEntity entity = new VideoProcessingEntity();
        
        assertNull(entity.getVideoId());
        assertNull(entity.getUserId());
        assertEquals(0, entity.getRetryCount());
    }
    
    @Test
    @DisplayName("Parameterized constructor should set all fields correctly")
    void parameterizedConstructorShouldSetAllFieldsCorrectly() {
        VideoProcessingEntity entity = createTestEntity();
        
        assertEquals(VIDEO_ID, entity.getVideoId());
        assertEquals(USER_ID, entity.getUserId());
        assertEquals(FILENAME, entity.getOriginalFilename());
        assertEquals(FILE_SIZE, entity.getFileSizeBytes());
        assertEquals(FORMAT, entity.getVideoFormat());
        assertEquals(CONTENT_TYPE, entity.getContentType());
        assertEquals(STORAGE_REF, entity.getStorageReference());
        assertEquals(STATUS_PENDING, entity.getStatusValue());
        assertEquals(STATUS_DESC, entity.getStatusDescription());
        assertFalse(entity.isStatusIsTerminal());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(0, entity.getRetryCount());
    }
    
    @Test
    @DisplayName("updateStatus should update all status fields and updatedAt timestamp")
    void updateStatusShouldUpdateAllStatusFieldsAndTimestamp() {
        VideoProcessingEntity entity = createTestEntity();
        LocalDateTime originalUpdatedAt = entity.getUpdatedAt();
        
        // Wait briefly to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        String errorMsg = null;
        String processedRef = "processed/video123.mp4";
        entity.updateStatus(STATUS_COMPLETED, "Processing completed", true, errorMsg, processedRef);
        
        assertEquals(STATUS_COMPLETED, entity.getStatusValue());
        assertEquals("Processing completed", entity.getStatusDescription());
        assertTrue(entity.isStatusIsTerminal());
        assertNull(entity.getErrorMessage());
        assertEquals(processedRef, entity.getProcessedFileReference());
        assertTrue(entity.getUpdatedAt().isAfter(originalUpdatedAt));
    }
    
    @Test
    @DisplayName("updateStatus should increment retry count when moving from FAILED to PENDING")
    void updateStatusShouldIncrementRetryCountWhenMovingFromFailedToPending() {
        VideoProcessingEntity entity = createTestEntity();
        
        // Set initial retry count > 0
        entity.setRetryCount(1);
        
        // Update status from failed to pending
        entity.updateStatus(STATUS_PENDING, "Retrying processing", false, null, null);
        
        assertEquals(2, entity.getRetryCount());
        assertEquals(STATUS_PENDING, entity.getStatusValue());
    }
    
    @Test
    @DisplayName("updateStatus should not increment retry count for non-retry scenarios")
    void updateStatusShouldNotIncrementRetryCountForNonRetryScenarios() {
        VideoProcessingEntity entity = createTestEntity();
        
        // Set initial retry count
        entity.setRetryCount(1);
        
        // Update to a status other than PENDING
        entity.updateStatus(STATUS_PROCESSING, "Processing video", false, null, null);
        
        assertEquals(1, entity.getRetryCount());
    }
    
    @Test
    @DisplayName("updateStatus should not increment retry count when retryCount is 0")
    void updateStatusShouldNotIncrementRetryCountWhenRetryCountIsZero() {
        VideoProcessingEntity entity = createTestEntity();
        
        // Ensure retry count is 0
        entity.setRetryCount(0);
        
        // Update to PENDING
        entity.updateStatus(STATUS_PENDING, "Pending processing", false, null, null);
        
        assertEquals(0, entity.getRetryCount());
    }
    
    @Test
    @DisplayName("incrementRetryCount should increment counter and update timestamp")
    void incrementRetryCountShouldIncrementCounterAndUpdateTimestamp() {
        VideoProcessingEntity entity = createTestEntity();
        LocalDateTime originalUpdatedAt = entity.getUpdatedAt();
        
        // Wait briefly to ensure time difference
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            // Ignore
        }
        
        entity.incrementRetryCount();
        
        assertEquals(1, entity.getRetryCount());
        assertTrue(entity.getUpdatedAt().isAfter(originalUpdatedAt));
    }
    
    @Test
    @DisplayName("Error message should be stored correctly")
    void errorMessageShouldBeStoredCorrectly() {
        VideoProcessingEntity entity = createTestEntity();
        String errorMessage = "File format not supported";
        
        entity.updateStatus(STATUS_FAILED, "Processing failed", true, errorMessage, null);
        
        assertEquals(errorMessage, entity.getErrorMessage());
        assertEquals(STATUS_FAILED, entity.getStatusValue());
        assertTrue(entity.isStatusIsTerminal());
    }
    
    @Test
    @DisplayName("Entity should correctly track vclipping result fields")
    void entityShouldCorrectlyTrackVclippingResultFields() {
        VideoProcessingEntity entity = createTestEntity();
        
        // Set vclipping result fields
        entity.setOutputLocation("output/video123/");
        entity.setExtractedFrameCount(120);
        entity.setProcessingDurationMs(5000L);
        entity.setDownloadReady(true);
        
        assertEquals("output/video123/", entity.getOutputLocation());
        assertEquals(Integer.valueOf(120), entity.getExtractedFrameCount());
        assertEquals(Long.valueOf(5000L), entity.getProcessingDurationMs());
        assertTrue(entity.isDownloadReady());
    }
    
    private VideoProcessingEntity createTestEntity() {
        return new VideoProcessingEntity(
            VIDEO_ID,
            USER_ID,
            FILENAME,
            FILE_SIZE,
            FORMAT,
            CONTENT_TYPE,
            STORAGE_REF,
            STATUS_PENDING,
            STATUS_DESC,
            false
        );
    }
}
