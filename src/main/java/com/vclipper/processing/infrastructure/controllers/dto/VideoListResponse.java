package com.vclipper.processing.infrastructure.controllers.dto;

import com.vclipper.processing.application.usecases.ListUserVideosUseCase;
import com.vclipper.processing.domain.entity.ProcessingStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for video list response
 * Returns list of user's videos with status
 */
public record VideoListResponse(
    String userId,
    List<VideoSummary> videos,
    int totalCount
) {
    /**
     * Create response from use case result
     */
    public static VideoListResponse from(String userId, List<ListUserVideosUseCase.VideoSummary> videoSummaries) {
        List<VideoSummary> videos = videoSummaries.stream()
            .map(VideoSummary::from)
            .toList();
        
        return new VideoListResponse(userId, videos, videos.size());
    }
    
    /**
     * Video summary for API response
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
        public static VideoSummary from(ListUserVideosUseCase.VideoSummary useCaseSummary) {
            return new VideoSummary(
                useCaseSummary.videoId(),
                useCaseSummary.originalFilename(),
                useCaseSummary.status(),
                useCaseSummary.fileSizeMB(),
                useCaseSummary.createdAt(),
                useCaseSummary.updatedAt(),
                useCaseSummary.isDownloadReady(),
                useCaseSummary.canRetry(),
                useCaseSummary.errorMessage()
            );
        }
    }
}
