package com.vclipper.processing.application.ports;

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
     * Video processing message structure
     */
    record VideoProcessingMessage(
        String videoId,
        String userId,
        String storageReference,
        String originalFilename,
        long fileSizeBytes,
        String contentType
    ) {
        /**
         * Create processing message from video processing request
         */
        public static VideoProcessingMessage from(String videoId, String userId, 
                                                String storageReference, String originalFilename,
                                                long fileSizeBytes, String contentType) {
            return new VideoProcessingMessage(
                videoId, userId, storageReference, 
                originalFilename, fileSizeBytes, contentType
            );
        }
    }
}
