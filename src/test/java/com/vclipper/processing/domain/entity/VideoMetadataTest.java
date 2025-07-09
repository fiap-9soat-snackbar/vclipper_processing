package com.vclipper.processing.domain.entity;

import com.vclipper.processing.domain.enums.VideoFormat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class VideoMetadataTest {

    @Test
    @DisplayName("Should create valid VideoMetadata instance with all fields")
    void shouldCreateValidInstance() {
        // Arrange
        String filename = "sample_video.mp4";
        long size = 1024 * 1024 * 10; // 10MB
        VideoFormat format = VideoFormat.MP4;
        String contentType = "video/mp4";
        String reference = "storage/videos/sample_video.mp4";
        
        // Act
        VideoMetadata metadata = new VideoMetadata(filename, size, format, contentType, reference);
        
        // Assert
        assertEquals(filename, metadata.originalFilename());
        assertEquals(size, metadata.fileSizeBytes());
        assertEquals(format, metadata.format());
        assertEquals(contentType, metadata.contentType());
        assertEquals(reference, metadata.storageReference());
        assertTrue(metadata.isValid());
    }
    
    @Test
    @DisplayName("Should calculate file size in MB correctly")
    void shouldCalculateFileSizeInMB() {
        // Arrange
        long sizeInBytes = 5 * 1024 * 1024; // 5MB in bytes
        VideoMetadata metadata = new VideoMetadata(
            "video.mp4", sizeInBytes, VideoFormat.MP4, "video/mp4", "reference");
        
        // Act
        double sizeInMB = metadata.getFileSizeMB();
        
        // Assert
        assertEquals(5.0, sizeInMB, 0.001, "File size should be 5MB");
    }
    
    @ParameterizedTest
    @CsvSource({
        "sample.mp4, mp4",
        "video.MOV, mov",
        "my_video.avi, avi",
        "RECORDING.WMV, wmv",
        "file.with.multiple.dots.flv, flv"
    })
    @DisplayName("Should extract correct file extension from filename")
    void shouldExtractCorrectFileExtension(String filename, String expectedExtension) {
        // Arrange
        VideoMetadata metadata = new VideoMetadata(
            filename, 1024, VideoFormat.MP4, "video/mp4", "reference");
        
        // Act
        String extension = metadata.getFileExtension();
        
        // Assert
        assertEquals(expectedExtension, extension);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"file", "", "noextension"})
    @DisplayName("Should return empty string for filenames without extension")
    void shouldReturnEmptyStringForFilenamesWithoutExtension(String filename) {
        // Arrange
        VideoMetadata metadata = new VideoMetadata(
            filename, 1024, VideoFormat.MP4, "video/mp4", "reference");
        
        // Act
        String extension = metadata.getFileExtension();
        
        // Assert
        assertEquals("", extension);
    }
    
    @Test
    @DisplayName("Should return empty string for null filename")
    void shouldReturnEmptyStringForNullFilename() {
        // Arrange
        VideoMetadata metadata = new VideoMetadata(
            null, 1024, VideoFormat.MP4, "video/mp4", "reference");
        
        // Act
        String extension = metadata.getFileExtension();
        
        // Assert
        assertEquals("", extension);
    }
    
    @Test
    @DisplayName("Should validate metadata is valid when all required fields are present")
    void shouldValidateMetadataIsValid() {
        // Arrange
        VideoMetadata validMetadata = new VideoMetadata(
            "video.mp4", 1024, VideoFormat.MP4, "video/mp4", "reference");
        
        // Act & Assert
        assertTrue(validMetadata.isValid());
    }
    
    @Test
    @DisplayName("Should validate metadata is invalid when required fields are missing")
    void shouldValidateMetadataIsInvalid() {
        // Arrange
        VideoMetadata missingFilename = new VideoMetadata(
            "", 1024, VideoFormat.MP4, "video/mp4", "reference");
        VideoMetadata zeroSize = new VideoMetadata(
            "video.mp4", 0, VideoFormat.MP4, "video/mp4", "reference");
        VideoMetadata nullFormat = new VideoMetadata(
            "video.mp4", 1024, null, "video/mp4", "reference");
        VideoMetadata missingReference = new VideoMetadata(
            "video.mp4", 1024, VideoFormat.MP4, "video/mp4", "");
        
        // Act & Assert
        assertFalse(missingFilename.isValid());
        assertFalse(zeroSize.isValid());
        assertFalse(nullFormat.isValid());
        assertFalse(missingReference.isValid());
    }
    
    @Test
    @DisplayName("Should correctly check if file size exceeds maximum allowed size")
    void shouldCheckIfFileSizeExceedsMaximum() {
        // Arrange
        long fileSize = 50 * 1024 * 1024; // 50MB
        VideoMetadata metadata = new VideoMetadata(
            "video.mp4", fileSize, VideoFormat.MP4, "video/mp4", "reference");
        
        // Act & Assert
        assertTrue(metadata.exceedsMaxSize(40 * 1024 * 1024)); // Exceeds 40MB
        assertFalse(metadata.exceedsMaxSize(60 * 1024 * 1024)); // Doesn't exceed 60MB
        assertFalse(metadata.exceedsMaxSize(50 * 1024 * 1024)); // Equal to 50MB (not exceeding)
    }
}
