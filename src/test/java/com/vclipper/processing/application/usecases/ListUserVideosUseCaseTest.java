package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoMetadata;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.enums.VideoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListUserVideosUseCaseTest {

    @Mock
    private VideoRepositoryPort videoRepository;

    private ListUserVideosUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListUserVideosUseCase(videoRepository);
    }

    @Test
    void shouldReturnEmptyListWhenUserHasNoVideos() {
        // Arrange
        String userId = "user123";
        when(videoRepository.findByUserId(userId)).thenReturn(Collections.emptyList());

        // Act
        List<ListUserVideosUseCase.VideoSummary> result = useCase.execute(userId);

        // Assert
        assertTrue(result.isEmpty());
        verify(videoRepository).findByUserId(userId);
    }

    @Test
    void shouldReturnSortedVideoSummariesByCreatedAtDescending() {
        // Arrange
        String userId = "user123";

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusDays(1);
        LocalDateTime twoDaysAgo = now.minusDays(2);

        VideoProcessingRequest video1 = createVideoRequest("video1", yesterday, ProcessingStatus.COMPLETED);
        VideoProcessingRequest video2 = createVideoRequest("video2", now, ProcessingStatus.PROCESSING);
        VideoProcessingRequest video3 = createVideoRequest("video3", twoDaysAgo, ProcessingStatus.FAILED);

        when(videoRepository.findByUserId(userId)).thenReturn(List.of(video1, video2, video3));

        // Act
        List<ListUserVideosUseCase.VideoSummary> result = useCase.execute(userId);

        // Assert
        assertEquals(3, result.size());
        // Verify sorting (newest first)
        assertEquals("video2", result.get(0).videoId());
        assertEquals("video1", result.get(1).videoId());
        assertEquals("video3", result.get(2).videoId());

        verify(videoRepository).findByUserId(userId);
    }

    @Test
    void shouldMapVideoPropertiesToSummaryCorrectly() {
        // Arrange
        String userId = "user123";
        String videoId = "video1";
        String filename = "test_video.mp4";
        long fileSize = 15L * 1024L * 1024L; // 15 MB in bytes
        String errorMsg = "Processing error";
        LocalDateTime now = LocalDateTime.now();

        VideoMetadata metadata = new VideoMetadata(filename, fileSize, VideoFormat.MP4, "video/mp4", "1920x1080");
        VideoProcessingRequest video = new VideoProcessingRequest(
                videoId,
                userId,
                metadata,
                ProcessingStatus.FAILED,
                now,
                now,
                errorMsg,
                null // outputUrl parameter
        );

        when(videoRepository.findByUserId(userId)).thenReturn(List.of(video));

        // Act
        List<ListUserVideosUseCase.VideoSummary> result = useCase.execute(userId);

        // Assert
        assertEquals(1, result.size());
        ListUserVideosUseCase.VideoSummary summary = result.get(0);

        assertEquals(videoId, summary.videoId());
        assertEquals(filename, summary.originalFilename());
        assertEquals(ProcessingStatus.FAILED, summary.status());
        assertEquals(15.0, summary.fileSizeMB());
        assertEquals(errorMsg, summary.errorMessage());

        verify(videoRepository).findByUserId(userId);
    }

    // Helper method to create test video requests
    private VideoProcessingRequest createVideoRequest(String videoId, LocalDateTime createdAt, ProcessingStatus status) {
        VideoMetadata metadata = new VideoMetadata(
            "video_" + videoId + ".mp4",
            10L,
            VideoFormat.MP4,
            "video/mp4",
            "1920x1080"
        );

        String errorMessage = status == ProcessingStatus.FAILED ? "Error message" : null;

        return new VideoProcessingRequest(
                videoId,
                "user123",
                metadata,
                status,
                createdAt,
                createdAt,
                errorMessage,
                null // outputUrl parameter
        );
    }
}
