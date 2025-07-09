package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.common.VideoNotReadyError;
import com.vclipper.processing.application.ports.FileStoragePort;
import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoMetadata;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import com.vclipper.processing.domain.enums.VideoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetVideoDownloadUrlUseCaseTest {

    @Mock
    private VideoRepositoryPort videoRepository;

    @Mock
    private FileStoragePort fileStorage;

    private GetVideoDownloadUrlUseCase useCase;
    private final int expirationMinutes = 30;

    private static final String VIDEO_ID = "video-123";
    private static final String USER_ID = "user-456";
    private static final String OTHER_USER_ID = "user-789";
    private static final String PROCESSED_FILE_REF = "processed/video-123.mp4";
    private static final String ORIGINAL_FILENAME = "my-video.mp4";
    private static final String DOWNLOAD_URL = "https://storage.example.com/videos/processed/video-123.mp4?token=abc123";

    @BeforeEach
    void setUp() {
        useCase = new GetVideoDownloadUrlUseCase(videoRepository, fileStorage, expirationMinutes);
    }

    @Test
    void shouldGenerateDownloadUrlWhenVideoIsReady() {
        // Arrange
        VideoProcessingRequest request = createVideoProcessingRequest(USER_ID, ProcessingStatus.COMPLETED);
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.of(request));
        when(fileStorage.generateDownloadUrl(PROCESSED_FILE_REF, expirationMinutes)).thenReturn(DOWNLOAD_URL);

        // Act
        Result<GetVideoDownloadUrlUseCase.DownloadUrlResponse, VideoNotReadyError> result =
            useCase.execute(VIDEO_ID, USER_ID);

        // Assert
        assertTrue(result.isSuccess());
        assertFalse(result.isFailure());

        // Fixed line - properly extract the value from the Result
        GetVideoDownloadUrlUseCase.DownloadUrlResponse response = result.getValue().orElseThrow();
        assertEquals(VIDEO_ID, response.videoId());
        assertEquals(ORIGINAL_FILENAME, response.originalFilename());
        assertEquals(DOWNLOAD_URL, response.downloadUrl());
        assertEquals(expirationMinutes, response.expirationMinutes());

        verify(videoRepository).findById(VIDEO_ID);
        verify(fileStorage).generateDownloadUrl(PROCESSED_FILE_REF, expirationMinutes);
    }

    @Test
    void shouldThrowExceptionWhenVideoNotFound() {
        // Arrange
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(VideoNotFoundException.class, () -> useCase.execute(VIDEO_ID, USER_ID));
        verify(videoRepository).findById(VIDEO_ID);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void shouldThrowExceptionWhenUserDoesNotOwnVideo() {
        // Arrange
        VideoProcessingRequest request = createVideoProcessingRequest(USER_ID, ProcessingStatus.COMPLETED);
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.of(request));

        // Act & Assert
        assertThrows(VideoNotFoundException.class, () -> useCase.execute(VIDEO_ID, OTHER_USER_ID));
        verify(videoRepository).findById(VIDEO_ID);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void shouldReturnErrorWhenVideoIsNotReadyForDownload() {
        // Arrange
        VideoProcessingRequest request = createVideoProcessingRequest(USER_ID, ProcessingStatus.PROCESSING);
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.of(request));

        // Act
        Result<GetVideoDownloadUrlUseCase.DownloadUrlResponse, VideoNotReadyError> result =
            useCase.execute(VIDEO_ID, USER_ID);

        // Assert
        assertTrue(result.isFailure());
        assertFalse(result.isSuccess());

        VideoNotReadyError error = result.getError().orElseThrow();
        assertEquals(VIDEO_ID, error.videoId());
        assertEquals(ProcessingStatus.PROCESSING, error.currentStatus());
        assertEquals("download", error.operation());
        assertTrue(error.message().contains("not ready for download"));

        verify(videoRepository).findById(VIDEO_ID);
        verifyNoInteractions(fileStorage);
    }

    @Test
    void shouldReturnErrorWhenVideoHasFailedProcessing() {
        // Arrange
        VideoProcessingRequest request = createVideoProcessingRequest(USER_ID, ProcessingStatus.FAILED);
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.of(request));

        // Act
        Result<GetVideoDownloadUrlUseCase.DownloadUrlResponse, VideoNotReadyError> result =
            useCase.execute(VIDEO_ID, USER_ID);

        // Assert
        assertTrue(result.isFailure());

        VideoNotReadyError error = result.getError().orElseThrow();
        assertEquals(VIDEO_ID, error.videoId());
        assertEquals(ProcessingStatus.FAILED, error.currentStatus());

        verify(videoRepository).findById(VIDEO_ID);
        verifyNoInteractions(fileStorage);
    }

    private VideoProcessingRequest createVideoProcessingRequest(String userId, ProcessingStatus status) {
        VideoMetadata metadata = new VideoMetadata(
            ORIGINAL_FILENAME,
            1024 * 1024 * 10L, // 10MB as long
            VideoFormat.MP4,
            "video/mp4",
            "1920x1080" // resolution
        );

        // Using the constructor with all parameters
        VideoProcessingRequest request = new VideoProcessingRequest(
            VIDEO_ID,
            userId,
            metadata,
            status,
            LocalDateTime.now(),
            LocalDateTime.now(),
            status == ProcessingStatus.FAILED ? "Processing error" : null,
            PROCESSED_FILE_REF
        );

        return request;
    }
}
