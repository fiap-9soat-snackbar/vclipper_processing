package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.ports.FileStoragePort;
import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import com.vclipper.processing.domain.exceptions.VideoProcessingException;

/**
 * Use case for generating video download URLs
 * Provides secure, time-limited download URLs for processed videos
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
     * @return Download URL response
     */
    public DownloadUrlResponse execute(String videoId, String userId) {
        VideoProcessingRequest request = videoRepository.findById(videoId)
            .orElseThrow(() -> new VideoNotFoundException(videoId));
        
        // Verify user owns this video
        if (!request.getUserId().equals(userId)) {
            throw new VideoNotFoundException(videoId);
        }
        
        // Verify video is ready for download
        if (!request.isDownloadReady()) {
            throw new VideoProcessingException(
                "Video is not ready for download. Current status: " + request.getStatus().value()
            );
        }
        
        // Generate presigned download URL
        String downloadUrl = fileStorage.generateDownloadUrl(
            request.getProcessedFileReference(),
            downloadUrlExpirationMinutes
        );
        
        return new DownloadUrlResponse(
            videoId,
            request.getMetadata().originalFilename(),
            downloadUrl,
            downloadUrlExpirationMinutes
        );
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
