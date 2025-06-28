package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.common.VideoNotReadyError;
import com.vclipper.processing.application.ports.FileStoragePort;
import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;

/**
 * Use case for generating video download URLs
 * Provides secure, time-limited download URLs for processed videos
 * 
 * Uses Result pattern for business state validation (video not ready)
 * Uses exceptions for security boundaries (video not found/unauthorized)
 */
public class GetVideoDownloadUrlUseCase {
    
    private final VideoRepositoryPort videoRepository;
    private final FileStoragePort fileStorage;
    private final int downloadUrlExpirationMinutes;
    
    public GetVideoDownloadUrlUseCase(VideoRepositoryPort videoRepository, 
                                    FileStoragePort fileStorage,
                                    int downloadUrlExpirationMinutes) {
        this.videoRepository = videoRepository;
        this.fileStorage = fileStorage;
        this.downloadUrlExpirationMinutes = downloadUrlExpirationMinutes;
    }
    
    /**
     * Generate download URL for processed video
     * 
     * @param videoId Video identifier
     * @param userId User identifier (for authorization)
     * @return Result containing download URL response or business error
     */
    public Result<DownloadUrlResponse, VideoNotReadyError> execute(String videoId, String userId) {
        VideoProcessingRequest request = videoRepository.findById(videoId)
            .orElseThrow(() -> new VideoNotFoundException(videoId));
        
        // Verify user owns this video (security boundary - keep as exception)
        if (!request.getUserId().equals(userId)) {
            throw new VideoNotFoundException(videoId);
        }
        
        // Check if video is ready for download (business state - use Result pattern)
        if (!request.isDownloadReady()) {
            return Result.failure(VideoNotReadyError.forDownload(videoId, request.getStatus()));
        }
        
        // Generate presigned download URL
        String downloadUrl = fileStorage.generateDownloadUrl(
            request.getProcessedFileReference(),
            downloadUrlExpirationMinutes
        );
        
        return Result.success(new DownloadUrlResponse(
            videoId,
            request.getMetadata().originalFilename(),
            downloadUrl,
            downloadUrlExpirationMinutes
        ));
    }
    
    /**
     * Download URL response
     */
    public record DownloadUrlResponse(
        String videoId,
        String originalFilename,
        String downloadUrl,
        int expirationMinutes
    ) {}
}
