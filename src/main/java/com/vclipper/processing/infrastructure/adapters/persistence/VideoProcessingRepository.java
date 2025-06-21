package com.vclipper.processing.infrastructure.adapters.persistence;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data MongoDB repository for video processing requests
 * Provides database operations for video processing entities
 */
@Repository
public interface VideoProcessingRepository extends MongoRepository<VideoProcessingEntity, String> {
    
    /**
     * Find all videos for a specific user, ordered by creation date (newest first)
     */
    List<VideoProcessingEntity> findByUserIdOrderByCreatedAtDesc(String userId);
    
    /**
     * Find videos by status
     */
    List<VideoProcessingEntity> findByStatusValue(String statusValue);
    
    /**
     * Find videos by user and status
     */
    List<VideoProcessingEntity> findByUserIdAndStatusValue(String userId, String statusValue);
    
    /**
     * Check if video exists for user (for authorization)
     */
    boolean existsByVideoIdAndUserId(String videoId, String userId);
    
    /**
     * Find video by ID and user ID (for authorization)
     */
    Optional<VideoProcessingEntity> findByVideoIdAndUserId(String videoId, String userId);
    
    /**
     * Count videos by user
     */
    long countByUserId(String userId);
    
    /**
     * Count videos by status
     */
    long countByStatusValue(String statusValue);
    
    /**
     * Find videos that can be retried (FAILED status)
     */
    @Query("{ 'statusValue': 'FAILED', 'retryCount': { $lt: ?0 } }")
    List<VideoProcessingEntity> findRetryableVideos(int maxRetries);
    
    /**
     * Find videos older than specified hours in PENDING status (for cleanup/monitoring)
     */
    @Query("{ 'statusValue': 'PENDING', 'createdAt': { $lt: ?0 } }")
    List<VideoProcessingEntity> findStuckPendingVideos(java.time.LocalDateTime cutoffTime);
}
