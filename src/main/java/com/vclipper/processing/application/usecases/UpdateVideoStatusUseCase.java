package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Use case for updating video processing status
 * Called by vclipping service when processing is completed or failed
 */
public class UpdateVideoStatusUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateVideoStatusUseCase.class);
    
    private final VideoRepositoryPort repositoryPort;
    
    public UpdateVideoStatusUseCase(VideoRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }
    
    /**
     * Update video processing status
     */
    public VideoStatusUpdateResponse execute(String videoId, ProcessingStatus newStatus, 
                                           String processedFileS3Key, String errorMessage) {
        logger.info("Updating video status: videoId={}, newStatus={}", videoId, newStatus.value());
        
        // Retrieve existing video processing request
        VideoProcessingRequest request = repositoryPort.findById(videoId)
            .orElseThrow(() -> {
                logger.warn("Video not found for status update: videoId={}", videoId);
                return new VideoNotFoundException("Video not found with ID: " + videoId);
            });
        
        ProcessingStatus previousStatus = request.getStatus();
        
        // Validate status transition
        if (!previousStatus.canTransitionTo(newStatus)) {
            String message = String.format("Invalid status transition from %s to %s for video %s", 
                previousStatus.value(), newStatus.value(), videoId);
            logger.warn(message);
            throw new IllegalStateException(message);
        }
        
        // Update status based on new status
        if (newStatus.equals(ProcessingStatus.COMPLETED)) {
            if (processedFileS3Key == null || processedFileS3Key.trim().isEmpty()) {
                throw new IllegalArgumentException("Processed file S3 key is required for COMPLETED status");
            }
            request.markAsCompleted(processedFileS3Key);
            logger.info("Video marked as completed: videoId={}, processedFileS3Key={}", videoId, processedFileS3Key);
        } else if (newStatus.equals(ProcessingStatus.FAILED)) {
            if (errorMessage == null || errorMessage.trim().isEmpty()) {
                throw new IllegalArgumentException("Error message is required for FAILED status");
            }
            request.markAsFailed(errorMessage);
            logger.warn("Video marked as failed: videoId={}, error={}", videoId, errorMessage);
        } else if (newStatus.equals(ProcessingStatus.PROCESSING)) {
            request.startProcessing();
            logger.info("Video marked as processing: videoId={}", videoId);
        } else {
            request.updateStatus(newStatus);
            logger.info("Video status updated: videoId={}, status={}", videoId, newStatus.value());
        }
        
        // Save updated request
        VideoProcessingRequest updatedRequest = repositoryPort.save(request);
        
        logger.info("Successfully updated video status: videoId={}, previousStatus={}, newStatus={}", 
            videoId, previousStatus.value(), newStatus.value());
        
        return new VideoStatusUpdateResponse(
            updatedRequest.getVideoId(),
            updatedRequest.getUserId(),
            previousStatus,
            updatedRequest.getStatus(),
            updatedRequest.getProcessedFileReference(),
            updatedRequest.getUpdatedAt()
        );
    }
    
    /**
     * Response record for status update operations
     */
    public record VideoStatusUpdateResponse(
        String videoId,
        String userId,
        ProcessingStatus previousStatus,
        ProcessingStatus newStatus,
        String processedFileS3Key,
        LocalDateTime updatedAt
    ) {}
}
