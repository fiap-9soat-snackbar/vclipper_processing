package com.vclipper.processing.infrastructure.adapters.processing;

import com.vclipper.processing.application.ports.FileStoragePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZipVideoProcessingAdapterTest {

    @Mock
    private FileStoragePort fileStoragePort;

    private ZipVideoProcessingAdapter adapter;

    private static final String VIDEO_ID = "video-123";
    private static final String ORIGINAL_S3_KEY = "originals/video-123.mp4";
    private static final String ORIGINAL_FILENAME = "test-video.mp4";
    private static final String PROCESSED_S3_KEY = "processed/video-123/test-video.zip";

    @BeforeEach
    void setUp() {
        adapter = new ZipVideoProcessingAdapter(fileStoragePort);
    }

    @Test
    void processVideo_shouldCreateZipAndReturnS3Key() throws IOException {
        // Arrange
        byte[] mockVideoContent = "mock video content".getBytes();
        ByteArrayInputStream mockVideoStream = new ByteArrayInputStream(mockVideoContent);

        when(fileStoragePort.downloadFile(ORIGINAL_S3_KEY)).thenReturn(mockVideoStream);
        when(fileStoragePort.storeProcessedFile(any(InputStream.class), eq(VIDEO_ID), eq(ORIGINAL_FILENAME)))
                .thenReturn(PROCESSED_S3_KEY);

        // Act
        String result = adapter.processVideo(VIDEO_ID, ORIGINAL_S3_KEY, ORIGINAL_FILENAME);

        // Assert
        assertEquals(PROCESSED_S3_KEY, result);

        // Verify interactions
        verify(fileStoragePort).downloadFile(ORIGINAL_S3_KEY);

        // Verify the uploaded ZIP contains valid ZIP content
        ArgumentCaptor<InputStream> zipStreamCaptor = ArgumentCaptor.forClass(InputStream.class);
        verify(fileStoragePort).storeProcessedFile(
                zipStreamCaptor.capture(),
                eq(VIDEO_ID),
                eq(ORIGINAL_FILENAME)
        );

        InputStream capturedStream = zipStreamCaptor.getValue();
        byte[] zipBytes = capturedStream.readAllBytes();

        // Check that we have valid ZIP data (ZIP files start with PK header)
        assertTrue(zipBytes.length > 0);
        assertEquals(0x50, zipBytes[0] & 0xFF); // 'P'
        assertEquals(0x4B, zipBytes[1] & 0xFF); // 'K'
    }

    @Test
    void processVideo_shouldPropagateUploadException() throws IOException {
        // Arrange
        byte[] mockVideoContent = "mock video content".getBytes();
        ByteArrayInputStream mockVideoStream = new ByteArrayInputStream(mockVideoContent);

        when(fileStoragePort.downloadFile(ORIGINAL_S3_KEY)).thenReturn(mockVideoStream);
        when(fileStoragePort.storeProcessedFile(any(), eq(VIDEO_ID), eq(ORIGINAL_FILENAME)))
                .thenThrow(new RuntimeException("Upload failed"));

        // Act & Assert
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> adapter.processVideo(VIDEO_ID, ORIGINAL_S3_KEY, ORIGINAL_FILENAME)
        );

        assertTrue(exception.getMessage().contains("Failed to process video to ZIP"));

        // Verify interactions
        verify(fileStoragePort).downloadFile(ORIGINAL_S3_KEY);
        verify(fileStoragePort).storeProcessedFile(any(), eq(VIDEO_ID), eq(ORIGINAL_FILENAME));
    }
}
