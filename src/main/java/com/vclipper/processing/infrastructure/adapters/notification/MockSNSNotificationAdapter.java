package com.vclipper.processing.infrastructure.adapters.notification;

import com.vclipper.processing.application.ports.NotificationPort;
import com.vclipper.processing.domain.entity.NotificationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of SNS notification service for development and testing
 * Simulates SNS operations with console logging and notification tracking
 */
public class MockSNSNotificationAdapter implements NotificationPort {
    
    private static final Logger logger = LoggerFactory.getLogger(MockSNSNotificationAdapter.class);
    
    // In-memory storage for sent notifications (for debugging/monitoring)
    private final ConcurrentHashMap<String, SentNotification> notificationHistory = new ConcurrentHashMap<>();
    
    @Override
    public String sendNotification(String userId, NotificationType notificationType, NotificationData notificationData) {
        String notificationId = UUID.randomUUID().toString();
        
        logger.info("📧 MOCK SNS: Sending notification");
        logger.info("   🆔 Notification ID: {}", notificationId);
        logger.info("   👤 User ID: {}", userId);
        logger.info("   🏷️  Type: {}", notificationType.type());
        logger.info("   📋 Subject: {}", notificationType.subject());
        
        // Format the message with actual data
        String formattedMessage = notificationType.formatMessage(
            notificationData.videoName(),
            notificationData.downloadUrl(),
            notificationData.errorMessage()
        );
        
        logger.info("   💬 Message: {}", formattedMessage);
        logger.info("   📤 Topic: vclipper-notifications");
        logger.info("   ⏰ Sent at: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Store notification for history/debugging
        SentNotification sentNotification = new SentNotification(
            notificationId,
            userId,
            notificationType.type(),
            notificationType.subject(),
            formattedMessage,
            LocalDateTime.now()
        );
        notificationHistory.put(notificationId, sentNotification);
        
        // Simulate different notification types with specific logging
        switch (notificationType.type()) {
            case "UPLOAD_CONFIRMED" -> {
                logger.info("   🎬 Video '{}' upload confirmed for user {}", notificationData.videoName(), userId);
                logger.info("   📱 User would receive: Upload confirmation notification");
            }
            case "PROCESSING_STARTED" -> {
                logger.info("   ⚙️  Video '{}' processing started for user {}", notificationData.videoName(), userId);
                logger.info("   📱 User would receive: Processing started notification");
            }
            case "PROCESSING_COMPLETED" -> {
                logger.info("   ✅ Video '{}' processing completed for user {}", notificationData.videoName(), userId);
                logger.info("   🔗 Download URL: {}", notificationData.downloadUrl());
                logger.info("   📱 User would receive: Download ready notification with link");
            }
            case "PROCESSING_FAILED" -> {
                logger.info("   ❌ Video '{}' processing failed for user {}", notificationData.videoName(), userId);
                logger.info("   🐛 Error: {}", notificationData.errorMessage());
                logger.info("   📱 User would receive: Processing failed notification with error details");
            }
        }
        
        logger.info("   ✅ Notification successfully sent via SNS");
        
        return notificationId;
    }
    
    @Override
    public String sendCustomNotification(String userId, String subject, String message) {
        String notificationId = UUID.randomUUID().toString();
        
        logger.info("📧 MOCK SNS: Sending custom notification");
        logger.info("   🆔 Notification ID: {}", notificationId);
        logger.info("   👤 User ID: {}", userId);
        logger.info("   📋 Subject: {}", subject);
        logger.info("   💬 Message: {}", message);
        logger.info("   📤 Topic: vclipper-notifications");
        logger.info("   ⏰ Sent at: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Store notification for history/debugging
        SentNotification sentNotification = new SentNotification(
            notificationId,
            userId,
            "CUSTOM",
            subject,
            message,
            LocalDateTime.now()
        );
        notificationHistory.put(notificationId, sentNotification);
        
        logger.info("   ✅ Custom notification successfully sent via SNS");
        
        return notificationId;
    }
    
    @Override
    public boolean isHealthy() {
        logger.debug("💚 MOCK SNS: Health check - Always healthy in mock mode");
        return true;
    }
    
    /**
     * Get notification history (for debugging/monitoring)
     */
    public int getNotificationCount() {
        int count = notificationHistory.size();
        logger.debug("📊 MOCK SNS: Total notifications sent: {}", count);
        return count;
    }
    
    /**
     * Get notification history for a user (for debugging)
     */
    public long getUserNotificationCount(String userId) {
        long count = notificationHistory.values().stream()
            .filter(notification -> notification.userId().equals(userId))
            .count();
        logger.debug("📊 MOCK SNS: Notifications sent to user {}: {}", userId, count);
        return count;
    }
    
    /**
     * Clear notification history (for testing)
     */
    public void clearHistory() {
        logger.info("🧹 MOCK SNS: Clearing notification history");
        notificationHistory.clear();
    }
    
    /**
     * Record of sent notification for tracking
     */
    private record SentNotification(
        String notificationId,
        String userId,
        String type,
        String subject,
        String message,
        LocalDateTime sentAt
    ) {}
}
