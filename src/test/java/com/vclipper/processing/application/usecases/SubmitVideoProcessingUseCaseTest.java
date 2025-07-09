package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.common.VideoUploadError;
import com.vclipper.processing.application.ports.*;
import com.vclipper.processing.domain.entity.*;
import com.vclipper.processing.domain.enums.VideoFormat;
import com.vclipper.processing.domain.exceptions.VideoUploadException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SubmitVideoProcessingUseCaseTest {

    @Mock
    private VideoRepositoryPort videoRepository;

    @Mock
    private FileStoragePort fileStorage;

    @Mock
    private MessageQueuePort messageQueue;

    @Mock
    private NotificationPort notification;

    @Mock
    private UserServicePort userService;

    @Mock
    private MimeTypeDetectionPort mimeTypeDetection;

    @Mock
    private UpdateVideoStatusUseCase updateVideoStatusUseCase;

    private SubmitVideoProcessingUseCase useCase;
    private final long MAX_FILE_SIZE = 1024 * 1024 * 100; // 100MB

    @BeforeEach
    void setUp() {
        useCase = new SubmitVideoProcessingUseCase(
            videoRepository,
            fileStorage,
            messageQueue,
            notification,
            userService,
            mimeTypeDetection,
            updateVideoStatusUseCase,
            MAX_FILE_SIZE
        );
    }

    @Test
    @DisplayName("Should successfully process a valid video submission")
    void shouldSuccessfullyProcessValidVideoSubmission() {
        // Arrange
        String userId = "test-user-id";
        String videoId = "test-video-id";
        String filename = "test-video.mp4";
        String contentType = "video/mp4";
        long fileSize = 1024 * 1024; // 1MB
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        // Setup mock for user validation
        when(userService.isActiveUser(userId)).thenReturn(true);

        // Setup mock for mime type detection
        when(mimeTypeDetection.detectMimeType(any(), eq(filename), eq(contentType)))
            .thenReturn("video/mp4");
        when(mimeTypeDetection.isSupportedVideoMimeType("video/mp4")).thenReturn(true);

        // Setup mock for file storage
        String storageRef = "storage-ref-123";
        when(fileStorage.store(any(), eq(filename), eq(contentType), eq(fileSize)))
            .thenReturn(storageRef);

        // Setup mock for video repository
        VideoProcessingRequest savedRequest = mock(VideoProcessingRequest.class);
        when(savedRequest.getVideoId()).thenReturn(videoId);
        when(savedRequest.getUserId()).thenReturn(userId);
        when(savedRequest.getStatus()).thenReturn(ProcessingStatus.PROCESSING);
        when(savedRequest.getMetadata()).thenReturn(
            new VideoMetadata(filename, fileSize, VideoFormat.MP4, contentType, storageRef)
        );

        when(videoRepository.save(any())).thenReturn(savedRequest);

        // Setup mock for updating video status
        when(updateVideoStatusUseCase.execute(eq(videoId), eq(userId), eq(ProcessingStatus.PROCESSING)))
            .thenReturn(Result.success(new UpdateVideoStatusUseCase.VideoStatusUpdateResponse(
                videoId, userId, ProcessingStatus.PENDING, ProcessingStatus.PROCESSING, null, null
            )));

        // Setup message queue mock
        when(messageQueue.sendProcessingMessage(any())).thenReturn("message-id-123");

        // Create submission request
        SubmitVideoProcessingUseCase.VideoProcessingSubmission submission =
            new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                userId, inputStream, filename, contentType, fileSize
            );

        // Act
        Result<SubmitVideoProcessingUseCase.VideoProcessingResponse, VideoUploadError> result =
            useCase.execute(submission);

        // Assert
        assertTrue(result.isSuccess());
        assertNotNull(result.getValue().orElse(null));

        SubmitVideoProcessingUseCase.VideoProcessingResponse response = result.getValue().get();
        assertEquals(videoId, response.videoId());
        assertEquals(ProcessingStatus.PROCESSING, response.status());
        assertTrue(response.success());

        // Verify interactions with mocks
        verify(userService).isActiveUser(userId);
        verify(mimeTypeDetection).detectMimeType(any(), eq(filename), eq(contentType));
        verify(mimeTypeDetection).isSupportedVideoMimeType("video/mp4");
        verify(fileStorage).store(any(), eq(filename), eq(contentType), eq(fileSize));
        verify(videoRepository).save(any());

        // Verify message queue interaction
        ArgumentCaptor<MessageQueuePort.VideoProcessingMessage> messageCaptor =
            ArgumentCaptor.forClass(MessageQueuePort.VideoProcessingMessage.class);
        verify(messageQueue).sendProcessingMessage(messageCaptor.capture());
        MessageQueuePort.VideoProcessingMessage capturedMessage = messageCaptor.getValue();
        assertEquals(videoId, capturedMessage.videoId());
        assertEquals(userId, capturedMessage.userId());
        assertEquals(storageRef, capturedMessage.storageLocation());

        // Verify notification sent
        ArgumentCaptor<NotificationType> notificationTypeCaptor =
            ArgumentCaptor.forClass(NotificationType.class);
        verify(notification).sendNotification(
            eq(userId),
            notificationTypeCaptor.capture(),
            any(NotificationPort.NotificationData.class)
        );
        assertEquals(NotificationType.UPLOAD_CONFIRMED, notificationTypeCaptor.getValue());

        // Verify status update
        verify(updateVideoStatusUseCase).execute(eq(videoId), eq(userId), eq(ProcessingStatus.PROCESSING));
    }

    @Test
    @DisplayName("Should reject video submission when file size exceeds maximum")
    void shouldRejectVideoSubmissionWhenFileSizeExceedsMaximum() {
        // Arrange
        String userId = "test-user-id";
        String filename = "large-video.mp4";
        String contentType = "video/mp4";
        long fileSize = MAX_FILE_SIZE + 1; // Exceeds max size
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        // Create submission request
        SubmitVideoProcessingUseCase.VideoProcessingSubmission submission =
            new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                userId, inputStream, filename, contentType, fileSize
            );

        // Act
        Result<SubmitVideoProcessingUseCase.VideoProcessingResponse, VideoUploadError> result =
            useCase.execute(submission);

        // Assert
        assertTrue(result.isFailure());
        assertNotNull(result.getError().orElse(null));

        VideoUploadError error = result.getError().get();
        assertEquals("FILE_TOO_LARGE", error.errorCode());
        assertEquals("file", error.field());

        // Verify no interactions with other services
        verifyNoInteractions(fileStorage, videoRepository, messageQueue, notification);
        // Note: we're not verifying userService since the order of validations may change
    }

    @Test
    @DisplayName("Should reject video submission when file format is not supported")
    void shouldRejectVideoSubmissionWhenFileFormatIsNotSupported() {
        // Arrange
        String userId = "test-user-id";
        String filename = "document.txt";
        String contentType = "text/plain";
        long fileSize = 1024; // 1KB
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        // Create submission request
        SubmitVideoProcessingUseCase.VideoProcessingSubmission submission =
            new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                userId, inputStream, filename, contentType, fileSize
            );

        // Act
        Result<SubmitVideoProcessingUseCase.VideoProcessingResponse, VideoUploadError> result =
            useCase.execute(submission);

        // Assert
        assertTrue(result.isFailure());
        assertNotNull(result.getError().orElse(null));

        VideoUploadError error = result.getError().get();
        assertEquals("INVALID_VIDEO_FORMAT", error.errorCode());
        assertEquals("file", error.field());

        // Verify no interactions with storage and other services
        verifyNoInteractions(fileStorage, videoRepository, messageQueue, notification);
    }

    @Test
    @DisplayName("Should reject video submission when MIME type is not supported")
    void shouldRejectVideoSubmissionWhenMimeTypeIsNotSupported() {
        // Arrange
        String userId = "test-user-id";
        String filename = "fake-video.mp4"; // Has MP4 extension but isn't a video
        String contentType = "video/mp4";
        long fileSize = 1024 * 1024; // 1MB
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        // Setup mock for mime type detection
        when(mimeTypeDetection.detectMimeType(any(), eq(filename), eq(contentType)))
            .thenReturn("application/octet-stream"); // Detected different MIME type
        when(mimeTypeDetection.isSupportedVideoMimeType("application/octet-stream")).thenReturn(false);

        // Create submission request
        SubmitVideoProcessingUseCase.VideoProcessingSubmission submission =
            new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                userId, inputStream, filename, contentType, fileSize
            );

        // Act
        Result<SubmitVideoProcessingUseCase.VideoProcessingResponse, VideoUploadError> result =
            useCase.execute(submission);

        // Assert
        assertTrue(result.isFailure());
        assertNotNull(result.getError().orElse(null));

        VideoUploadError error = result.getError().get();
        assertEquals("INVALID_VIDEO_FORMAT", error.errorCode());
        assertEquals("file", error.field());

        // Verify MIME type detection was called
        verify(mimeTypeDetection).detectMimeType(any(), eq(filename), eq(contentType));
        verify(mimeTypeDetection).isSupportedVideoMimeType("application/octet-stream");

        // Verify no interactions with storage and other services
        verifyNoInteractions(fileStorage, videoRepository, messageQueue, notification);
    }

    @Test
    @DisplayName("Should reject video submission when user is inactive")
    void shouldRejectVideoSubmissionWhenUserIsInactive() {
        // Arrange
        String userId = "inactive-user-id";
        String filename = "test-video.mp4";
        String contentType = "video/mp4";
        long fileSize = 1024 * 1024; // 1MB
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        // Setup mock for user validation
        when(userService.isActiveUser(userId)).thenReturn(false);

        // Setup mock for mime type detection
        when(mimeTypeDetection.detectMimeType(any(), eq(filename), eq(contentType)))
            .thenReturn("video/mp4");
        when(mimeTypeDetection.isSupportedVideoMimeType("video/mp4")).thenReturn(true);

        // Create submission request
        SubmitVideoProcessingUseCase.VideoProcessingSubmission submission =
            new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                userId, inputStream, filename, contentType, fileSize
            );

        // Act
        Result<SubmitVideoProcessingUseCase.VideoProcessingResponse, VideoUploadError> result =
            useCase.execute(submission);

        // Assert
        assertTrue(result.isFailure());
        assertNotNull(result.getError().orElse(null));

        VideoUploadError error = result.getError().get();
        assertEquals("INVALID_USER", error.errorCode());
        assertEquals("userId", error.field());

        // Verify user service was called
        verify(userService).isActiveUser(userId);

        // Verify no interactions with storage and other services
        verifyNoInteractions(fileStorage, videoRepository, messageQueue, notification);
    }

    @Test
    @DisplayName("Should throw exception when unexpected error occurs")
    void shouldThrowExceptionWhenUnexpectedErrorOccurs() {
        // Arrange
        String userId = "test-user-id";
        String filename = "test-video.mp4";
        String contentType = "video/mp4";
        long fileSize = 1024 * 1024; // 1MB
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        // Setup mock for user validation
        when(userService.isActiveUser(userId)).thenReturn(true);

        // Setup mock for mime type detection
        when(mimeTypeDetection.detectMimeType(any(), eq(filename), eq(contentType)))
            .thenReturn("video/mp4");
        when(mimeTypeDetection.isSupportedVideoMimeType("video/mp4")).thenReturn(true);

        // Setup mock for file storage to throw exception
        when(fileStorage.store(any(), eq(filename), eq(contentType), eq(fileSize)))
            .thenThrow(new RuntimeException("Storage service unavailable"));

        // Create submission request
        SubmitVideoProcessingUseCase.VideoProcessingSubmission submission =
            new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                userId, inputStream, filename, contentType, fileSize
            );

        // Act & Assert
        assertThrows(VideoUploadException.class, () -> useCase.execute(submission));

        // Verify interactions
        verify(userService).isActiveUser(userId);
        verify(mimeTypeDetection).detectMimeType(any(), eq(filename), eq(contentType));
        verify(mimeTypeDetection).isSupportedVideoMimeType("video/mp4");
        verify(fileStorage).store(any(), eq(filename), eq(contentType), eq(fileSize));

        // Verify no interactions with other services
        verifyNoInteractions(videoRepository, messageQueue, notification);
    }

    @Test
    @DisplayName("Should reject empty video file")
    void shouldRejectEmptyVideoFile() {
        // Arrange
        String userId = "test-user-id";
        String filename = "empty-video.mp4";
        String contentType = "video/mp4";
        long fileSize = 0; // Empty file
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // Create submission request
        SubmitVideoProcessingUseCase.VideoProcessingSubmission submission =
            new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                userId, inputStream, filename, contentType, fileSize
            );

        // Act
        Result<SubmitVideoProcessingUseCase.VideoProcessingResponse, VideoUploadError> result =
            useCase.execute(submission);

        // Assert
        assertTrue(result.isFailure());
        assertNotNull(result.getError().orElse(null));

        VideoUploadError error = result.getError().get();
        assertEquals("EMPTY_FILE", error.errorCode());
        assertEquals("file", error.field());

        // Verify no interactions with services
        verifyNoInteractions(fileStorage, videoRepository, messageQueue, notification);
    }

    @Test
    @DisplayName("Should handle status update failure gracefully")
    void shouldHandleStatusUpdateFailureGracefully() {
        // Arrange
        String userId = "test-user-id";
        String videoId = "test-video-id";
        String filename = "test-video.mp4";
        String contentType = "video/mp4";
        long fileSize = 1024 * 1024; // 1MB
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        // Setup mock for user validation
        when(userService.isActiveUser(userId)).thenReturn(true);

        // Setup mock for mime type detection
        when(mimeTypeDetection.detectMimeType(any(), eq(filename), eq(contentType)))
            .thenReturn("video/mp4");
        when(mimeTypeDetection.isSupportedVideoMimeType("video/mp4")).thenReturn(true);

        // Setup mock for file storage
        String storageRef = "storage-ref-123";
        when(fileStorage.store(any(), eq(filename), eq(contentType), eq(fileSize)))
            .thenReturn(storageRef);

        // Setup mock for video repository
        VideoProcessingRequest savedRequest = mock(VideoProcessingRequest.class);
        when(savedRequest.getVideoId()).thenReturn(videoId);
        when(savedRequest.getUserId()).thenReturn(userId);
        when(savedRequest.getStatus()).thenReturn(ProcessingStatus.PENDING);
        when(savedRequest.getMetadata()).thenReturn(
            new VideoMetadata(filename, fileSize, VideoFormat.MP4, contentType, storageRef)
        );

        when(videoRepository.save(any())).thenReturn(savedRequest);

        // Setup mock for updating video status to fail
        when(updateVideoStatusUseCase.execute(eq(videoId), eq(userId), eq(ProcessingStatus.PROCESSING)))
            .thenReturn(Result.failure(new ProcessingError("error", "Failed to update status", null)));

        // Setup message queue mock
        when(messageQueue.sendProcessingMessage(any())).thenReturn("message-id-123");

        // Create submission request
        SubmitVideoProcessingUseCase.VideoProcessingSubmission submission =
            new SubmitVideoProcessingUseCase.VideoProcessingSubmission(
                userId, inputStream, filename, contentType, fileSize
            );

        // Act
        Result<SubmitVideoProcessingUseCase.VideoProcessingResponse, VideoUploadError> result =
            useCase.execute(submission);

        // Assert - the use case should still succeed even if status update fails
        assertTrue(result.isSuccess());
        assertNotNull(result.getValue().orElse(null));

        SubmitVideoProcessingUseCase.VideoProcessingResponse response = result.getValue().get();
        assertEquals(videoId, response.videoId());
        assertEquals(ProcessingStatus.PENDING, response.status()); // Status remains PENDING as update failed
        assertTrue(response.success());

        // Verify interactions with mocks
        verify(messageQueue).sendProcessingMessage(any());
        verify(updateVideoStatusUseCase).execute(eq(videoId), eq(userId), eq(ProcessingStatus.PROCESSING));

        // Status update failed but the operation still completed successfully
        verify(notification).sendNotification(
            eq(userId),
            eq(NotificationType.UPLOAD_CONFIRMED),
            any(NotificationPort.NotificationData.class)
        );
    }
}
