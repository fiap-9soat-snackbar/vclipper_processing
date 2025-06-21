package com.vclipper.processing.application.ports;

import com.vclipper.processing.domain.entity.NotificationType;

/**
 * Port interface for notification operations
 * Abstracts notification implementation (SNS, email, SMS, etc.)
 */
public interface NotificationPort {
    
    /**
     * Send notification to user
     * 
     * @param userId User identifier
     * @param notificationType Type of notification with message template
     * @param notificationData Data to populate message template
     * @return Notification ID or reference for tracking
     */
    String sendNotification(String userId, NotificationType notificationType, NotificationData notificationData);
    
    /**
     * Send notification to user with custom message
     * 
     * @param userId User identifier
     * @param subject Notification subject
     * @param message Custom message content
     * @return Notification ID or reference for tracking
     */
    String sendCustomNotification(String userId, String subject, String message);
    
    /**
     * Check if notification service is healthy and accessible
     * 
     * @return true if service is accessible, false otherwise
     */
    boolean isHealthy();
    
    /**
     * Notification data for template population
     */
    record NotificationData(
        String videoName,
        String downloadUrl,
        String errorMessage
    ) {
        /**
         * Create notification data for upload confirmation
         */
        public static NotificationData forUpload(String videoName) {
            return new NotificationData(videoName, null, null);
        }
        
        /**
         * Create notification data for processing completion
         */
        public static NotificationData forCompletion(String videoName, String downloadUrl) {
            return new NotificationData(videoName, downloadUrl, null);
        }
        
        /**
         * Create notification data for processing failure
         */
        public static NotificationData forFailure(String videoName, String errorMessage) {
            return new NotificationData(videoName, null, errorMessage);
        }
        
        /**
         * Create notification data for processing start
         */
        public static NotificationData forProcessingStart(String videoName) {
            return new NotificationData(videoName, null, null);
        }
    }
}
