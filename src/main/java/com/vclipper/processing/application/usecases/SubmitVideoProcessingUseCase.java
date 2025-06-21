package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.ports.*;
import com.vclipper.processing.domain.entity.*;
import com.vclipper.processing.domain.enums.VideoFormat;
import com.vclipper.processing.domain.exceptions.InvalidVideoFormatException;
import com.vclipper.processing.domain.exceptions.VideoUploadException;

import java.io.InputStream;

/**
 * Use case for submitting video processing requests
 * Orchestrates video upload, validation, storage, and processing queue submission
 */
public class SubmitVideoProcessingUseCase {
    
    private final VideoRepositoryPort videoRepository;
    private final FileStoragePort fileStorage;
    private final MessageQueuePort messageQueue;
    private final NotificationPort notification;
    private final UserServicePort userService;
    private final long maxFileSizeBytes;
    
    public SubmitVideoProcessingUseCase(VideoRepositoryPort videoRepository,
                                      FileStoragePort fileStorage,
                                      MessageQueuePort messageQueue,
                                      NotificationPort notification,
                                      UserServicePort userService,
                                      long maxFileSizeBytes) {
        this.videoRepository = videoRepository;
        this.fileStorage = fileStorage;
        this.messageQueue = messageQueue;
        this.notification = notification;
        this.userService = userService;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
    
    /**
     * Submit video for processing
     * 
     * @param request Video processing submission request
     * @return Processing response with video ID and status
     */
    public VideoProcessingResponse execute(VideoProcessingSubmission request) {
        try {
            // 1. Validate user
            validateUser(request.userId());
            
            // 2. Validate video file
            validateVideoFile(request);
            
            // 3. Store video file
            String storageReference = storeVideoFile(request);
            
            // 4. Create video metadata
            VideoMetadata metadata = createVideoMetadata(request, storageReference);
            
            // 5. Create processing request entity
            VideoProcessingRequest processingRequest = new VideoProcessingRequest(request.userId(), metadata);
            
            // 6. Save processing request
            VideoProcessingRequest savedRequest = videoRepository.save(processingRequest);
            
            // 7. Send processing message to queue
            sendProcessingMessage(savedRequest);
            
            // 8. Send upload confirmation notification
            sendUploadNotification(savedRequest);
            
            // 9. Return response
            return VideoProcessingResponse.success(
                savedRequest.getVideoId(),
                savedRequest.getStatus(),
                "Video uploaded successfully and queued for processing"
            );
            
        } catch (Exception e) {
            throw new VideoUploadException("Failed to submit video for processing: " + e.getMessage(), e);
        }
    }
    
    private void validateUser(String userId) {
        if (!userService.isActiveUser(userId)) {
            throw new VideoUploadException("User not found or inactive: " + userId);
        }
    }
    
    private void validateVideoFile(VideoProcessingSubmission request) {
        // Validate file size
        if (request.fileSizeBytes() > maxFileSizeBytes) {
            throw new VideoUploadException(
                String.format("File size %d bytes exceeds maximum allowed size %d bytes", 
                    request.fileSizeBytes(), maxFileSizeBytes)
            );
        }
        
        // Validate file format
        if (!VideoFormat.isSupported(getFileExtension(request.originalFilename()))) {
            throw new InvalidVideoFormatException(getFileExtension(request.originalFilename()));
        }
        
        // Validate MIME type
        if (!VideoFormat.isSupportedMimeType(request.contentType())) {
            throw new InvalidVideoFormatException("Unsupported MIME type: " + request.contentType());
        }
    }
    
    private String storeVideoFile(VideoProcessingSubmission request) {
        return fileStorage.store(
            request.inputStream(),
            request.originalFilename(),
            request.contentType(),
            request.fileSizeBytes()
        );
    }
    
    private VideoMetadata createVideoMetadata(VideoProcessingSubmission request, String storageReference) {
        VideoFormat format = VideoFormat.fromExtension(getFileExtension(request.originalFilename()));
        
        return new VideoMetadata(
            request.originalFilename(),
            request.fileSizeBytes(),
            format,
            request.contentType(),
            storageReference
        );
    }
    
    private void sendProcessingMessage(VideoProcessingRequest request) {
        MessageQueuePort.VideoProcessingMessage message = MessageQueuePort.VideoProcessingMessage.from(
            request.getVideoId(),
            request.getUserId(),
            request.getMetadata().storageReference(),
            request.getMetadata().originalFilename(),
            request.getMetadata().fileSizeBytes(),
            request.getMetadata().contentType()
        );
        
        messageQueue.sendProcessingMessage(message);
    }
    
    private void sendUploadNotification(VideoProcessingRequest request) {
        NotificationPort.NotificationData notificationData = 
            NotificationPort.NotificationData.forUpload(request.getMetadata().originalFilename());
        
        notification.sendNotification(
            request.getUserId(),
            NotificationType.UPLOAD_CONFIRMED,
            notificationData
        );
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Video processing submission request
     */
    public record VideoProcessingSubmission(
        String userId,
        InputStream inputStream,
        String originalFilename,
        String contentType,
        long fileSizeBytes
    ) {}
    
    /**
     * Video processing response
     */
    public record VideoProcessingResponse(
        String videoId,
        ProcessingStatus status,
        String message,
        boolean success
    ) {
        public static VideoProcessingResponse success(String videoId, ProcessingStatus status, String message) {
            return new VideoProcessingResponse(videoId, status, message, true);
        }
        
        public static VideoProcessingResponse failure(String message) {
            return new VideoProcessingResponse(null, null, message, false);
        }
    }
}
