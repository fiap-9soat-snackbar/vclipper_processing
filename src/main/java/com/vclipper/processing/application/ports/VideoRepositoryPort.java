package com.vclipper.processing.application.ports;

import com.vclipper.processing.domain.entity.VideoProcessingRequest;

import java.util.List;
import java.util.Optional;

/**
 * Port interface for video processing request persistence operations
 * This interface defines the contract for data persistence without exposing implementation details
 */
public interface VideoRepositoryPort {
    
    /**
     * Save a video processing request
     */
    VideoProcessingRequest save(VideoProcessingRequest request);
    
    /**
     * Find a video processing request by ID
     */
    Optional<VideoProcessingRequest> findById(String videoId);
    
    /**
     * Find all video processing requests for a specific user
     */
    List<VideoProcessingRequest> findByUserId(String userId);
    
    /**
     * Find all video processing requests with a specific status
     */
    List<VideoProcessingRequest> findByStatus(String status);
    
    /**
     * Check if a video processing request exists
     */
    boolean existsById(String videoId);
    
    /**
     * Delete a video processing request
     */
    void deleteById(String videoId);
    
    /**
     * Update the status of a video processing request
     */
    VideoProcessingRequest updateStatus(String videoId, String status, String errorMessage);
}
