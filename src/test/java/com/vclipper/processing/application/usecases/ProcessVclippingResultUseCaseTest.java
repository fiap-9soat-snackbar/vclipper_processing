package com.vclipper.processing.application.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.ports.ProcessResultPort;
import com.vclipper.processing.application.usecases.ProcessVclippingResultUseCase.ResultProcessingRequest;
import com.vclipper.processing.application.usecases.ProcessVclippingResultUseCase.ResultProcessingResponse;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.infrastructure.adapters.messaging.dto.VclippingResultMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcessVclippingResultUseCaseTest {

    @Mock
    private ProcessResultPort processResultPort;

    @Mock
    private ObjectMapper objectMapper;

    private ProcessVclippingResultUseCase useCase;

    // Test data
    private static final String VIDEO_ID = "vid-123";
    private static final String USER_ID = "user-456";
    private static final String OUTPUT_LOCATION = "vclipping-frames/vid-123/output.zip";
    private static final String PROCESSED_LOCATION = "processed-videos/vid-123/test_frames.zip";
    private static final Integer FRAME_COUNT = 150;
    private static final Long DURATION_MS = 5000L;
    private static final String RAW_MESSAGE = "{\"videoId\":\"vid-123\",\"userId\":\"user-456\",\"status\":\"COMPLETED\"}";

    @BeforeEach
    void setUp() {
        useCase = new ProcessVclippingResultUseCase(processResultPort, objectMapper);
    }

    @Test
    @DisplayName("Should successfully process COMPLETED vclipping result")
    void shouldSuccessfullyProcessCompletedResult() {
        // Arrange
        VclippingResultMessage resultMessage = createCompletedResultMessage();
        ResultProcessingRequest request = new ResultProcessingRequest(resultMessage);

        // Mock copyProcessedFile to return success with the target location
        when(processResultPort.copyProcessedFile(anyString(), anyString()))
                .thenReturn(Result.success(PROCESSED_LOCATION));

        // Mock updateVideoStatus to return success
        when(processResultPort.updateVideoStatus(
                eq(VIDEO_ID), eq(USER_ID), eq(ProcessingStatus.COMPLETED),
                eq(PROCESSED_LOCATION), eq(FRAME_COUNT), eq(DURATION_MS), isNull()))
                .thenReturn(Result.success(null));

        // Act
        Result<ResultProcessingResponse, ProcessingError> result = useCase.execute(request);

        // Assert
        assertTrue(result.isSuccess());
        ResultProcessingResponse response = result.getValue().orElseThrow();
        assertEquals(VIDEO_ID, response.videoId());
        assertEquals(USER_ID, response.userId());
        assertEquals(ProcessingStatus.COMPLETED.value(), response.finalStatus());
        assertEquals(OUTPUT_LOCATION, response.outputLocation());
        assertEquals(FRAME_COUNT, response.extractedFrameCount());
        assertEquals(DURATION_MS, response.processingDurationMs());
        assertTrue(response.success());
        assertNull(response.errorMessage());

        // Verify file copy was attempted
        verify(processResultPort).copyProcessedFile(eq(OUTPUT_LOCATION), anyString());

        // Verify status update was called with copied file location
        verify(processResultPort).updateVideoStatus(
                eq(VIDEO_ID), eq(USER_ID), eq(ProcessingStatus.COMPLETED),
                eq(PROCESSED_LOCATION), eq(FRAME_COUNT), eq(DURATION_MS), isNull());
    }

    @Test
    @DisplayName("Should successfully process FAILED vclipping result")
    void shouldSuccessfullyProcessFailedResult() {
        // Arrange
        String errorMessage = "Processing failed due to invalid video format";
        VclippingResultMessage resultMessage = createFailedResultMessage(errorMessage);
        ResultProcessingRequest request = new ResultProcessingRequest(resultMessage);

        // Mock updateVideoStatus to return success
        when(processResultPort.updateVideoStatus(
                eq(VIDEO_ID), eq(USER_ID), eq(ProcessingStatus.FAILED),
                isNull(), eq(FRAME_COUNT), eq(DURATION_MS), eq(errorMessage)))
                .thenReturn(Result.success(null));

        // Act
        Result<ResultProcessingResponse, ProcessingError> result = useCase.execute(request);

        // Assert
        assertTrue(result.isSuccess());
        ResultProcessingResponse response = result.getValue().orElseThrow();
        assertEquals(VIDEO_ID, response.videoId());
        assertEquals(USER_ID, response.userId());
        assertEquals(ProcessingStatus.FAILED.value(), response.finalStatus());
        assertEquals(OUTPUT_LOCATION, response.outputLocation());
        assertEquals(FRAME_COUNT, response.extractedFrameCount());
        assertEquals(DURATION_MS, response.processingDurationMs());
        assertTrue(response.success());
        assertNull(response.errorMessage());

        // Verify file copy was NOT attempted for FAILED status
        verify(processResultPort, never()).copyProcessedFile(anyString(), anyString());

        // Verify status update was called with error message
        verify(processResultPort).updateVideoStatus(
                eq(VIDEO_ID), eq(USER_ID), eq(ProcessingStatus.FAILED),
                isNull(), eq(FRAME_COUNT), eq(DURATION_MS), eq(errorMessage));
    }

    @Test
    @DisplayName("Should handle idempotent case when video is already in target status")
    void shouldHandleIdempotentCaseWhenVideoAlreadyInTargetStatus() {
        // Arrange
        VclippingResultMessage resultMessage = createCompletedResultMessage();
        ResultProcessingRequest request = new ResultProcessingRequest(resultMessage);

        // Mock copyProcessedFile to return success
        when(processResultPort.copyProcessedFile(anyString(), anyString()))
                .thenReturn(Result.success(PROCESSED_LOCATION));

        // Mock updateVideoStatus to return failure with specific idempotent error
        String idempotentErrorMessage = "Cannot transition from COMPLETED to COMPLETED";
        ProcessingError idempotentError = ProcessingError.invalidRequest(idempotentErrorMessage);
        when(processResultPort.updateVideoStatus(
                eq(VIDEO_ID), eq(USER_ID), eq(ProcessingStatus.COMPLETED),
                eq(PROCESSED_LOCATION), eq(FRAME_COUNT), eq(DURATION_MS), isNull()))
                .thenReturn(Result.failure(idempotentError));

        // Act
        Result<ResultProcessingResponse, ProcessingError> result = useCase.execute(request);

        // Assert
        assertTrue(result.isSuccess());
        ResultProcessingResponse response = result.getValue().orElseThrow();
        assertEquals(VIDEO_ID, response.videoId());
        assertEquals(USER_ID, response.userId());
        assertEquals(ProcessingStatus.COMPLETED.value(), response.finalStatus());
        assertEquals(OUTPUT_LOCATION, response.outputLocation());
        assertEquals(FRAME_COUNT, response.extractedFrameCount());
        assertEquals(DURATION_MS, response.processingDurationMs());
        assertTrue(response.success());
        assertTrue(response.errorMessage().contains("already in target status"));
    }

    @Test
    @DisplayName("Should handle failure when copying processed file fails")
    void shouldHandleFailureWhenCopyingProcessedFileFails() {
        // Arrange
        VclippingResultMessage resultMessage = createCompletedResultMessage();
        ResultProcessingRequest request = new ResultProcessingRequest(resultMessage);

        // Mock copyProcessedFile to return failure
        ProcessingError copyError = ProcessingError.fileCopyFailed("Failed to copy file", new RuntimeException("S3 error"));
        when(processResultPort.copyProcessedFile(anyString(), anyString()))
                .thenReturn(Result.failure(copyError));

        // Act
        Result<ResultProcessingResponse, ProcessingError> result = useCase.execute(request);

        // Assert
        assertTrue(result.isFailure());
        ProcessingError error = result.getError().orElseThrow();
        assertEquals(copyError, error);

        // Verify status update was NOT called
        verify(processResultPort, never()).updateVideoStatus(
                anyString(), anyString(), any(ProcessingStatus.class),
                anyString(), anyInt(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Should handle validation failure when message is missing required fields")
    void shouldHandleValidationFailureWhenMessageIsMissingRequiredFields() {
        // Arrange
        VclippingResultMessage invalidMessage = new VclippingResultMessage(
                null, // Missing videoId
                USER_ID,
                "COMPLETED",
                OUTPUT_LOCATION,
                FRAME_COUNT,
                DURATION_MS,
                null,
                null
        );
        ResultProcessingRequest request = new ResultProcessingRequest(invalidMessage);

        // Act
        Result<ResultProcessingResponse, ProcessingError> result = useCase.execute(request);

        // Assert
        assertTrue(result.isFailure());
        ProcessingError error = result.getError().orElseThrow();
        assertEquals("INVALID_REQUEST", error.errorCode());
        assertTrue(error.message().contains("Result message is missing required fields"));

        // Verify no ports were called
        verifyNoInteractions(processResultPort);
    }

    @Test
    @DisplayName("Should handle failure when updating video status fails")
    void shouldHandleFailureWhenUpdatingVideoStatusFails() {
        // Arrange
        VclippingResultMessage resultMessage = createCompletedResultMessage();
        ResultProcessingRequest request = new ResultProcessingRequest(resultMessage);

        // Mock copyProcessedFile to return success
        when(processResultPort.copyProcessedFile(anyString(), anyString()))
                .thenReturn(Result.success(PROCESSED_LOCATION));

        // Mock updateVideoStatus to return failure
        ProcessingError updateError = ProcessingError.statusUpdateFailed("Failed to update status", new RuntimeException("DB error"));
        when(processResultPort.updateVideoStatus(
                eq(VIDEO_ID), eq(USER_ID), eq(ProcessingStatus.COMPLETED),
                eq(PROCESSED_LOCATION), eq(FRAME_COUNT), eq(DURATION_MS), isNull()))
                .thenReturn(Result.failure(updateError));

        // Act
        Result<ResultProcessingResponse, ProcessingError> result = useCase.execute(request);

        // Assert
        assertTrue(result.isSuccess()); // Success because we need SQS to mark message as processed
        ResultProcessingResponse response = result.getValue().orElseThrow();
        assertEquals(VIDEO_ID, response.videoId());
        assertEquals(USER_ID, response.userId());
        assertNull(response.finalStatus());
        assertNull(response.outputLocation());
        assertNull(response.extractedFrameCount());
        assertNull(response.processingDurationMs());
        assertFalse(response.success());
        assertEquals(updateError.message(), response.errorMessage());
    }

    @Test
    @DisplayName("Should successfully process raw SQS message")
    void shouldSuccessfullyProcessRawSqsMessage() throws Exception {
        // Arrange
        VclippingResultMessage resultMessage = createCompletedResultMessage();

        // Mock ObjectMapper to return the result message
        when(objectMapper.readValue(RAW_MESSAGE, VclippingResultMessage.class))
                .thenReturn(resultMessage);

        // Mock copyProcessedFile to return success
        when(processResultPort.copyProcessedFile(anyString(), anyString()))
                .thenReturn(Result.success(PROCESSED_LOCATION));

        // Mock updateVideoStatus to return success
        when(processResultPort.updateVideoStatus(
                eq(VIDEO_ID), eq(USER_ID), eq(ProcessingStatus.COMPLETED),
                eq(PROCESSED_LOCATION), eq(FRAME_COUNT), eq(DURATION_MS), isNull()))
                .thenReturn(Result.success(null));

        // Act
        Result<ResultProcessingResponse, ProcessingError> result = useCase.executeFromRawMessage(RAW_MESSAGE);

        // Assert
        assertTrue(result.isSuccess());
        ResultProcessingResponse response = result.getValue().orElseThrow();
        assertEquals(VIDEO_ID, response.videoId());
        assertEquals(USER_ID, response.userId());
        assertEquals(ProcessingStatus.COMPLETED.value(), response.finalStatus());
        assertTrue(response.success());
    }

    @Test
    @DisplayName("Should handle failure when parsing raw SQS message fails")
    void shouldHandleFailureWhenParsingRawSqsMessageFails() throws Exception {
        // Arrange
        // Mock ObjectMapper to throw exception
        when(objectMapper.readValue(RAW_MESSAGE, VclippingResultMessage.class))
                .thenThrow(new RuntimeException("Invalid JSON"));

        // Act
        Result<ResultProcessingResponse, ProcessingError> result = useCase.executeFromRawMessage(RAW_MESSAGE);

        // Assert
        assertTrue(result.isFailure());
        ProcessingError error = result.getError().orElseThrow();
        assertEquals("INVALID_FORMAT", error.errorCode());
        assertTrue(error.message().contains("Invalid SQS result message format"));
    }

    @Test
    @DisplayName("Should determine correct target status from result message status")
    void shouldDetermineCorrectTargetStatusFromResultMessageStatus() {
        // Test COMPLETED status
        VclippingResultMessage completedMessage = createResultMessageWithStatus("COMPLETED");
        ResultProcessingRequest completedRequest = new ResultProcessingRequest(completedMessage);

        // Mock copy and update for COMPLETED status
        when(processResultPort.copyProcessedFile(anyString(), anyString()))
                .thenReturn(Result.success(PROCESSED_LOCATION));
        when(processResultPort.updateVideoStatus(
                anyString(), anyString(), eq(ProcessingStatus.COMPLETED),
                anyString(), anyInt(), anyLong(), isNull()))
                .thenReturn(Result.success(null));

        Result<ResultProcessingResponse, ProcessingError> completedResult = useCase.execute(completedRequest);
        assertTrue(completedResult.isSuccess());
        assertEquals(ProcessingStatus.COMPLETED.value(),
                     completedResult.getValue().orElseThrow().finalStatus());

        // Test FAILED status
        VclippingResultMessage failedMessage = createResultMessageWithStatus("FAILED");
        ResultProcessingRequest failedRequest = new ResultProcessingRequest(failedMessage);

        // Mock update for FAILED status
        when(processResultPort.updateVideoStatus(
                anyString(), anyString(), eq(ProcessingStatus.FAILED),
                isNull(), anyInt(), anyLong(), isNull()))
                .thenReturn(Result.success(null));

        Result<ResultProcessingResponse, ProcessingError> failedResult = useCase.execute(failedRequest);
        assertTrue(failedResult.isSuccess());
        assertEquals(ProcessingStatus.FAILED.value(),
                     failedResult.getValue().orElseThrow().finalStatus());

        // Test ERROR status (should map to FAILED)
        VclippingResultMessage errorMessage = createResultMessageWithStatus("ERROR");
        ResultProcessingRequest errorRequest = new ResultProcessingRequest(errorMessage);

        // Mock update for ERROR status (maps to FAILED)
        when(processResultPort.updateVideoStatus(
                anyString(), anyString(), eq(ProcessingStatus.FAILED),
                isNull(), anyInt(), anyLong(), isNull()))
                .thenReturn(Result.success(null));

        Result<ResultProcessingResponse, ProcessingError> errorResult = useCase.execute(errorRequest);
        assertTrue(errorResult.isSuccess());
        assertEquals(ProcessingStatus.FAILED.value(),
                     errorResult.getValue().orElseThrow().finalStatus());

        // Test unknown status (should map to FAILED)
        VclippingResultMessage unknownMessage = createResultMessageWithStatus("UNKNOWN");
        ResultProcessingRequest unknownRequest = new ResultProcessingRequest(unknownMessage);

        // Mock update for unknown status (maps to FAILED)
        when(processResultPort.updateVideoStatus(
                anyString(), anyString(), eq(ProcessingStatus.FAILED),
                isNull(), anyInt(), anyLong(), isNull()))
                .thenReturn(Result.success(null));

        Result<ResultProcessingResponse, ProcessingError> unknownResult = useCase.execute(unknownRequest);
        assertTrue(unknownResult.isSuccess());
        assertEquals(ProcessingStatus.FAILED.value(),
                     unknownResult.getValue().orElseThrow().finalStatus());
    }

    @Test
    @DisplayName("Should use correct S3 key format when copying processed file")
    void shouldUseCorrectS3KeyFormatWhenCopyingProcessedFile() {
        // Arrange
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalFileName", "test_video.mp4");

        VclippingResultMessage resultMessage = new VclippingResultMessage(
                VIDEO_ID, USER_ID, "COMPLETED", OUTPUT_LOCATION,
                FRAME_COUNT, DURATION_MS, null, metadata
        );
        ResultProcessingRequest request = new ResultProcessingRequest(resultMessage);

        // Capture the S3 key used in copyProcessedFile
        ArgumentCaptor<String> targetS3KeyCaptor = ArgumentCaptor.forClass(String.class);
        when(processResultPort.copyProcessedFile(eq(OUTPUT_LOCATION), targetS3KeyCaptor.capture()))
                .thenReturn(Result.success(PROCESSED_LOCATION));

        when(processResultPort.updateVideoStatus(
                anyString(), anyString(), any(ProcessingStatus.class),
                anyString(), anyInt(), anyLong(), isNull()))
                .thenReturn(Result.success(null));

        // Act
        useCase.execute(request);

        // Assert
        String targetS3Key = targetS3KeyCaptor.getValue();
        assertTrue(targetS3Key.startsWith("processed-videos/"));
        assertTrue(targetS3Key.contains(VIDEO_ID));
        assertTrue(targetS3Key.contains("test_video"));
        assertTrue(targetS3Key.contains("_frames.zip"));
        assertFalse(targetS3Key.contains(".mp4")); // Extension should be removed
    }

    // Helper methods to create test data

    private VclippingResultMessage createCompletedResultMessage() {
        return new VclippingResultMessage(
                VIDEO_ID,
                USER_ID,
                "COMPLETED",
                OUTPUT_LOCATION,
                FRAME_COUNT,
                DURATION_MS,
                null,
                Map.of("originalFileName", "test.mp4")
        );
    }

    private VclippingResultMessage createFailedResultMessage(String errorMessage) {
        return new VclippingResultMessage(
                VIDEO_ID,
                USER_ID,
                "FAILED",
                OUTPUT_LOCATION,
                FRAME_COUNT,
                DURATION_MS,
                errorMessage,
                Map.of("originalFileName", "test.mp4")
        );
    }

    private VclippingResultMessage createResultMessageWithStatus(String status) {
        return new VclippingResultMessage(
                VIDEO_ID,
                USER_ID,
                status,
                OUTPUT_LOCATION,
                FRAME_COUNT,
                DURATION_MS,
                null,
                Map.of("originalFileName", "test.mp4")
        );
    }
}
