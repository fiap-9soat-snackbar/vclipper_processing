package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;

import java.time.LocalDateTime;

/**
 * Use case for retrieving video processing status
 * Provides current status and processing details for a video
 */
public class GetProcessingStatusUseCase {
    
    private final VideoRepositoryPort videoRepository;
    
    public GetProcessingStatusUseCase(VideoRepositoryPort videoRepository) {
        this.videoRepository = videoRepository;
    }
    
    /**
     * Get processing status for a video
     * 
     * @param videoId Video identifier
     * @param userId User identifier (for authorization)
     * @return Processing status response
     */
    public ProcessingStatusResponse execute(String videoId, String userId) {
        VideoProcessingRequest request = videoRepository.findById(videoId)
            .orElseThrow(() -> new VideoNotFoundException(videoId));
        
        // Verify user owns this video
        if (!request.getUserId().equals(userId)) {
            throw new VideoNotFoundException(videoId); // Don't reveal existence to unauthorized users
        }
        
        return ProcessingStatusResponse.from(request);
    }
    
    /**
     * Processing status response
     */
    public record ProcessingStatusResponse(
        String videoId,
        String userId,
        ProcessingStatus status,
        String originalFilename,
        double fileSizeMB,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String errorMessage,
        boolean isDownloadReady,
        boolean canRetry
    ) {
        public static ProcessingStatusResponse from(VideoProcessingRequest request) {
            return new ProcessingStatusResponse(
                request.getVideoId(),
                request.getUserId(),
                request.getStatus(),
                request.getMetadata().originalFilename(),
                request.getMetadata().getFileSizeMB(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.getErrorMessage(),
                request.isDownloadReady(),
                request.canRetry()
            );
        }
    }
}
