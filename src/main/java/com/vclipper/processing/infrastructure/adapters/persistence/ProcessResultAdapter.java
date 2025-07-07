package com.vclipper.processing.infrastructure.adapters.persistence;

import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.ports.FileStoragePort;
import com.vclipper.processing.application.ports.ProcessResultPort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * MongoDB adapter implementation for ProcessResultPort.
 * 
 * Handles updating video processing status based on results from vclipping service.
 * Follows the same pattern as VideoRepositoryAdapter for consistency.
 */
@Component
public class ProcessResultAdapter implements ProcessResultPort {
    
    private static final Logger logger = LoggerFactory.getLogger(ProcessResultAdapter.class);
    
    private final VideoProcessingRepository repository;
    private final FileStoragePort fileStorage;
    
    public ProcessResultAdapter(VideoProcessingRepository repository, 
                              FileStoragePort fileStorage) {
        this.repository = repository;
        this.fileStorage = fileStorage;
    }
    
    @Override
    public Result<Void, ProcessingError> updateVideoStatus(
            String videoId,
            String userId,
            ProcessingStatus newStatus,
            String outputLocation,
            Integer extractedFrameCount,
            Long processingDurationMs,
            String errorMessage) {
        
        logger.debug("Updating video status: videoId={}, userId={}, status={}", 
                    videoId, userId, newStatus.value());
        
        try {
            // 1. Find the video processing entity
            Optional<VideoProcessingEntity> entityOpt = repository.findById(videoId);
            
            if (entityOpt.isEmpty()) {
                logger.warn("Video not found for status update: videoId={}, userId={}", videoId, userId);
                return Result.failure(ProcessingError.notFound(
                    "Video not found: " + videoId
                ));
            }
            
            VideoProcessingEntity entity = entityOpt.get();
            
            // 2. Verify user ownership
            if (!userId.equals(entity.getUserId())) {
                logger.warn("User mismatch for video status update: videoId={}, expectedUser={}, actualUser={}", 
                           videoId, userId, entity.getUserId());
                return Result.failure(ProcessingError.invalidRequest(
                    "User does not own this video: " + videoId
                ));
            }
            
            // 3. Check if status transition is valid
            ProcessingStatus currentStatus = ProcessingStatus.fromValue(entity.getStatusValue());
            if (!currentStatus.canTransitionTo(newStatus)) {
                logger.warn("Invalid status transition: videoId={}, from={}, to={}", 
                           videoId, currentStatus.value(), newStatus.value());
                return Result.failure(ProcessingError.invalidRequest(
                    String.format("Cannot transition from %s to %s", 
                                currentStatus.value(), newStatus.value())
                ));
            }
            
            // 4. Update entity with new status and result data
            entity.setStatusValue(newStatus.value());
            entity.setStatusDescription(newStatus.description());
            entity.setStatusIsTerminal(newStatus.isTerminal());
            entity.setUpdatedAt(LocalDateTime.now());
            
            // Update result-specific fields
            if (outputLocation != null) {
                entity.setOutputLocation(outputLocation);
                
                // For COMPLETED status, also set processedFileReference for download readiness
                if (newStatus.equals(ProcessingStatus.COMPLETED)) {
                    entity.setProcessedFileReference(outputLocation);
                    logger.info("✅ Set processedFileReference for download: {}", outputLocation);
                }
            }
            
            if (extractedFrameCount != null) {
                entity.setExtractedFrameCount(extractedFrameCount);
            }
            
            if (processingDurationMs != null) {
                entity.setProcessingDurationMs(processingDurationMs);
            }
            
            if (errorMessage != null) {
                entity.setErrorMessage(errorMessage);
            }
            
            // Set download readiness for completed videos
            if (newStatus.equals(ProcessingStatus.COMPLETED)) {
                entity.setDownloadReady(true);
            }
            
            // 5. Save the updated entity
            repository.save(entity);
            
            logger.info("✅ Video status updated successfully: videoId={}, status={}, frames={}, duration={}ms", 
                       videoId, newStatus.value(), extractedFrameCount, processingDurationMs);
            
            return Result.success(null);
            
        } catch (IllegalArgumentException e) {
            logger.error("Invalid status value: {}", e.getMessage());
            return Result.failure(ProcessingError.invalidRequest("Invalid status: " + e.getMessage()));
            
        } catch (Exception e) {
            logger.error("Failed to update video status: videoId={}, error={}", videoId, e.getMessage(), e);
            return Result.failure(ProcessingError.databaseError(
                "Failed to update video status: " + e.getMessage(), e
            ));
        }
    }
    
    @Override
    public Result<Boolean, ProcessingError> canUpdateVideo(String videoId, String userId) {
        logger.debug("Checking if video can be updated: videoId={}, userId={}", videoId, userId);
        
        try {
            Optional<VideoProcessingEntity> entityOpt = repository.findById(videoId);
            
            if (entityOpt.isEmpty()) {
                logger.debug("Video not found: videoId={}", videoId);
                return Result.success(false);
            }
            
            VideoProcessingEntity entity = entityOpt.get();
            
            // Check user ownership
            if (!userId.equals(entity.getUserId())) {
                logger.debug("User does not own video: videoId={}, userId={}", videoId, userId);
                return Result.success(false);
            }
            
            // Check if status is not terminal
            ProcessingStatus currentStatus = ProcessingStatus.fromValue(entity.getStatusValue());
            boolean canUpdate = !currentStatus.isTerminal();
            
            logger.debug("Video update check result: videoId={}, canUpdate={}, currentStatus={}", 
                        videoId, canUpdate, currentStatus.value());
            
            return Result.success(canUpdate);
            
        } catch (Exception e) {
            logger.error("Failed to check video update eligibility: videoId={}, error={}", 
                        videoId, e.getMessage(), e);
            return Result.failure(ProcessingError.databaseError(
                "Failed to check video update eligibility: " + e.getMessage(), e
            ));
        }
    }
    
    @Override
    public Result<String, ProcessingError> copyProcessedFile(String sourceS3Key, String targetS3Key) {
        logger.info("📁 Copying processed file: {} → {}", sourceS3Key, targetS3Key);
        
        try {
            // Use FileStoragePort to copy file within S3
            fileStorage.copyFile(sourceS3Key, targetS3Key);
            
            logger.info("✅ Successfully copied processed file to: {}", targetS3Key);
            return Result.success(targetS3Key);
            
        } catch (Exception e) {
            logger.error("❌ Failed to copy processed file: {} → {}, error: {}", 
                        sourceS3Key, targetS3Key, e.getMessage(), e);
            return Result.failure(ProcessingError.fileCopyFailed(
                String.format("Failed to copy file from %s to %s", sourceS3Key, targetS3Key), e));
        }
    }
}
