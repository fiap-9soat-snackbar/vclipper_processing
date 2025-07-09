package com.vclipper.processing.infrastructure.adapters.persistence;

import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoMetadata;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.enums.VideoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EntityMapperTest {

    private EntityMapper entityMapper;
    private VideoProcessingRequest domainEntity;
    private VideoProcessingEntity persistenceEntity;
    private LocalDateTime fixedTime;
    private String videoId;
    private String userId;
    private String originalFilename;
    private String storageReference;
    private String processedFileReference;
    private String errorMessage;

    @BeforeEach
    void setUp() {
        entityMapper = new EntityMapper();
        videoId = UUID.randomUUID().toString();
        userId = "user123";
        originalFilename = "test-video.mp4";
        storageReference = "storage/original/test-video.mp4";
        processedFileReference = "storage/processed/test-video.mp4";
        errorMessage = "Test error message";
        fixedTime = LocalDateTime.now();

        // Create test domain entity
        VideoMetadata metadata = new VideoMetadata(
                originalFilename,
                1024L,
                VideoFormat.MP4,
                "video/mp4",
                storageReference
        );

        ProcessingStatus status = new ProcessingStatus(
                "PROCESSING",
                "Video is being processed",
                false
        );

        domainEntity = new VideoProcessingRequest(
                videoId,
                userId,
                metadata,
                status,
                fixedTime,
                fixedTime,
                errorMessage,
                processedFileReference
        );

        // Create test persistence entity
        persistenceEntity = new VideoProcessingEntity(
                videoId,
                userId,
                originalFilename,
                1024L,
                "MP4",
                "video/mp4",
                storageReference,
                "PROCESSING",
                "Video is being processed",
                false
        );
        persistenceEntity.setCreatedAt(fixedTime);
        persistenceEntity.setUpdatedAt(fixedTime);
        persistenceEntity.setErrorMessage(errorMessage);
        persistenceEntity.setProcessedFileReference(processedFileReference);
    }

    @Test
    void toEntity_shouldMapDomainEntityToPersistenceEntity() {
        // When
        VideoProcessingEntity result = entityMapper.toEntity(domainEntity);

        // Then
        assertEquals(videoId, result.getVideoId());
        assertEquals(userId, result.getUserId());
        assertEquals(originalFilename, result.getOriginalFilename());
        assertEquals(1024L, result.getFileSizeBytes());
        assertEquals("MP4", result.getVideoFormat());
        assertEquals("video/mp4", result.getContentType());
        assertEquals(storageReference, result.getStorageReference());
        assertEquals("PROCESSING", result.getStatusValue());
        assertEquals("Video is being processed", result.getStatusDescription());
        assertFalse(result.isStatusIsTerminal());
        assertEquals(fixedTime, result.getCreatedAt());
        assertEquals(fixedTime, result.getUpdatedAt());
        assertEquals(errorMessage, result.getErrorMessage());
        assertEquals(processedFileReference, result.getProcessedFileReference());
    }

    @Test
    void toDomain_shouldMapPersistenceEntityToDomainEntity() {
        // When
        VideoProcessingRequest result = entityMapper.toDomain(persistenceEntity);

        // Then
        assertEquals(videoId, result.getVideoId());
        assertEquals(userId, result.getUserId());
        assertEquals(originalFilename, result.getMetadata().originalFilename());
        assertEquals(1024L, result.getMetadata().fileSizeBytes());
        assertEquals(VideoFormat.MP4, result.getMetadata().format());
        assertEquals("video/mp4", result.getMetadata().contentType());
        assertEquals(storageReference, result.getMetadata().storageReference());
        assertEquals("PROCESSING", result.getStatus().value());
        assertEquals("Video is being processed", result.getStatus().description());
        assertFalse(result.getStatus().isTerminal());
        assertEquals(fixedTime, result.getCreatedAt());
        assertEquals(fixedTime, result.getUpdatedAt());
        assertEquals(errorMessage, result.getErrorMessage());
        assertEquals(processedFileReference, result.getProcessedFileReference());
    }


    @Test
    void updateEntity_shouldUpdateWithErrorMessage() {
        // Given
        VideoProcessingEntity entityToUpdate = new VideoProcessingEntity(
                videoId,
                userId,
                originalFilename,
                1024L,
                "MP4",
                "video/mp4",
                storageReference,
                "PROCESSING",
                "Video is being processed",
                false
        );

        // Update domain entity with failure
        domainEntity.markAsFailed("Processing failed due to invalid format");

        // When
        entityMapper.updateEntity(entityToUpdate, domainEntity);

        // Then
        assertEquals("Processing failed due to invalid format", entityToUpdate.getErrorMessage());
        assertTrue(entityToUpdate.isStatusIsTerminal());
    }
}
