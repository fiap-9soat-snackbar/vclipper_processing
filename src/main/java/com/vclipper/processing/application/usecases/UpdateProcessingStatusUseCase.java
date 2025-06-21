package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.ports.NotificationPort;
import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.NotificationType;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;

/**
 * Use case for updating video processing status
 * Handles status updates from the video processing service and sends notifications
 */
public class UpdateProcessingStatusUseCase {
    
    private final VideoRepositoryPort videoRepository;
    private final NotificationPort notification;
    
    public UpdateProcessingStatusUseCase(VideoRepositoryPort videoRepository, 
                                       NotificationPort notification) {
        this.videoRepository = videoRepository;
        this.notification = notification;
    }
    
    /**
     * Update processing status and send notification
     * 
     * @param request Status update request
     * @return Updated processing status
     */
    public StatusUpdateResponse execute(StatusUpdateRequest request) {
        VideoProcessingRequest processingRequest = videoRepository.findById(request.videoId())
            .orElseThrow(() -> new VideoNotFoundException(request.videoId()));
        
        // Update status based on request type
        switch (request.newStatus().value()) {
            case "PROCESSING" -> {
                processingRequest.startProcessing();
                sendNotification(processingRequest, NotificationType.PROCESSING_STARTED, null, null);
            }
            case "COMPLETED" -> {
                processingRequest.markAsCompleted(request.processedFileReference());
                sendNotification(processingRequest, NotificationType.PROCESSING_COMPLETED, 
                    request.downloadUrl(), null);
            }
            case "FAILED" -> {
                processingRequest.markAsFailed(request.errorMessage());
                sendNotification(processingRequest, NotificationType.PROCESSING_FAILED, 
                    null, request.errorMessage());
            }
        }
        
        // Save updated request
        VideoProcessingRequest updatedRequest = videoRepository.save(processingRequest);
        
        return StatusUpdateResponse.from(updatedRequest);
    }
    
    private void sendNotification(VideoProcessingRequest request, NotificationType notificationType,
                                String downloadUrl, String errorMessage) {
        NotificationPort.NotificationData notificationData = switch (notificationType.type()) {
            case "PROCESSING_STARTED" -> NotificationPort.NotificationData.forProcessingStart(
                request.getMetadata().originalFilename());
            case "PROCESSING_COMPLETED" -> NotificationPort.NotificationData.forCompletion(
                request.getMetadata().originalFilename(), downloadUrl);
            case "PROCESSING_FAILED" -> NotificationPort.NotificationData.forFailure(
                request.getMetadata().originalFilename(), errorMessage);
            default -> NotificationPort.NotificationData.forUpload(
                request.getMetadata().originalFilename());
        };
        
        notification.sendNotification(request.getUserId(), notificationType, notificationData);
    }
    
    /**
     * Status update request
     */
    public record StatusUpdateRequest(
        String videoId,
        ProcessingStatus newStatus,
        String processedFileReference,
        String downloadUrl,
        String errorMessage
    ) {
        public static StatusUpdateRequest forProcessingStart(String videoId) {
            return new StatusUpdateRequest(videoId, ProcessingStatus.PROCESSING, null, null, null);
        }
        
        public static StatusUpdateRequest forCompletion(String videoId, String processedFileReference, String downloadUrl) {
            return new StatusUpdateRequest(videoId, ProcessingStatus.COMPLETED, processedFileReference, downloadUrl, null);
        }
        
        public static StatusUpdateRequest forFailure(String videoId, String errorMessage) {
            return new StatusUpdateRequest(videoId, ProcessingStatus.FAILED, null, null, errorMessage);
        }
    }
    
    /**
     * Status update response
     */
    public record StatusUpdateResponse(
        String videoId,
        ProcessingStatus status,
        String message
    ) {
        public static StatusUpdateResponse from(VideoProcessingRequest request) {
            return new StatusUpdateResponse(
                request.getVideoId(),
                request.getStatus(),
                "Status updated to " + request.getStatus().value()
            );
        }
    }
}
