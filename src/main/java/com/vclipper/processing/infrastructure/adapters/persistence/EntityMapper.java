package com.vclipper.processing.infrastructure.adapters.persistence;

import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoMetadata;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.enums.VideoFormat;
import org.springframework.stereotype.Component;

/**
 * Mapper between domain entities and MongoDB entities
 * Handles conversion between clean architecture domain and infrastructure layers
 */
@Component
public class EntityMapper {
    
    /**
     * Convert domain entity to MongoDB entity
     */
    public VideoProcessingEntity toEntity(VideoProcessingRequest domain) {
        VideoProcessingEntity entity = new VideoProcessingEntity(
            domain.getVideoId(),
            domain.getUserId(),
            domain.getMetadata().originalFilename(),
            domain.getMetadata().fileSizeBytes(),
            domain.getMetadata().format().name(),
            domain.getMetadata().contentType(),
            domain.getMetadata().storageReference(),
            domain.getStatus().value(),
            domain.getStatus().description(),
            domain.getStatus().isTerminal()
        );
        
        entity.setCreatedAt(domain.getCreatedAt());
        entity.setUpdatedAt(domain.getUpdatedAt());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setProcessedFileReference(domain.getProcessedFileReference());
        
        return entity;
    }
    
    /**
     * Convert MongoDB entity to domain entity
     */
    public VideoProcessingRequest toDomain(VideoProcessingEntity entity) {
        // Create processing status
        ProcessingStatus status = new ProcessingStatus(
            entity.getStatusValue(),
            entity.getStatusDescription(),
            entity.isStatusIsTerminal()
        );
        
        // Create video metadata
        VideoMetadata metadata = new VideoMetadata(
            entity.getOriginalFilename(),
            entity.getFileSizeBytes(),
            VideoFormat.valueOf(entity.getVideoFormat()),
            entity.getContentType(),
            entity.getStorageReference()
        );
        
        // Create domain entity using constructor for existing requests
        return new VideoProcessingRequest(
            entity.getVideoId(),
            entity.getUserId(),
            metadata,
            status,
            entity.getCreatedAt(),
            entity.getUpdatedAt(),
            entity.getErrorMessage(),
            entity.getProcessedFileReference()
        );
    }
    
    /**
     * Update entity from domain (for updates)
     */
    public void updateEntity(VideoProcessingEntity entity, VideoProcessingRequest domain) {
        entity.updateStatus(
            domain.getStatus().value(),
            domain.getStatus().description(),
            domain.getStatus().isTerminal(),
            domain.getErrorMessage(),
            domain.getProcessedFileReference()
        );
    }
}
