package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoMetadata;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GetProcessingStatusUseCaseTest {

    @Mock
    private VideoRepositoryPort videoRepository;

    private GetProcessingStatusUseCase useCase;

    private final String VIDEO_ID = "video-123";
    private final String USER_ID = "user-456";
    private final String OTHER_USER_ID = "user-789";
    private final LocalDateTime CREATED_AT = LocalDateTime.now().minusHours(1);
    private final LocalDateTime UPDATED_AT = LocalDateTime.now();

    @BeforeEach
    void setUp() {
        useCase = new GetProcessingStatusUseCase(videoRepository);
    }

    @Test
    void shouldReturnProcessingStatusWhenVideoExists() {
        // Arrange
        VideoProcessingRequest mockRequest = createFullMockVideoRequest(USER_ID);
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.of(mockRequest));

        // Act
        GetProcessingStatusUseCase.ProcessingStatusResponse response = useCase.execute(VIDEO_ID, USER_ID);

        // Assert
        assertNotNull(response);
        assertEquals(VIDEO_ID, response.videoId());
        assertEquals(USER_ID, response.userId());
        assertEquals(ProcessingStatus.PROCESSING, response.status());
        assertEquals("video.mp4", response.originalFilename());
        assertEquals(10.5, response.fileSizeMB());
        assertEquals(CREATED_AT, response.createdAt());
        assertEquals(UPDATED_AT, response.updatedAt());
        assertNull(response.errorMessage());
        assertFalse(response.isDownloadReady());
        assertTrue(response.canRetry());

        verify(videoRepository).findById(VIDEO_ID);
    }

//    @Test
//    void shouldThrowExceptionWhenVideoDoesNotBelongToUser() {
//        // Arrange
//        VideoProcessingRequest mockRequest = mock(VideoProcessingRequest.class);
//        when(mockRequest.getUserId()).thenReturn(OTHER_USER_ID);
//        when(mockRequest.getVideoId()).thenReturn(VIDEO_ID);
//        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.of(mockRequest));
//
//        // Act & Assert
//        VideoNotFoundException exception = assertThrows(
//                VideoNotFoundException.class,
//                () -> useCase.execute(VIDEO_ID, USER_ID)
//        );
//
//        assertEquals("Video not found with ID: " + VIDEO_ID, exception.getMessage());
//    }

    @Test
    void shouldThrowExceptionWhenVideoDoesNotExist() {
        // Arrange
        when(videoRepository.findById(VIDEO_ID)).thenReturn(Optional.empty());

        // Act & Assert
        VideoNotFoundException exception = assertThrows(
                VideoNotFoundException.class,
                () -> useCase.execute(VIDEO_ID, USER_ID)
        );

        assertEquals("Video not found with ID: " + VIDEO_ID, exception.getMessage());
        verify(videoRepository).findById(VIDEO_ID);
    }

    private VideoProcessingRequest createFullMockVideoRequest(String userId) {
        VideoProcessingRequest request = mock(VideoProcessingRequest.class);
        VideoMetadata metadata = mock(VideoMetadata.class);

        when(request.getVideoId()).thenReturn(VIDEO_ID);
        when(request.getUserId()).thenReturn(userId);
        when(request.getStatus()).thenReturn(ProcessingStatus.PROCESSING);
        when(request.getMetadata()).thenReturn(metadata);
        when(metadata.originalFilename()).thenReturn("video.mp4");
        when(metadata.getFileSizeMB()).thenReturn(10.5);
        when(request.getCreatedAt()).thenReturn(CREATED_AT);
        when(request.getUpdatedAt()).thenReturn(UPDATED_AT);
        when(request.getErrorMessage()).thenReturn(null);
        when(request.isDownloadReady()).thenReturn(false);
        when(request.canRetry()).thenReturn(true);

        return request;
    }
}
