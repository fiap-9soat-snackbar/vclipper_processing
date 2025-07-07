package com.vclipper.processing.application.ports;

import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.domain.entity.ProcessingStatus;

/**
 * Port interface for processing vclipping results.
 * 
 * Defines the contract for updating video processing status based on
 * results received from the vclipping service via SQS.
 * 
 * This port follows clean architecture principles:
 * - Defined in Application layer
 * - Implemented by Infrastructure layer
 * - Used by Application layer use cases
 * - Domain-focused interface (no infrastructure concerns)
 */
public interface ProcessResultPort {
    
    /**
     * Update video processing status based on vclipping service results.
     * 
     * @param videoId The unique identifier of the video
     * @param userId The user who owns the video
     * @param newStatus The new processing status (COMPLETED or FAILED)
     * @param outputLocation Location of the processed output (if successful)
     * @param extractedFrameCount Number of frames extracted (if successful)
     * @param processingDurationMs Processing duration in milliseconds
     * @param errorMessage Error message (if failed)
     * @return Result indicating success or failure of the status update
     */
    Result<Void, ProcessingError> updateVideoStatus(
        String videoId,
        String userId,
        ProcessingStatus newStatus,
        String outputLocation,
        Integer extractedFrameCount,
        Long processingDurationMs,
        String errorMessage
    );
    
    /**
     * Check if a video exists and can be updated.
     * 
     * @param videoId The unique identifier of the video
     * @param userId The user who owns the video
     * @return Result indicating if the video exists and is updatable
     */
    Result<Boolean, ProcessingError> canUpdateVideo(String videoId, String userId);
    
    /**
     * Copy processed file from vclipping output location to processed-videos location.
     * 
     * This enables proper file organization and access control:
     * - vclipping outputs go to vclipping-frames/ (temporary)
     * - Final processed files go to processed-videos/ (for download)
     * 
     * @param sourceS3Key Source S3 key (vclipping output location)
     * @param targetS3Key Target S3 key (processed-videos location)
     * @return Result with target S3 key if successful, or error if failed
     */
    Result<String, ProcessingError> copyProcessedFile(String sourceS3Key, String targetS3Key);
}
