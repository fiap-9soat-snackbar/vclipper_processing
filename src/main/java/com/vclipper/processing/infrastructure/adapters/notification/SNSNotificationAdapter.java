package com.vclipper.processing.infrastructure.adapters.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.ports.NotificationPort;
import com.vclipper.processing.domain.entity.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

/**
 * AWS SNS implementation for notification operations
 * Provides real SNS integration for sending notifications
 */
public class SNSNotificationAdapter implements NotificationPort {
    
    private static final Logger logger = LoggerFactory.getLogger(SNSNotificationAdapter.class);
    
    private final SnsClient snsClient;
    private final String topicArn;
    private final ObjectMapper objectMapper;
    
    public SNSNotificationAdapter(SnsClient snsClient, String topicArn, ObjectMapper objectMapper) {
        this.snsClient = snsClient;
        this.topicArn = topicArn;
        this.objectMapper = objectMapper;
        logger.info("🚀 Real SNS Notification Adapter initialized with topic: {}", topicArn);
    }
    
    @Override
    public String sendNotification(String userId, NotificationType notificationType, NotificationData notificationData) {
        try {
            // Generate message from template
            String subject = notificationType.subject();
            String message = notificationType.formatMessage(
                notificationData.videoName(),
                notificationData.downloadUrl(),
                notificationData.errorMessage()
            );
            
            // Create structured notification message
            NotificationMessage structuredMessage = new NotificationMessage(
                userId,
                notificationType.type(),
                subject,
                message,
                notificationData.videoName(),
                notificationData.downloadUrl(),
                notificationData.errorMessage(),
                System.currentTimeMillis()
            );
            
            // Serialize message to JSON for structured processing
            String messageBody = objectMapper.writeValueAsString(structuredMessage);
            
            // Build publish request
            PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .subject(subject)
                .message(messageBody)
                .build();
            
            logger.info("📧 Sending notification via SNS - UserId: {}, Type: {}, Subject: {}", 
                userId, notificationType, subject);
            logger.debug("Notification message: {}", messageBody);
            
            PublishResponse response = snsClient.publish(publishRequest);
            
            logger.info("✅ Successfully sent notification via SNS - MessageId: {}", response.messageId());
            
            return response.messageId();
            
        } catch (JsonProcessingException e) {
            logger.error("❌ Failed to serialize notification message - UserId: {}, Type: {}", 
                userId, notificationType, e);
            throw new RuntimeException("Failed to serialize notification message", e);
        } catch (SnsException e) {
            logger.error("❌ Failed to send notification via SNS - UserId: {}, Type: {}, Error: {}", 
                userId, notificationType, e.getMessage(), e);
            throw new RuntimeException("Failed to send notification via SNS", e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error sending notification via SNS - UserId: {}, Type: {}", 
                userId, notificationType, e);
            throw new RuntimeException("Unexpected error sending notification via SNS", e);
        }
    }
    
    @Override
    public String sendCustomNotification(String userId, String subject, String message) {
        try {
            // Create structured notification message for custom notification
            NotificationMessage structuredMessage = new NotificationMessage(
                userId,
                "CUSTOM",
                subject,
                message,
                null, // no video name for custom notifications
                null, // no download URL for custom notifications
                null, // no error message for custom notifications
                System.currentTimeMillis()
            );
            
            // Serialize message to JSON for structured processing
            String messageBody = objectMapper.writeValueAsString(structuredMessage);
            
            // Build publish request
            PublishRequest publishRequest = PublishRequest.builder()
                .topicArn(topicArn)
                .subject(subject)
                .message(messageBody)
                .build();
            
            logger.info("📧 Sending custom notification via SNS - UserId: {}, Subject: {}", userId, subject);
            logger.debug("Custom notification message: {}", messageBody);
            
            PublishResponse response = snsClient.publish(publishRequest);
            
            logger.info("✅ Successfully sent custom notification via SNS - MessageId: {}", response.messageId());
            
            return response.messageId();
            
        } catch (JsonProcessingException e) {
            logger.error("❌ Failed to serialize custom notification message - UserId: {}, Subject: {}", 
                userId, subject, e);
            throw new RuntimeException("Failed to serialize custom notification message", e);
        } catch (SnsException e) {
            logger.error("❌ Failed to send custom notification via SNS - UserId: {}, Subject: {}, Error: {}", 
                userId, subject, e.getMessage(), e);
            throw new RuntimeException("Failed to send custom notification via SNS", e);
        } catch (Exception e) {
            logger.error("❌ Unexpected error sending custom notification via SNS - UserId: {}, Subject: {}", 
                userId, subject, e);
            throw new RuntimeException("Unexpected error sending custom notification via SNS", e);
        }
    }
    
    @Override
    public boolean isHealthy() {
        try {
            // Simple health check - try to get topic attributes
            snsClient.getTopicAttributes(builder -> builder.topicArn(topicArn));
            logger.debug("✅ SNS health check passed for topic: {}", topicArn);
            return true;
        } catch (Exception e) {
            logger.warn("❌ SNS health check failed for topic: {} - Error: {}", topicArn, e.getMessage());
            return false;
        }
    }
    
    /**
     * Structured notification message for SNS
     */
    public record NotificationMessage(
        String userId,
        String type,
        String subject,
        String message,
        String videoName,
        String downloadUrl,
        String errorMessage,
        long timestamp
    ) {}
}
