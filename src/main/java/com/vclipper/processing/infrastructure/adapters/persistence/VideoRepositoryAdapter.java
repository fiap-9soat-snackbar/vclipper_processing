package com.vclipper.processing.infrastructure.adapters.persistence;

import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * MongoDB adapter implementation for VideoRepositoryPort
 * Bridges the gap between domain and infrastructure layers
 */
@Component
public class VideoRepositoryAdapter implements VideoRepositoryPort {
    
    private static final Logger logger = LoggerFactory.getLogger(VideoRepositoryAdapter.class);
    
    private final VideoProcessingRepository repository;
    private final EntityMapper mapper;
    
    public VideoRepositoryAdapter(VideoProcessingRepository repository, EntityMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
    
    @Override
    public VideoProcessingRequest save(VideoProcessingRequest request) {
        logger.debug("Saving video processing request: {}", request.getVideoId());
        
        try {
            // Check if entity already exists
            Optional<VideoProcessingEntity> existingEntity = repository.findById(request.getVideoId());
            
            if (existingEntity.isPresent()) {
                // Update existing entity
                VideoProcessingEntity entity = existingEntity.get();
                mapper.updateEntity(entity, request);
                repository.save(entity);
                logger.debug("Updated existing video processing request: {}", request.getVideoId());
            } else {
                // Create new entity
                VideoProcessingEntity entity = mapper.toEntity(request);
                repository.save(entity);
                logger.debug("Created new video processing request: {}", request.getVideoId());
            }
            
            return request;
            
        } catch (Exception e) {
            logger.error("Error saving video processing request: {}", request.getVideoId(), e);
            throw new RuntimeException("Failed to save video processing request", e);
        }
    }
    
    @Override
    public Optional<VideoProcessingRequest> findById(String videoId) {
        logger.debug("Finding video processing request by ID: {}", videoId);
        
        try {
            return repository.findById(videoId)
                .map(mapper::toDomain);
        } catch (Exception e) {
            logger.error("Error finding video processing request by ID: {}", videoId, e);
            return Optional.empty();
        }
    }
    
    @Override
    public List<VideoProcessingRequest> findByUserId(String userId) {
        logger.debug("Finding video processing requests for user: {}", userId);
        
        try {
            return repository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(mapper::toDomain)
                .toList();
        } catch (Exception e) {
            logger.error("Error finding video processing requests for user: {}", userId, e);
            return List.of();
        }
    }
    
    @Override
    public List<VideoProcessingRequest> findByStatus(String status) {
        logger.debug("Finding video processing requests by status: {}", status);
        
        try {
            return repository.findByStatusValue(status)
                .stream()
                .map(mapper::toDomain)
                .toList();
        } catch (Exception e) {
            logger.error("Error finding video processing requests by status: {}", status, e);
            return List.of();
        }
    }
    
    @Override
    public boolean existsById(String videoId) {
        logger.debug("Checking if video processing request exists: {}", videoId);
        
        try {
            return repository.existsById(videoId);
        } catch (Exception e) {
            logger.error("Error checking if video processing request exists: {}", videoId, e);
            return false;
        }
    }
    
    @Override
    public void deleteById(String videoId) {
        logger.debug("Deleting video processing request: {}", videoId);
        
        try {
            repository.deleteById(videoId);
            logger.info("Deleted video processing request: {}", videoId);
        } catch (Exception e) {
            logger.error("Error deleting video processing request: {}", videoId, e);
            throw new RuntimeException("Failed to delete video processing request", e);
        }
    }
    
    @Override
    public VideoProcessingRequest updateStatus(String videoId, String status, String errorMessage) {
        logger.debug("Updating status for video processing request: {} to {}", videoId, status);
        
        try {
            Optional<VideoProcessingEntity> entityOpt = repository.findById(videoId);
            if (entityOpt.isEmpty()) {
                throw new RuntimeException("Video processing request not found: " + videoId);
            }
            
            VideoProcessingEntity entity = entityOpt.get();
            
            // Update status based on the new status value
            switch (status) {
                case "PROCESSING" -> entity.updateStatus("PROCESSING", "Video is currently being processed", false, null, null);
                case "COMPLETED" -> entity.updateStatus("COMPLETED", "Video processing completed successfully", true, null, entity.getProcessedFileReference());
                case "FAILED" -> entity.updateStatus("FAILED", "Video processing failed", true, errorMessage, null);
                default -> throw new IllegalArgumentException("Invalid status: " + status);
            }
            
            repository.save(entity);
            
            return mapper.toDomain(entity);
            
        } catch (Exception e) {
            logger.error("Error updating status for video processing request: {}", videoId, e);
            throw new RuntimeException("Failed to update video processing request status", e);
        }
    }
}
