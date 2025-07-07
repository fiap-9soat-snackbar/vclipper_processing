package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.ports.NotificationPort;
import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.NotificationType;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Consolidated use case for updating video processing status.
 * 
 * Handles all status transitions with proper business rule validation:
 * - PENDING → PROCESSING (when SQS message is sent)
 * - PROCESSING → COMPLETED (when vclipping finishes successfully)
 * - PROCESSING → FAILED (when vclipping fails)
 * - Any status → FAILED (for error scenarios)
 * 
 * Features:
 * - Result pattern for type-safe error handling
 * - Automatic notification sending
 * - Async video processing trigger
 * - Controller compatibility
 * - Clean architecture compliance
 */
public class UpdateVideoStatusUseCase {
    
    private static final Logger logger = LoggerFactory.getLogger(UpdateVideoStatusUseCase.class);
    
    private final VideoRepositoryPort repositoryPort;
    private final NotificationPort notification;
    
    public UpdateVideoStatusUseCase(VideoRepositoryPort repositoryPort, 
                                  NotificationPort notification) {
        this.repositoryPort = repositoryPort;
        this.notification = notification;
    }
    
    /**
     * Update video processing status (Controller interface - maintains compatibility).
     * 
     * @param videoId Video identifier
     * @param newStatus New processing status
     * @param processedFileS3Key S3 key for processed file (for COMPLETED status)
     * @param errorMessage Error message (for FAILED status)
     * @return VideoStatusUpdateResponse for controller
     */
    public VideoStatusUpdateResponse execute(String videoId, ProcessingStatus newStatus, 
                                           String processedFileS3Key, String errorMessage) {
        logger.info("🔄 Updating video status via controller: videoId={}, newStatus={}", videoId, newStatus.value());
        
        Result<VideoStatusUpdateResponse, ProcessingError> result = executeWithResult(
            videoId, null, newStatus, processedFileS3Key, errorMessage, true
        );
        
        if (result.isSuccess()) {
            return result.getValue().orElseThrow();
        } else {
            ProcessingError error = result.getError().orElseThrow();
            logger.error("❌ Status update failed: {}", error.message());
            throw new RuntimeException(error.message(), error.cause());
        }
    }
    
    /**
     * Update video processing status (Internal interface with Result pattern).
     * 
     * @param videoId Video identifier
     * @param userId User identifier (for security validation, optional for controller calls)
     * @param newStatus New processing status
     * @return Result with success/failure information
     */
    public Result<VideoStatusUpdateResponse, ProcessingError> execute(String videoId, String userId, ProcessingStatus newStatus) {
        return executeWithResult(videoId, userId, newStatus, null, null, false);
    }
    
    /**
     * Update video processing status with full parameters (Internal interface).
     * 
     * @param videoId Video identifier
     * @param userId User identifier (for security validation)
     * @param newStatus New processing status
     * @param processedFileS3Key S3 key for processed file (for COMPLETED status)
     * @param errorMessage Error message (for FAILED status)
     * @return Result with success/failure information
     */
    public Result<VideoStatusUpdateResponse, ProcessingError> execute(String videoId, String userId, 
                                                                    ProcessingStatus newStatus, 
                                                                    String processedFileS3Key, 
                                                                    String errorMessage) {
        return executeWithResult(videoId, userId, newStatus, processedFileS3Key, errorMessage, false);
    }
    
    /**
     * Core implementation with Result pattern and comprehensive error handling.
     */
    private Result<VideoStatusUpdateResponse, ProcessingError> executeWithResult(
            String videoId, String userId, ProcessingStatus newStatus, 
            String processedFileS3Key, String errorMessage, boolean skipUserValidation) {
        
        logger.info("🔄 Processing status update: videoId={}, newStatus={}", videoId, newStatus.value());
        
        try {
            // 1. Find the video processing request
            Optional<VideoProcessingRequest> requestOpt = repositoryPort.findById(videoId);
            
            if (requestOpt.isEmpty()) {
                logger.warn("❌ Video not found for status update: videoId={}", videoId);
                return Result.failure(ProcessingError.notFound("Video not found: " + videoId));
            }
            
            VideoProcessingRequest request = requestOpt.get();
            
            // 2. Verify user ownership (security check) - skip for controller calls
            if (!skipUserValidation && userId != null && !userId.equals(request.getUserId())) {
                logger.warn("❌ User mismatch for video status update: videoId={}, expectedUser={}, actualUser={}", 
                           videoId, userId, request.getUserId());
                return Result.failure(ProcessingError.invalidRequest(
                    "User does not own this video: " + videoId));
            }
            
            ProcessingStatus previousStatus = request.getStatus();
            
            // 3. Validate status transition
            if (!previousStatus.canTransitionTo(newStatus)) {
                String message = String.format("Cannot transition from %s to %s", 
                                              previousStatus.value(), newStatus.value());
                logger.warn("❌ Invalid status transition: videoId={}, {}", videoId, message);
                return Result.failure(ProcessingError.invalidRequest(message));
            }
            
            // 4. Update status based on new status with proper domain logic
            updateStatusWithSideEffects(request, newStatus, processedFileS3Key, errorMessage);
            
            // 5. Save updated request
            VideoProcessingRequest updatedRequest = repositoryPort.save(request);
            
            // 6. Send notification
            sendStatusNotification(updatedRequest, newStatus, processedFileS3Key, errorMessage);
            
            logger.info("✅ Video status updated successfully: videoId={}, from={}, to={}", 
                       videoId, previousStatus.value(), newStatus.value());
            
            VideoStatusUpdateResponse response = new VideoStatusUpdateResponse(
                updatedRequest.getVideoId(),
                updatedRequest.getUserId(),
                previousStatus,
                updatedRequest.getStatus(),
                updatedRequest.getProcessedFileReference(),
                updatedRequest.getUpdatedAt()
            );
            
            return Result.success(response);
            
        } catch (VideoNotFoundException e) {
            logger.warn("❌ Video not found: videoId={}", videoId);
            return Result.failure(ProcessingError.notFound("Video not found: " + videoId));
            
        } catch (IllegalArgumentException e) {
            logger.error("❌ Invalid argument: videoId={}, error={}", videoId, e.getMessage());
            return Result.failure(ProcessingError.invalidRequest("Invalid request: " + e.getMessage()));
            
        } catch (IllegalStateException e) {
            logger.error("❌ Invalid state transition: videoId={}, error={}", videoId, e.getMessage());
            return Result.failure(ProcessingError.invalidRequest("Invalid state transition: " + e.getMessage()));
            
        } catch (Exception e) {
            logger.error("❌ Failed to update video status: videoId={}, error={}", videoId, e.getMessage(), e);
            return Result.failure(ProcessingError.databaseError(
                "Failed to update video status: " + e.getMessage(), e));
        }
    }
    
    /**
     * Update status with appropriate side effects based on the new status.
     */
    private void updateStatusWithSideEffects(VideoProcessingRequest request, ProcessingStatus newStatus, 
                                           String processedFileS3Key, String errorMessage) {
        
        switch (newStatus.value()) {
            case "PROCESSING" -> {
                request.startProcessing();
                logger.info("📊 Video marked as processing: videoId={}", request.getVideoId());
                
                // Note: Async video processing disabled - we now use real vclipping processing via SQS
                // The video will be processed by vclipping service and result will come via SQS
                logger.info("🔄 Video queued for vclipping processing via SQS");
            }
            case "COMPLETED" -> {
                if (processedFileS3Key == null || processedFileS3Key.trim().isEmpty()) {
                    throw new IllegalArgumentException("Processed file S3 key is required for COMPLETED status");
                }
                request.markAsCompleted(processedFileS3Key);
                logger.info("✅ Video marked as completed: videoId={}, processedFileS3Key={}", 
                           request.getVideoId(), processedFileS3Key);
            }
            case "FAILED" -> {
                if (errorMessage == null || errorMessage.trim().isEmpty()) {
                    throw new IllegalArgumentException("Error message is required for FAILED status");
                }
                request.markAsFailed(errorMessage);
                logger.warn("❌ Video marked as failed: videoId={}, error={}", 
                           request.getVideoId(), errorMessage);
            }
            default -> {
                request.updateStatus(newStatus);
                logger.info("📊 Video status updated: videoId={}, status={}", 
                           request.getVideoId(), newStatus.value());
            }
        }
    }
    
    /**
     * Send appropriate notification based on status change.
     */
    private void sendStatusNotification(VideoProcessingRequest request, ProcessingStatus newStatus, 
                                      String processedFileS3Key, String errorMessage) {
        
        if (notification == null) {
            logger.debug("📧 Notification service not available, skipping notification");
            return;
        }
        
        try {
            NotificationType notificationType;
            NotificationPort.NotificationData notificationData;
            
            switch (newStatus.value()) {
                case "PROCESSING" -> {
                    notificationType = NotificationType.PROCESSING_STARTED;
                    notificationData = NotificationPort.NotificationData.forProcessingStart(
                        request.getMetadata().originalFilename());
                }
                case "COMPLETED" -> {
                    notificationType = NotificationType.PROCESSING_COMPLETED;
                    // Note: downloadUrl would need to be generated separately if needed
                    notificationData = NotificationPort.NotificationData.forCompletion(
                        request.getMetadata().originalFilename(), null);
                }
                case "FAILED" -> {
                    notificationType = NotificationType.PROCESSING_FAILED;
                    notificationData = NotificationPort.NotificationData.forFailure(
                        request.getMetadata().originalFilename(), errorMessage);
                }
                default -> {
                    logger.debug("📧 No notification configured for status: {}", newStatus.value());
                    return;
                }
            }
            
            notification.sendNotification(request.getUserId(), notificationType, notificationData);
            logger.info("📧 Notification sent for status change: videoId={}, status={}", 
                       request.getVideoId(), newStatus.value());
            
        } catch (Exception e) {
            logger.error("📧 Failed to send notification: videoId={}, status={}, error={}", 
                        request.getVideoId(), newStatus.value(), e.getMessage(), e);
            // Don't fail the status update if notification fails
        }
    }
    
    /**
     * Response record for status update operations.
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
