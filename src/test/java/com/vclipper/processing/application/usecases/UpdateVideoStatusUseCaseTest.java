package com.vclipper.processing.application.usecases;

import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.ports.NotificationPort;
import com.vclipper.processing.application.ports.VideoRepositoryPort;
import com.vclipper.processing.domain.entity.NotificationType;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.entity.VideoMetadata;
import com.vclipper.processing.domain.entity.VideoProcessingRequest;
import com.vclipper.processing.domain.enums.VideoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateVideoStatusUseCaseTest {

    @Mock
    private VideoRepositoryPort videoRepositoryPort;

    @Mock
    private NotificationPort notificationPort;

    private UpdateVideoStatusUseCase useCase;

    @Captor
    private ArgumentCaptor<VideoProcessingRequest> requestCaptor;

    @Captor
    private ArgumentCaptor<String> userIdCaptor;

    @Captor
    private ArgumentCaptor<NotificationType> notificationTypeCaptor;

    @Captor
    private ArgumentCaptor<NotificationPort.NotificationData> notificationDataCaptor;

    private static final String VIDEO_ID = "vid-123";
    private static final String USER_ID = "user-456";
    private static final String ORIGINAL_FILENAME = "test-video.mp4";
    private static final String S3_KEY = "processed/test-video-processed.mp4";
    private static final String ERROR_MESSAGE = "Processing failed due to unsupported format";

    private VideoProcessingRequest createPendingVideoRequest() {
        VideoMetadata metadata = new VideoMetadata(
                ORIGINAL_FILENAME,
                1024L * 1024L, // 1MB file size
                VideoFormat.MP4,
                "video/mp4",
                "uploads/test-video.mp4"
        );

        return new VideoProcessingRequest(
                VIDEO_ID,
                USER_ID,
                metadata,
                ProcessingStatus.PENDING,
                LocalDateTime.now().minusMinutes(5),
                LocalDateTime.now().minusMinutes(5),
                null,
                null
        );
    }

    private VideoProcessingRequest createProcessingVideoRequest() {
        VideoProcessingRequest request = createPendingVideoRequest();
        request.startProcessing();
        return request;
    }

    @BeforeEach
    void setUp() {
        useCase = new UpdateVideoStatusUseCase(videoRepositoryPort, notificationPort);
    }

    @Nested
    @DisplayName("Status Transition Tests")
    class StatusTransitionTests {

        @Test
        @DisplayName("Should transition from PENDING to PROCESSING successfully")
        void shouldTransitionFromPendingToProcessing() {
            // Arrange
            VideoProcessingRequest pendingRequest = createPendingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(pendingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING);

            // Assert
            assertTrue(result.isSuccess());
            assertEquals(ProcessingStatus.PENDING, result.getValue().get().previousStatus());
            assertEquals(ProcessingStatus.PROCESSING, result.getValue().get().newStatus());

            verify(videoRepositoryPort).save(requestCaptor.capture());
            assertEquals(ProcessingStatus.PROCESSING, requestCaptor.getValue().getStatus());

            verify(notificationPort).sendNotification(
                eq(USER_ID),
                eq(NotificationType.PROCESSING_STARTED),
                any(NotificationPort.NotificationData.class)
            );
        }

        @Test
        @DisplayName("Should transition from PROCESSING to COMPLETED successfully")
        void shouldTransitionFromProcessingToCompleted() {
            // Arrange
            VideoProcessingRequest processingRequest = createProcessingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(processingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.COMPLETED, S3_KEY, null);

            // Assert
            assertTrue(result.isSuccess());
            assertEquals(ProcessingStatus.PROCESSING, result.getValue().get().previousStatus());
            assertEquals(ProcessingStatus.COMPLETED, result.getValue().get().newStatus());
            assertEquals(S3_KEY, result.getValue().get().processedFileS3Key());

            verify(videoRepositoryPort).save(requestCaptor.capture());
            assertEquals(ProcessingStatus.COMPLETED, requestCaptor.getValue().getStatus());
            assertEquals(S3_KEY, requestCaptor.getValue().getProcessedFileReference());

            verify(notificationPort).sendNotification(
                eq(USER_ID),
                eq(NotificationType.PROCESSING_COMPLETED),
                any(NotificationPort.NotificationData.class)
            );
        }

        @Test
        @DisplayName("Should transition from PROCESSING to FAILED successfully")
        void shouldTransitionFromProcessingToFailed() {
            // Arrange
            VideoProcessingRequest processingRequest = createProcessingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(processingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.FAILED, null, ERROR_MESSAGE);

            // Assert
            assertTrue(result.isSuccess());
            assertEquals(ProcessingStatus.PROCESSING, result.getValue().get().previousStatus());
            assertEquals(ProcessingStatus.FAILED, result.getValue().get().newStatus());

            verify(videoRepositoryPort).save(requestCaptor.capture());
            assertEquals(ProcessingStatus.FAILED, requestCaptor.getValue().getStatus());

            verify(notificationPort).sendNotification(
                userIdCaptor.capture(),
                notificationTypeCaptor.capture(),
                notificationDataCaptor.capture()
            );

            assertEquals(USER_ID, userIdCaptor.getValue());
            assertEquals(NotificationType.PROCESSING_FAILED, notificationTypeCaptor.getValue());
            assertEquals(ORIGINAL_FILENAME, notificationDataCaptor.getValue().videoName());
            assertEquals(ERROR_MESSAGE, notificationDataCaptor.getValue().errorMessage());
        }

        @Test
        @DisplayName("Should transition from PENDING to FAILED successfully")
        void shouldTransitionFromPendingToFailed() {
            // Arrange
            VideoProcessingRequest pendingRequest = createPendingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(pendingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.FAILED, null, ERROR_MESSAGE);

            // Assert
            assertTrue(result.isSuccess());
            assertEquals(ProcessingStatus.PENDING, result.getValue().get().previousStatus());
            assertEquals(ProcessingStatus.FAILED, result.getValue().get().newStatus());

            verify(videoRepositoryPort).save(requestCaptor.capture());
            assertEquals(ProcessingStatus.FAILED, requestCaptor.getValue().getStatus());

            verify(notificationPort).sendNotification(
                eq(USER_ID),
                eq(NotificationType.PROCESSING_FAILED),
                any(NotificationPort.NotificationData.class)
            );
        }
    }

    @Nested
    @DisplayName("Invalid Transition Tests")
    class InvalidTransitionTests {

        @Test
        @DisplayName("Should reject transition from COMPLETED to any status")
        void shouldRejectTransitionFromCompleted() {
            // Arrange
            VideoProcessingRequest completedRequest = createPendingVideoRequest();
            completedRequest.markAsCompleted(S3_KEY);

            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(completedRequest));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertTrue(result.getError().get().message().contains("Cannot transition from"));

            verify(videoRepositoryPort, never()).save(any());
            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }

        @Test
        @DisplayName("Should reject transition from FAILED to any status")
        void shouldRejectTransitionFromFailed() {
            // Arrange
            VideoProcessingRequest failedRequest = createPendingVideoRequest();
            failedRequest.markAsFailed(ERROR_MESSAGE);

            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(failedRequest));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertTrue(result.getError().get().message().contains("Cannot transition from"));

            verify(videoRepositoryPort, never()).save(any());
            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }

        @Test
        @DisplayName("Should reject invalid transition from PENDING to COMPLETED")
        void shouldRejectTransitionFromPendingToCompleted() {
            // Arrange
            VideoProcessingRequest pendingRequest = createPendingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(pendingRequest));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.COMPLETED, S3_KEY, null);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertTrue(result.getError().get().message().contains("Cannot transition from"));

            verify(videoRepositoryPort, never()).save(any());
            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {

        @Test
        @DisplayName("Should return error when video not found")
        void shouldReturnErrorWhenVideoNotFound() {
            // Arrange
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.empty());

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertEquals("Video not found: " + VIDEO_ID, result.getError().get().message());

            verify(videoRepositoryPort, never()).save(any());
            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }

        @Test
        @DisplayName("Should return error when user doesn't own the video")
        void shouldReturnErrorWhenUserDoesntOwnVideo() {
            // Arrange
            VideoProcessingRequest pendingRequest = createPendingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(pendingRequest));

            String differentUserId = "different-user-789";

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, differentUserId, ProcessingStatus.PROCESSING);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertTrue(result.getError().get().message().contains("User does not own this video"));

            verify(videoRepositoryPort, never()).save(any());
            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }

        @Test
        @DisplayName("Should return error when S3 key is missing for COMPLETED status")
        void shouldReturnErrorWhenS3KeyMissingForCompletedStatus() {
            // Arrange
            VideoProcessingRequest processingRequest = createProcessingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(processingRequest));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.COMPLETED, null, null);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertTrue(result.getError().get().message().contains("Processed file S3 key is required"));

            verify(videoRepositoryPort, never()).save(any());
            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }

        @Test
        @DisplayName("Should return error when error message is missing for FAILED status")
        void shouldReturnErrorWhenErrorMessageMissingForFailedStatus() {
            // Arrange
            VideoProcessingRequest processingRequest = createProcessingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(processingRequest));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.FAILED, null, null);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertTrue(result.getError().get().message().contains("Error message is required"));

            verify(videoRepositoryPort, never()).save(any());
            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }

        @Test
        @DisplayName("Should handle repository exceptions")
        void shouldHandleRepositoryExceptions() {
            // Arrange
            VideoProcessingRequest pendingRequest = createPendingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(pendingRequest));
            when(videoRepositoryPort.save(any())).thenThrow(new RuntimeException("Database connection error"));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING);

            // Assert
            assertFalse(result.isSuccess());
            assertTrue(result.getError().isPresent());
            assertTrue(result.getError().get().message().contains("Failed to update video status"));

            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Controller Interface Tests")
    class ControllerInterfaceTests {

        @Test
        @DisplayName("Should handle controller interface call for status update")
        void shouldHandleControllerInterfaceCall() {
            // Arrange
            VideoProcessingRequest processingRequest = createProcessingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(processingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            UpdateVideoStatusUseCase.VideoStatusUpdateResponse response =
                useCase.execute(VIDEO_ID, ProcessingStatus.COMPLETED, S3_KEY, null);

            // Assert
            assertNotNull(response);
            assertEquals(ProcessingStatus.PROCESSING, response.previousStatus());
            assertEquals(ProcessingStatus.COMPLETED, response.newStatus());
            assertEquals(S3_KEY, response.processedFileS3Key());

            verify(videoRepositoryPort).save(requestCaptor.capture());
            assertEquals(ProcessingStatus.COMPLETED, requestCaptor.getValue().getStatus());
            assertEquals(S3_KEY, requestCaptor.getValue().getProcessedFileReference());

            verify(notificationPort).sendNotification(
                eq(USER_ID),
                eq(NotificationType.PROCESSING_COMPLETED),
                any(NotificationPort.NotificationData.class)
            );
        }

        @Test
        @DisplayName("Should throw RuntimeException when controller interface fails")
        void shouldThrowRuntimeExceptionWhenControllerInterfaceFails() {
            // Arrange
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.empty());

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                useCase.execute(VIDEO_ID, ProcessingStatus.COMPLETED, S3_KEY, null)
            );

            assertTrue(exception.getMessage().contains("Video not found"));

            verify(videoRepositoryPort, never()).save(any());
            verify(notificationPort, never()).sendNotification(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Notification Tests")
    class NotificationTests {

        @Test
        @DisplayName("Should send correct notification for PROCESSING status")
        void shouldSendCorrectNotificationForProcessingStatus() {
            // Arrange
            VideoProcessingRequest pendingRequest = createPendingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(pendingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING);

            // Assert
            assertTrue(result.isSuccess());

            verify(notificationPort).sendNotification(
                userIdCaptor.capture(),
                notificationTypeCaptor.capture(),
                notificationDataCaptor.capture()
            );

            assertEquals(USER_ID, userIdCaptor.getValue());
            assertEquals(NotificationType.PROCESSING_STARTED, notificationTypeCaptor.getValue());
            assertEquals(ORIGINAL_FILENAME, notificationDataCaptor.getValue().videoName());
            assertNull(notificationDataCaptor.getValue().downloadUrl());
            assertNull(notificationDataCaptor.getValue().errorMessage());
        }

        @Test
        @DisplayName("Should send correct notification for COMPLETED status")
        void shouldSendCorrectNotificationForCompletedStatus() {
            // Arrange
            VideoProcessingRequest processingRequest = createProcessingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(processingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.COMPLETED, S3_KEY, null);

            // Assert
            assertTrue(result.isSuccess());

            verify(notificationPort).sendNotification(
                userIdCaptor.capture(),
                notificationTypeCaptor.capture(),
                notificationDataCaptor.capture()
            );

            assertEquals(USER_ID, userIdCaptor.getValue());
            assertEquals(NotificationType.PROCESSING_COMPLETED, notificationTypeCaptor.getValue());
            assertEquals(ORIGINAL_FILENAME, notificationDataCaptor.getValue().videoName());
            assertNull(notificationDataCaptor.getValue().downloadUrl()); // Note: download URL is not set in the code
            assertNull(notificationDataCaptor.getValue().errorMessage());
        }

        @Test
        @DisplayName("Should not fail if notification service throws exception")
        void shouldNotFailIfNotificationServiceThrowsException() {
            // Arrange
            VideoProcessingRequest pendingRequest = createPendingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(pendingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
            doThrow(new RuntimeException("Notification service unavailable")).when(notificationPort)
                .sendNotification(any(), any(), any());

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING);

            // Assert
            assertTrue(result.isSuccess()); // Should still succeed even if notification fails
            assertEquals(ProcessingStatus.PENDING, result.getValue().get().previousStatus());
            assertEquals(ProcessingStatus.PROCESSING, result.getValue().get().newStatus());

            verify(videoRepositoryPort).save(any()); // Should still save the request
        }

        @Test
        @DisplayName("Should handle null notification service gracefully")
        void shouldHandleNullNotificationServiceGracefully() {
            // Arrange
            VideoProcessingRequest pendingRequest = createPendingVideoRequest();
            when(videoRepositoryPort.findById(VIDEO_ID)).thenReturn(Optional.of(pendingRequest));
            when(videoRepositoryPort.save(any(VideoProcessingRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            useCase = new UpdateVideoStatusUseCase(videoRepositoryPort, null); // Set notification service to null

            // Act
            Result<UpdateVideoStatusUseCase.VideoStatusUpdateResponse, ProcessingError> result =
                useCase.execute(VIDEO_ID, USER_ID, ProcessingStatus.PROCESSING);

            // Assert
            assertTrue(result.isSuccess()); // Should succeed without notification
            assertEquals(ProcessingStatus.PENDING, result.getValue().get().previousStatus());
            assertEquals(ProcessingStatus.PROCESSING, result.getValue().get().newStatus());
        }
    }
}
