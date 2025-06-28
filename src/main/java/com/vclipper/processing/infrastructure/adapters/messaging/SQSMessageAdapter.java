package com.vclipper.processing.infrastructure.adapters.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.ports.MessageQueuePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

/**
 * AWS SQS implementation for message queue operations
 * Provides real SQS integration for publishing messages
 */
public class SQSMessageAdapter implements MessageQueuePort {
    
    private static final Logger logger = LoggerFactory.getLogger(SQSMessageAdapter.class);
    
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final ObjectMapper objectMapper;
    
    public SQSMessageAdapter(SqsClient sqsClient, String queueUrl, ObjectMapper objectMapper) {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.objectMapper = objectMapper;
        logger.info("🚀 Real SQS Message Adapter initialized with queue: {}", queueUrl);
    }
    
    @Override
    public String sendProcessingMessage(VideoProcessingMessage processingMessage) {
        try {
            // Serialize message to JSON
            String messageBody = objectMapper.writeValueAsString(processingMessage);
            
            // Build send message request
            SendMessageRequest.Builder requestBuilder = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody);
            
            // Add FIFO-specific attributes if queue is FIFO
            if (queueUrl.endsWith(".fifo")) {
                requestBuilder
                    .messageGroupId("video-processing") // For FIFO queues
                    .messageDeduplicationId(processingMessage.videoId()); // Prevent duplicates
            }
            
            SendMessageRequest sendRequest = requestBuilder.build();
            
            logger.info("📨 Sending processing message to SQS for video: {}", processingMessage.videoId());
            logger.debug("Message body: {}", messageBody);
            
            SendMessageResponse response = sqsClient.sendMessage(sendRequest);
            
            logger.info("✅ Successfully sent message to SQS - MessageId: {}", response.messageId());
            
            return response.messageId();
            
        } catch (JsonProcessingException e) {
            logger.error("❌ Failed to serialize processing message for video: {}", processingMessage.videoId(), e);
            throw new RuntimeException("Failed to serialize processing message", e);
        } catch (SqsException e) {
            logger.error("❌ Failed to send message to SQS for video: {} - Error: {}", 
                processingMessage.videoId(), e.getMessage(), e);
            throw new RuntimeException("Failed to send message to SQS", e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error sending message to SQS for video: {}", processingMessage.videoId(), e);
            throw new RuntimeException("Unexpected error sending message to SQS", e);
        }
    }
    
    @Override
    public String sendDelayedMessage(VideoProcessingMessage processingMessage, int delaySeconds) {
        try {
            // Serialize message to JSON
            String messageBody = objectMapper.writeValueAsString(processingMessage);
            
            // Build send message request with delay
            SendMessageRequest.Builder requestBuilder = SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .delaySeconds(delaySeconds);
            
            // Add FIFO-specific attributes if queue is FIFO
            if (queueUrl.endsWith(".fifo")) {
                requestBuilder
                    .messageGroupId("video-processing-delayed") // Different group for delayed messages
                    .messageDeduplicationId(processingMessage.videoId() + "-" + delaySeconds); // Unique dedup ID
            }
            
            SendMessageRequest sendRequest = requestBuilder.build();
            
            logger.info("📨 Sending delayed processing message to SQS for video: {} (delay: {}s)", 
                processingMessage.videoId(), delaySeconds);
            logger.debug("Message body: {}", messageBody);
            
            SendMessageResponse response = sqsClient.sendMessage(sendRequest);
            
            logger.info("✅ Successfully sent delayed message to SQS - MessageId: {}", response.messageId());
            
            return response.messageId();
            
        } catch (JsonProcessingException e) {
            logger.error("❌ Failed to serialize delayed processing message for video: {}", processingMessage.videoId(), e);
            throw new RuntimeException("Failed to serialize delayed processing message", e);
        } catch (SqsException e) {
            logger.error("❌ Failed to send delayed message to SQS for video: {} - Error: {}", 
                processingMessage.videoId(), e.getMessage(), e);
            throw new RuntimeException("Failed to send delayed message to SQS", e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error sending delayed message to SQS for video: {}", processingMessage.videoId(), e);
            throw new RuntimeException("Unexpected error sending delayed message to SQS", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - try to get queue attributes
            sqsClient.getQueueAttributes(builder -> builder.queueUrl(queueUrl));
            logger.debug("✅ SQS health check passed for queue: {}", queueUrl);
            return true;
        } catch (Exception e) {
            logger.warn("❌ SQS health check failed for queue: {} - Error: {}", queueUrl, e.getMessage());
            return false;
        }
    }
}
