package com.vclipper.processing.application.ports;

import java.util.Map;
import java.util.UUID;

/**
 * Port interface for message queue operations
 * Abstracts message queue implementation (SQS, RabbitMQ, etc.)
 */
public interface MessageQueuePort {
    
    /**
     * Send a video processing message to the queue
     * 
     * @param processingMessage Message containing processing details
     * @return Message ID or reference for tracking
     */
    String sendProcessingMessage(VideoProcessingMessage processingMessage);
    
    /**
     * Send a message with delay (for retry mechanisms, throttling, budget management)
     * 
     * @param processingMessage Message to send
     * @param delaySeconds Delay in seconds before message becomes available
     * @return Message ID or reference for tracking
     */
    String sendDelayedMessage(VideoProcessingMessage processingMessage, int delaySeconds);
    
    /**
     * Check if message queue is healthy and accessible
     * 
     * @return true if queue is accessible, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Enhanced video processing message structure with flexible parameters
     */
    record VideoProcessingMessage(
        String messageId,                    // NEW: UUID for message tracking
        String videoId,
        String userId,
        String storageLocation,              // RENAMED: from storageReference
        String originalFilename,
        long fileSizeBytes,
        String contentType,
        Map<String, Object> processingOptions // NEW: Flexible parameters
    ) {
        /**
         * Factory method with default processing options
         */
        public static VideoProcessingMessage withDefaults(
                String videoId, String userId, String storageLocation,
                String originalFilename, long fileSizeBytes, String contentType) {
            
            Map<String, Object> defaultOptions = Map.of(
                "framesPerSecond", 1.0,
                "outputImageFormat", "PNG",
                "outputCompressionFormat", "ZIP",
                "jpegQuality", 85,
                "maintainAspectRatio", true
            );
            
            return new VideoProcessingMessage(
                UUID.randomUUID().toString(),  // Generate unique message ID
                videoId, userId, storageLocation,
                originalFilename, fileSizeBytes, contentType,
                defaultOptions
            );
        }
        
        /**
         * Factory method for backward compatibility
         */
        public static VideoProcessingMessage from(String videoId, String userId, 
                                                String storageReference, String originalFilename,
                                                long fileSizeBytes, String contentType) {
            return withDefaults(videoId, userId, storageReference, 
                              originalFilename, fileSizeBytes, contentType);
        }
    }
}
