package com.vclipper.processing.infrastructure.adapters.persistence;

import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoMetadata;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.enums.VideoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoRepositoryAdapterTest {

    @Mock
    private VideoProcessingRepository repository;

    @Mock
    private EntityMapper mapper;

    @InjectMocks
    private VideoRepositoryAdapter adapter;

    private VideoProcessingRequest domainRequest;
    private VideoProcessingEntity entity;
    private final String VIDEO_ID = "test-video-id";
    private final String USER_ID = "test-user-id";

    @BeforeEach
    void setUp() {
        // Setup test data
        VideoMetadata metadata = new VideoMetadata(
                "test.mp4",
                1024L,
                VideoFormat.MP4,
                "video/mp4",
                "storage/test.mp4"
        );

        ProcessingStatus status = new ProcessingStatus(
                "PENDING",
                "Waiting for processing",
                false
        );

        LocalDateTime now = LocalDateTime.now();
        domainRequest = new VideoProcessingRequest(
                VIDEO_ID,
                USER_ID,
                metadata,
                status,
                now,
                now,
                null,
                null
        );

        entity = new VideoProcessingEntity(
                VIDEO_ID,
                USER_ID,
                "test.mp4",
                1024L,
                "MP4",
                "video/mp4",
                "storage/test.mp4",
                "PENDING",
                "Waiting for processing",
                false
        );
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
    }

    @Test
    void save_WhenEntityDoesNotExist_ShouldCreateNewEntity() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.empty());
        when(mapper.toEntity(domainRequest)).thenReturn(entity);

        // Act
        VideoProcessingRequest result = adapter.save(domainRequest);

        // Assert
        assertNotNull(result);
        assertEquals(domainRequest, result);
        verify(repository).findById(VIDEO_ID);
        verify(mapper).toEntity(domainRequest);
        verify(repository).save(entity);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void save_WhenEntityExists_ShouldUpdateExistingEntity() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act
        VideoProcessingRequest result = adapter.save(domainRequest);

        // Assert
        assertNotNull(result);
        assertEquals(domainRequest, result);
        verify(repository).findById(VIDEO_ID);
        verify(mapper).updateEntity(entity, domainRequest);
        verify(repository).save(entity);
        verifyNoMoreInteractions(mapper, repository);
    }

    @Test
    void save_WhenExceptionOccurs_ShouldThrowRuntimeException() {
        // Arrange
        when(repository.findById(anyString())).thenThrow(new RuntimeException("Database error"));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> adapter.save(domainRequest));
        assertThat(exception.getMessage()).contains("Failed to save video processing request");
    }

    @Test
    void findById_WhenEntityExists_ShouldReturnDomainObject() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainRequest);

        // Act
        Optional<VideoProcessingRequest> result = adapter.findById(VIDEO_ID);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(domainRequest, result.get());
        verify(repository).findById(VIDEO_ID);
        verify(mapper).toDomain(entity);
    }

    @Test
    void findById_WhenEntityDoesNotExist_ShouldReturnEmptyOptional() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.empty());

        // Act
        Optional<VideoProcessingRequest> result = adapter.findById(VIDEO_ID);

        // Assert
        assertFalse(result.isPresent());
        verify(repository).findById(VIDEO_ID);
        verifyNoInteractions(mapper);
    }

    @Test
    void findById_WhenExceptionOccurs_ShouldReturnEmptyOptional() {
        // Arrange
        when(repository.findById(anyString())).thenThrow(new RuntimeException("Database error"));

        // Act
        Optional<VideoProcessingRequest> result = adapter.findById(VIDEO_ID);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void findByUserId_ShouldReturnListOfDomainObjects() {
        // Arrange
        List<VideoProcessingEntity> entities = Arrays.asList(entity, entity);
        when(repository.findByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(entities);
        when(mapper.toDomain(any(VideoProcessingEntity.class))).thenReturn(domainRequest);

        // Act
        List<VideoProcessingRequest> results = adapter.findByUserId(USER_ID);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(repository).findByUserIdOrderByCreatedAtDesc(USER_ID);
        verify(mapper, times(2)).toDomain(entity);
    }

    @Test
    void findByUserId_WhenExceptionOccurs_ShouldReturnEmptyList() {
        // Arrange
        when(repository.findByUserIdOrderByCreatedAtDesc(anyString()))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        List<VideoProcessingRequest> results = adapter.findByUserId(USER_ID);

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void findByStatus_ShouldReturnListOfDomainObjects() {
        // Arrange
        String status = "PENDING";
        List<VideoProcessingEntity> entities = Arrays.asList(entity, entity);
        when(repository.findByStatusValue(status)).thenReturn(entities);
        when(mapper.toDomain(any(VideoProcessingEntity.class))).thenReturn(domainRequest);

        // Act
        List<VideoProcessingRequest> results = adapter.findByStatus(status);

        // Assert
        assertNotNull(results);
        assertEquals(2, results.size());
        verify(repository).findByStatusValue(status);
        verify(mapper, times(2)).toDomain(entity);
    }

    @Test
    void findByStatus_WhenExceptionOccurs_ShouldReturnEmptyList() {
        // Arrange
        when(repository.findByStatusValue(anyString()))
            .thenThrow(new RuntimeException("Database error"));

        // Act
        List<VideoProcessingRequest> results = adapter.findByStatus("PENDING");

        // Assert
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void existsById_WhenEntityExists_ShouldReturnTrue() {
        // Arrange
        when(repository.existsById(VIDEO_ID)).thenReturn(true);

        // Act
        boolean result = adapter.existsById(VIDEO_ID);

        // Assert
        assertTrue(result);
        verify(repository).existsById(VIDEO_ID);
    }

    @Test
    void existsById_WhenExceptionOccurs_ShouldReturnFalse() {
        // Arrange
        when(repository.existsById(anyString())).thenThrow(new RuntimeException("Database error"));

        // Act
        boolean result = adapter.existsById(VIDEO_ID);

        // Assert
        assertFalse(result);
    }

    @Test
    void deleteById_ShouldCallRepository() {
        // Act
        adapter.deleteById(VIDEO_ID);

        // Assert
        verify(repository).deleteById(VIDEO_ID);
    }

    @Test
    void deleteById_WhenExceptionOccurs_ShouldThrowRuntimeException() {
        // Arrange
        doThrow(new RuntimeException("Database error")).when(repository).deleteById(anyString());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> adapter.deleteById(VIDEO_ID));
        assertThat(exception.getMessage()).contains("Failed to delete video processing request");
    }

    @Test
    void updateStatus_WhenEntityExists_ShouldUpdateStatusAndReturnDomainObject() {
        // Arrange
        String status = "COMPLETED";
        String errorMessage = null;

        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));
        when(mapper.toDomain(entity)).thenReturn(domainRequest);

        // Act
        VideoProcessingRequest result = adapter.updateStatus(VIDEO_ID, status, errorMessage);

        // Assert
        assertNotNull(result);
        assertEquals(domainRequest, result);
        verify(repository).findById(VIDEO_ID);
        verify(repository).save(entity);
        verify(mapper).toDomain(entity);
    }

    @Test
    void updateStatus_WhenEntityDoesNotExist_ShouldThrowRuntimeException() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.empty());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class,
            () -> adapter.updateStatus(VIDEO_ID, "COMPLETED", null));
    }

    @Test
    void updateStatus_WithInvalidStatus_ShouldThrowIllegalArgumentException() {
        // Arrange
        when(repository.findById(VIDEO_ID)).thenReturn(Optional.of(entity));

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class,
            () -> adapter.updateStatus(VIDEO_ID, "INVALID_STATUS", null));
        assertThat(exception.getMessage()).contains("Failed to update video processing request status");
    }
}
