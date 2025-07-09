package com.vclipper.processing.infrastructure.adapters.persistence;

import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.ports.FileStoragePort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessResultAdapterTest {

    @Mock
    private VideoProcessingRepository repository;

    @Mock
    private FileStoragePort fileStorage;

    private ProcessResultAdapter adapter;

    private static final String VIDEO_ID = "video-123";
    private static final String USER_ID = "user-456";
    private static final String OUTPUT_LOCATION = "s3://bucket/output/video-123.mp4";
    private static final int FRAME_COUNT = 150;
    private static final long PROCESSING_DURATION = 5000L;
    private static final String ERROR_MESSAGE = "Processing failed due to invalid format";

    @BeforeEach
    void setUp() {
        adapter = new ProcessResultAdapter(repository, fileStorage);
    }

    private VideoProcessingEntity createVideoEntity(ProcessingStatus status) {
        VideoProcessingEntity entity = new VideoProcessingEntity();
        entity.setVideoId(VIDEO_ID);
        entity.setUserId(USER_ID);
        entity.setStatusValue(status.value());
        entity.setStatusDescription(status.description());
        entity.setStatusIsTerminal(status.isTerminal());
        entity.setCreatedAt(LocalDateTime.now().minusMinutes(30));
        entity.setUpdatedAt(LocalDateTime.now().minusMinutes(25));
        return entity;
    }

    @Test
    void updateVideoStatus_Success() {
        // Arrange
        VideoProcessingEntity entity = createVideoEntity(ProcessingStatus.PROCESSING);
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act
        Result<Void, ProcessingError> result = adapter.updateVideoStatus(
                VIDEO_ID, USER_ID, ProcessingStatus.COMPLETED,
                OUTPUT_LOCATION, FRAME_COUNT, PROCESSING_DURATION, null);

        // Assert
        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<VideoProcessingEntity> entityCaptor = ArgumentCaptor.forClass(VideoProcessingEntity.class);
        verify(repository).save(entityCaptor.capture());

        VideoProcessingEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getStatusValue()).isEqualTo(ProcessingStatus.COMPLETED.value());
        assertThat(savedEntity.getOutputLocation()).isEqualTo(OUTPUT_LOCATION);
        assertThat(savedEntity.getExtractedFrameCount()).isEqualTo(FRAME_COUNT);
        assertThat(savedEntity.getProcessingDurationMs()).isEqualTo(PROCESSING_DURATION);
        assertThat(savedEntity.isDownloadReady()).isTrue();
        assertThat(savedEntity.getProcessedFileReference()).isEqualTo(OUTPUT_LOCATION);
    }

    @Test
    void updateVideoStatus_VideoNotFound() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.empty());

        // Act
        Result<Void, ProcessingError> result = adapter.updateVideoStatus(
                VIDEO_ID, USER_ID, ProcessingStatus.COMPLETED,
                OUTPUT_LOCATION, FRAME_COUNT, PROCESSING_DURATION, null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void updateVideoStatus_UserMismatch() {
        // Arrange
        VideoProcessingEntity entity = createVideoEntity(ProcessingStatus.PROCESSING);
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act
        Result<Void, ProcessingError> result = adapter.updateVideoStatus(
                VIDEO_ID, "different-user", ProcessingStatus.COMPLETED,
                OUTPUT_LOCATION, FRAME_COUNT, PROCESSING_DURATION, null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void updateVideoStatus_InvalidTransition() {
        // Arrange
        VideoProcessingEntity entity = createVideoEntity(ProcessingStatus.COMPLETED);
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act
        Result<Void, ProcessingError> result = adapter.updateVideoStatus(
                VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING,
                OUTPUT_LOCATION, FRAME_COUNT, PROCESSING_DURATION, null);

        // Assert
        assertThat(result.isFailure()).isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void updateVideoStatus_WithError() {
        // Arrange
        VideoProcessingEntity entity = createVideoEntity(ProcessingStatus.PROCESSING);
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act
        Result<Void, ProcessingError> result = adapter.updateVideoStatus(
                VIDEO_ID, USER_ID, ProcessingStatus.FAILED,
                null, null, PROCESSING_DURATION, ERROR_MESSAGE);

        // Assert
        assertThat(result.isSuccess()).isTrue();

        ArgumentCaptor<VideoProcessingEntity> entityCaptor = ArgumentCaptor.forClass(VideoProcessingEntity.class);
        verify(repository).save(entityCaptor.capture());

        VideoProcessingEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getStatusValue()).isEqualTo(ProcessingStatus.FAILED.value());
        assertThat(savedEntity.getErrorMessage()).isEqualTo(ERROR_MESSAGE);
        assertThat(savedEntity.getProcessingDurationMs()).isEqualTo(PROCESSING_DURATION);
    }

    @Test
    void updateVideoStatus_RepositoryException() {
        // Arrange
        VideoProcessingEntity entity = createVideoEntity(ProcessingStatus.PROCESSING);
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));
        when(repository.save(any())).thenThrow(new RuntimeException("Database connection error"));

        // Act
        Result<Void, ProcessingError> result = adapter.updateVideoStatus(
                VIDEO_ID, USER_ID, ProcessingStatus.COMPLETED,
                OUTPUT_LOCATION, FRAME_COUNT, PROCESSING_DURATION, null);

        // Assert
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void canUpdateVideo_Success_NotTerminal() {
        // Arrange
        VideoProcessingEntity entity = createVideoEntity(ProcessingStatus.PROCESSING);
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act
        Result<Boolean, ProcessingError> result = adapter.canUpdateVideo(VIDEO_ID, USER_ID);

        // Assert
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void canUpdateVideo_Success_TerminalStatus() {
        // Arrange
        VideoProcessingEntity entity = createVideoEntity(ProcessingStatus.COMPLETED);
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act
        Result<Boolean, ProcessingError> result = adapter.canUpdateVideo(VIDEO_ID, USER_ID);

        // Assert
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void canUpdateVideo_VideoNotFound() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.empty());

        // Act
        Result<Boolean, ProcessingError> result = adapter.canUpdateVideo(VIDEO_ID, USER_ID);

        // Assert
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void canUpdateVideo_UserMismatch() {
        // Arrange
        VideoProcessingEntity entity = createVideoEntity(ProcessingStatus.PROCESSING);
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act
        Result<Boolean, ProcessingError> result = adapter.canUpdateVideo(VIDEO_ID, "different-user");

        // Assert
        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void canUpdateVideo_Exception() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenThrow(new RuntimeException("Database error"));

        // Act
        Result<Boolean, ProcessingError> result = adapter.canUpdateVideo(VIDEO_ID, USER_ID);

        // Assert
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void copyProcessedFile_Success() {
        // Arrange
        String sourceKey = "temp/processed/video-123.mp4";
        String targetKey = "final/video-123/output.mp4";
        doNothing().when(fileStorage).copyFile(sourceKey, targetKey);

        // Act
        Result<String, ProcessingError> result = adapter.copyProcessedFile(sourceKey, targetKey);

        // Assert
        assertThat(result.isSuccess()).isTrue();
        verify(fileStorage).copyFile(sourceKey, targetKey);
    }

    @Test
    void copyProcessedFile_Failure() {
        // Arrange
        String sourceKey = "temp/processed/video-123.mp4";
        String targetKey = "final/video-123/output.mp4";
        doThrow(new RuntimeException("File copy failed")).when(fileStorage).copyFile(anyString(), anyString());

        // Act
        Result<String, ProcessingError> result = adapter.copyProcessedFile(sourceKey, targetKey);

        // Assert
        assertThat(result.isFailure()).isTrue();
    }
}
