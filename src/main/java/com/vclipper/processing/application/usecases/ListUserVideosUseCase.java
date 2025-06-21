package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Use case for listing user's video processing requests
 * Provides paginated list of videos with their current status
 */
public class ListUserVideosUseCase {
    
    private final VideoRepositoryPort videoRepository;
    
    public ListUserVideosUseCase(VideoRepositoryPort videoRepository) {
        this.videoRepository = videoRepository;
    }
    
    /**
     * List all videos for a user
     * 
     * @param userId User identifier
     * @return List of user's videos with status
     */
    public List<VideoSummary> execute(String userId) {
        List<VideoProcessingRequest> requests = videoRepository.findByUserId(userId);
        
        return requests.stream()
            .map(VideoSummary::from)
            .sorted((a, b) -> b.createdAt().compareTo(a.createdAt())) // Most recent first
            .toList();
    }
    
    /**
     * Video summary for listing
     */
    public record VideoSummary(
        String videoId,
        String originalFilename,
        ProcessingStatus status,
        double fileSizeMB,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean isDownloadReady,
        boolean canRetry,
        String errorMessage
    ) {
        public static VideoSummary from(VideoProcessingRequest request) {
            return new VideoSummary(
                request.getVideoId(),
                request.getMetadata().originalFilename(),
                request.getStatus(),
                request.getMetadata().getFileSizeMB(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                request.isDownloadReady(),
                request.canRetry(),
                request.getErrorMessage()
            );
        }
    }
}
