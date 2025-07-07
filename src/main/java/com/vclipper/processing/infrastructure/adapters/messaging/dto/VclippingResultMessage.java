package com.vclipper.processing.infrastructure.adapters.messaging.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * DTO for result messages received from vclipping service via SQS.
 * 
 * This DTO matches the exact format sent by vclipping service:
 * {
 *   "videoId": "9af3cb58-83bf-4dc8-ba7c-ead3c558c671",
 *   "userId": "cross-service-test-1751833886",
 *   "status": "COMPLETED",
 *   "outputLocation": "vclipping-frames//VID_20250301_104432_frames_1751833910022.zip",
 *   "extractedFrameCount": 0,
 *   "processingDurationMs": 7529,
 *   "errorMessage": null,
 *   "metadata": {
 *     "timestamp": 1751833913282,
 *     "serviceName": "vclipping-service",
 *     "originalFileName": "VID_20250301_104432.mp4"
 *   }
 * }
 * 
 * Infrastructure layer responsibility:
 * - JSON deserialization from SQS messages
 * - Type-safe access to message fields
 * - Validation of required fields
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VclippingResultMessage(
    @JsonProperty("videoId")
    String videoId,
    
    @JsonProperty("userId") 
    String userId,
    
    @JsonProperty("status")
    String status,
    
    @JsonProperty("outputLocation")
    String outputLocation,
    
    @JsonProperty("extractedFrameCount")
    Integer extractedFrameCount,
    
    @JsonProperty("processingDurationMs")
    Long processingDurationMs,
    
    @JsonProperty("errorMessage")
    String errorMessage,
    
    @JsonProperty("metadata")
    Map<String, Object> metadata
) {
    
    /**
     * Validate that this message contains all required fields for processing.
     * 
     * @return true if message is valid for processing
     */
    public boolean isValid() {
        return videoId != null && !videoId.trim().isEmpty() &&
               userId != null && !userId.trim().isEmpty() &&
               status != null && !status.trim().isEmpty();
    }
    
    /**
     * Check if this represents a successful processing result.
     * 
     * @return true if status indicates successful processing
     */
    public boolean isSuccess() {
        return "COMPLETED".equalsIgnoreCase(status);
    }
    
    /**
     * Check if this represents a failed processing result.
     * 
     * @return true if status indicates failed processing
     */
    public boolean isFailure() {
        return "FAILED".equalsIgnoreCase(status) || 
               "ERROR".equalsIgnoreCase(status);
    }
    
    /**
     * Get the original filename from metadata if available.
     * 
     * @return original filename or null if not available
     */
    public String getOriginalFileName() {
        if (metadata == null) {
            return null;
        }
        Object fileName = metadata.get("originalFileName");
        return fileName instanceof String ? (String) fileName : null;
    }
    
    /**
     * Get the processing timestamp from metadata if available.
     * 
     * @return processing timestamp or null if not available
     */
    public Long getProcessingTimestamp() {
        if (metadata == null) {
            return null;
        }
        Object timestamp = metadata.get("timestamp");
        if (timestamp instanceof Number) {
            return ((Number) timestamp).longValue();
        }
        return null;
    }
    
    /**
     * Get the service name from metadata if available.
     * 
     * @return service name or null if not available
     */
    public String getServiceName() {
        if (metadata == null) {
            return null;
        }
        Object serviceName = metadata.get("serviceName");
        return serviceName instanceof String ? (String) serviceName : null;
    }
    
    /**
     * Create a summary string for logging purposes.
     * 
     * @return formatted summary of the result message
     */
    public String toSummary() {
        return String.format("VclippingResult[videoId=%s, userId=%s, status=%s, frames=%d, duration=%dms]",
            videoId, userId, status, 
            extractedFrameCount != null ? extractedFrameCount : 0,
            processingDurationMs != null ? processingDurationMs : 0);
    }
}
