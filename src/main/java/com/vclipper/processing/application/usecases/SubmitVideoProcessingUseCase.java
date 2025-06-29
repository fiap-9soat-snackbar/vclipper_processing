package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.common.VideoUploadError;
import com.vclipper.processing.application.ports.*;
import com.vclipper.processing.domain.entity.*;
import com.vclipper.processing.domain.enums.VideoFormat;
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
    private final MimeTypeDetectionPort mimeTypeDetection;
    private final long maxFileSizeBytes;
    
    public SubmitVideoProcessingUseCase(VideoRepositoryPort videoRepository,
                                      FileStoragePort fileStorage,
                                      MessageQueuePort messageQueue,
                                      NotificationPort notification,
                                      UserServicePort userService,
                                      MimeTypeDetectionPort mimeTypeDetection,
                                      long maxFileSizeBytes) {
        this.videoRepository = videoRepository;
        this.fileStorage = fileStorage;
        this.messageQueue = messageQueue;
        this.notification = notification;
        this.userService = userService;
        this.mimeTypeDetection = mimeTypeDetection;
        this.maxFileSizeBytes = maxFileSizeBytes;
    }
    
    /**
     * Submit video for processing using Result pattern
     * 
     * @param request Video processing submission request
     * @return Result containing processing response or validation error
     */
    public Result<VideoProcessingResponse, VideoUploadError> execute(VideoProcessingSubmission request) {
        try {
            // 1. Validate video file (business validation - use Result pattern)
            Result<Void, VideoUploadError> validationResult = validateVideoFile(request);
            if (validationResult.isFailure()) {
                return Result.failure(validationResult.getError().get());
            }
            
            // 2. Validate user (business validation - use Result pattern)
            Result<Void, VideoUploadError> userValidationResult = validateUser(request.userId());
            if (userValidationResult.isFailure()) {
                return Result.failure(userValidationResult.getError().get());
            }
            
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
            return Result.success(VideoProcessingResponse.success(
                savedRequest.getVideoId(),
                savedRequest.getStatus(),
                "Video uploaded successfully and queued for processing"
            ));
            
        } catch (Exception e) {
            throw new VideoUploadException("Failed to submit video for processing: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate video file using Result pattern for business validation
     */
    private Result<Void, VideoUploadError> validateVideoFile(VideoProcessingSubmission request) {
        // Validate file size
        if (request.fileSizeBytes() > maxFileSizeBytes) {
            return Result.failure(VideoUploadError.fileTooLarge(request.fileSizeBytes(), maxFileSizeBytes));
        }
        
        // Validate file is not empty
        if (request.fileSizeBytes() == 0) {
            return Result.failure(VideoUploadError.emptyFile());
        }
        
        // Validate file format by extension
        String fileExtension = getFileExtension(request.originalFilename());
        if (!VideoFormat.isSupported(fileExtension)) {
            return Result.failure(VideoUploadError.invalidFormat(fileExtension));
        }
        
        // Detect and validate MIME type using content analysis
        String detectedMimeType = mimeTypeDetection.detectMimeType(
            request.inputStream(), 
            request.originalFilename(), 
            request.contentType()
        );
        
        if (detectedMimeType == null || !mimeTypeDetection.isSupportedVideoMimeType(detectedMimeType)) {
            return Result.failure(VideoUploadError.invalidFormat(
                String.format("Detected: %s, Provided: %s", detectedMimeType, request.contentType())
            ));
        }
        
        return Result.success(null);
    }
    
    /**
     * Validate user using Result pattern for business validation
     */
    private Result<Void, VideoUploadError> validateUser(String userId) {
        if (!userService.isActiveUser(userId)) {
            return Result.failure(VideoUploadError.invalidUser(userId));
        }
        return Result.success(null);
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
