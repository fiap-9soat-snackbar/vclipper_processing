package com.vclipper.processing.application.usecases;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.ports.ProcessResultPort;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.infrastructure.adapters.messaging.dto.VclippingResultMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Use case for processing result messages from vclipping service.
 *
 * Handles incoming SQS result messages and orchestrates status updates.
 * Follows the same pattern as vclipping's ConsumeProcessingMessageUseCase.
 * Uses Result pattern for business validation and error handling.
 */
public class ProcessVclippingResultUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ProcessVclippingResultUseCase.class);

    private final ProcessResultPort processResultPort;
    private final ObjectMapper objectMapper;

    public ProcessVclippingResultUseCase(ProcessResultPort processResultPort, ObjectMapper objectMapper) {
        this.processResultPort = processResultPort;
        this.objectMapper = objectMapper;
        logger.info("🚀 ProcessVclippingResultUseCase initialized");
    }

    /**
     * Process incoming result message from vclipping service.
     *
     * @param request Result processing request
     * @return Result containing processing response or error
     */
    public Result<ResultProcessingResponse, ProcessingError> execute(ResultProcessingRequest request) {
        logger.info("📨 Processing vclipping result message");
        logger.info("   🎥 Video ID: {}", request.resultMessage().videoId());
        logger.info("   👤 User ID: {}", request.resultMessage().userId());
        logger.info("   📊 Status: {}", request.resultMessage().status());
        logger.info("   🖼️  Extracted frames: {}", request.resultMessage().extractedFrameCount());
        logger.info("   ⏱️  Processing duration: {}ms", request.resultMessage().processingDurationMs());

        // 1. Validate result message
        Result<Void, ProcessingError> validationResult = validateResultMessage(request.resultMessage());
        if (validationResult.isFailure()) {
            ProcessingError validationError = validationResult.getError().orElseThrow();
            logger.warn("   ❌ Result message validation failed: {}", validationError.message());
            return Result.failure(validationError);
        }

        // 2. Determine target processing status
        ProcessingStatus targetStatus = determineTargetStatus(request.resultMessage());
        logger.info("   🎯 Target status: {}", targetStatus.value());

        // 3. Copy vclipping output to processed-videos location (for COMPLETED status)
        String processedFileS3Key = null;
        if (targetStatus == ProcessingStatus.COMPLETED) {
            Result<String, ProcessingError> copyResult = copyVclippingOutputToProcessedLocation(request.resultMessage());
            if (copyResult.isFailure()) {
                ProcessingError copyError = copyResult.getError().orElseThrow();
                logger.error("   ❌ Failed to copy vclipping output: {}", copyError.message());
                return Result.failure(copyError);
            }
            processedFileS3Key = copyResult.getValue().orElseThrow();
            logger.info("   ✅ Vclipping output copied to: {}", processedFileS3Key);
        }

        // 4. Update video processing status
        Result<Void, ProcessingError> updateResult = updateVideoStatus(
            request.resultMessage(),
            targetStatus,
            processedFileS3Key  // Use copied file location instead of vclipping output location
        );

        if (updateResult.isSuccess()) {
            logger.info("   ✅ Video status updated successfully");

            ResultProcessingResponse response = new ResultProcessingResponse(
                request.resultMessage().videoId(),
                request.resultMessage().userId(),
                targetStatus.value(),
                request.resultMessage().outputLocation(),
                request.resultMessage().extractedFrameCount(),
                request.resultMessage().processingDurationMs(),
                true,
                null
            );

            return Result.success(response);

        } else {
            ProcessingError updateError = updateResult.getError().orElseThrow();

            // Handle idempotent case: video already in target status
            if (updateError.errorCode().equals("INVALID_REQUEST") &&
                updateError.message().contains("Cannot transition from " + targetStatus.value() + " to " + targetStatus.value())) {

                logger.info("   ✅ Video already in target status (idempotent): {}", targetStatus.value());

                ResultProcessingResponse response = new ResultProcessingResponse(
                    request.resultMessage().videoId(),
                    request.resultMessage().userId(),
                    targetStatus.value(),
                    request.resultMessage().outputLocation(),
                    request.resultMessage().extractedFrameCount(),
                    request.resultMessage().processingDurationMs(),
                    true,
                    "Video already in target status (idempotent)"
                );

                return Result.success(response);
            }

            logger.error("   ❌ Failed to update video status: {}", updateError.message());

            ResultProcessingResponse response = new ResultProcessingResponse(
                request.resultMessage().videoId(),
                request.resultMessage().userId(),
                null,
                null,
                null,
                null,
                false,
                updateError.message()
            );

            return Result.success(response); // Return success with error details for SQS handling
        }
    }

    /**
     * Execute processing from raw SQS message body (for @SqsListener integration).
     * This method handles JSON parsing and delegates to the main execute method.
     *
     * @param rawMessageBody Raw JSON message body from SQS
     * @return Result containing processing response or error
     */
    public Result<ResultProcessingResponse, ProcessingError> executeFromRawMessage(String rawMessageBody) {
        logger.info("📨 Processing raw SQS result message");
        logger.debug("📄 Raw message: {}", rawMessageBody);

        try {
            // Parse raw JSON message
            VclippingResultMessage resultMessage = parseRawMessage(rawMessageBody);

            // Create request and delegate to main execute method
            ResultProcessingRequest request = new ResultProcessingRequest(resultMessage);

            return execute(request);

        } catch (Exception e) {
            logger.error("❌ Failed to parse raw SQS result message: {}", e.getMessage(), e);
            return Result.failure(ProcessingError.invalidFormat("Invalid SQS result message format: " + e.getMessage()));
        }
    }

    /**
     * Validate result message structure and required fields.
     */
    private Result<Void, ProcessingError> validateResultMessage(VclippingResultMessage message) {
        if (message == null) {
            return Result.failure(ProcessingError.invalidRequest("Result message is null"));
        }

        if (!message.isValid()) {
            return Result.failure(ProcessingError.invalidRequest("Result message is missing required fields"));
        }

        if (message.videoId() == null || message.videoId().trim().isEmpty()) {
            return Result.failure(ProcessingError.invalidRequest("Video ID is required"));
        }

        if (message.userId() == null || message.userId().trim().isEmpty()) {
            return Result.failure(ProcessingError.invalidRequest("User ID is required"));
        }

        if (message.status() == null || message.status().trim().isEmpty()) {
            return Result.failure(ProcessingError.invalidRequest("Status is required"));
        }

        return Result.success(null);
    }

    /**
     * Determine the target ProcessingStatus based on the result message.
     */
    private ProcessingStatus determineTargetStatus(VclippingResultMessage message) {
        return switch (message.status().toUpperCase()) {
            case "COMPLETED" -> ProcessingStatus.COMPLETED;
            case "FAILED", "ERROR" -> ProcessingStatus.FAILED;
            default -> {
                logger.warn("   ⚠️  Unknown status '{}', treating as FAILED", message.status());
                yield ProcessingStatus.FAILED;
            }
        };
    }

    /**
     * Copy vclipping output to processed-videos location for download access.
     */
    private Result<String, ProcessingError> copyVclippingOutputToProcessedLocation(VclippingResultMessage message) {
        try {
            logger.info("   📁 Copying vclipping output to processed-videos location");
            logger.info("   📥 Source: {}", message.outputLocation());

            // Generate target S3 key in processed-videos location
            String originalFilename = extractOriginalFilename(message);
            String targetS3Key = String.format("processed-videos/%s/%s_frames.zip",
                                             message.videoId(),
                                             removeExtension(originalFilename));

            logger.info("   📤 Target: {}", targetS3Key);

            // Copy file from vclipping output location to processed-videos location
            // This uses the FileStoragePort to copy within S3
            return processResultPort.copyProcessedFile(message.outputLocation(), targetS3Key);

        } catch (Exception e) {
            logger.error("   ❌ Failed to copy vclipping output: {}", e.getMessage(), e);
            return Result.failure(ProcessingError.fileCopyFailed(
                "Failed to copy vclipping output to processed location", e));
        }
    }

    /**
     * Extract original filename from metadata or use fallback.
     */
    private String extractOriginalFilename(VclippingResultMessage message) {
        if (message.metadata() != null && message.metadata().containsKey("originalFileName")) {
            return message.metadata().get("originalFileName").toString();
        }
        // Fallback: use videoId
        return message.videoId();
    }

    /**
     * Remove file extension from filename.
     */
    private String removeExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
    }

    /**
     * Update video processing status using the port.
     */
    private Result<Void, ProcessingError> updateVideoStatus(
            VclippingResultMessage message,
            ProcessingStatus targetStatus,
            String processedFileS3Key) {

        try {
            return processResultPort.updateVideoStatus(
                message.videoId(),
                message.userId(),
                targetStatus,
                processedFileS3Key,  // Use copied file location
                message.extractedFrameCount(),
                message.processingDurationMs(),
                message.errorMessage()
            );

        } catch (Exception e) {
            logger.error("   ❌ Unexpected error updating video status: {}", e.getMessage(), e);
            return Result.failure(ProcessingError.statusUpdateFailed("Failed to update video status", e));
        }
    }

    /**
     * Parse raw JSON message body into VclippingResultMessage.
     * Application layer responsibility for message parsing and validation.
     */
    private VclippingResultMessage parseRawMessage(String rawMessageBody) {
        try {
            VclippingResultMessage message = objectMapper.readValue(rawMessageBody, VclippingResultMessage.class);

            logger.debug("✅ Result message parsed successfully: {}", message.toSummary());

            return message;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse SQS result message", e);
        }
    }

    // Supporting records

    /**
     * Request record for result processing.
     */
    public record ResultProcessingRequest(
        VclippingResultMessage resultMessage
    ) {}

    /**
     * Response record for result processing.
     */
    public record ResultProcessingResponse(
        String videoId,
        String userId,
        String finalStatus,
        String outputLocation,
        Integer extractedFrameCount,
        Long processingDurationMs,
        boolean success,
        String errorMessage
    ) {}
}
