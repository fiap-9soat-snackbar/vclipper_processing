package com.vclipper.processing.domain.entity;

/**
 * Value object representing notification types with message templates
 */
public record NotificationType(String type, String messageTemplate, String subject) {
    
    public static final NotificationType UPLOAD_CONFIRMED = new NotificationType(
        "UPLOAD_CONFIRMED",
        "Your video '{videoName}' has been uploaded and queued for processing",
        "Video Upload Confirmed"
    );
    
    public static final NotificationType PROCESSING_STARTED = new NotificationType(
        "PROCESSING_STARTED",
        "Your video '{videoName}' is now being processed. You'll be notified when it's ready",
        "Video Processing Started"
    );
    
    public static final NotificationType PROCESSING_COMPLETED = new NotificationType(
        "PROCESSING_COMPLETED",
        "Your video '{videoName}' has been processed successfully. Download is ready: {downloadUrl}",
        "Video Processing Completed"
    );
    
    public static final NotificationType PROCESSING_FAILED = new NotificationType(
        "PROCESSING_FAILED",
        "Your video '{videoName}' processing failed: {errorMessage}. Please try uploading again",
        "Video Processing Failed"
    );
    
    /**
     * Replace placeholders in message template with actual values
     */
    public String formatMessage(String videoName, String downloadUrl, String errorMessage) {
        return messageTemplate
                .replace("{videoName}", videoName != null ? videoName : "your video")
                .replace("{downloadUrl}", downloadUrl != null ? downloadUrl : "")
                .replace("{errorMessage}", errorMessage != null ? errorMessage : "Unknown error");
    }
    
    /**
     * Factory method for creating notification type from string
     */
    public static NotificationType fromType(String type) {
        return switch (type.toUpperCase()) {
            case "UPLOAD_CONFIRMED" -> UPLOAD_CONFIRMED;
            case "PROCESSING_STARTED" -> PROCESSING_STARTED;
            case "PROCESSING_COMPLETED" -> PROCESSING_COMPLETED;
            case "PROCESSING_FAILED" -> PROCESSING_FAILED;
            default -> throw new IllegalArgumentException("Invalid notification type: " + type);
        };
    }
}
