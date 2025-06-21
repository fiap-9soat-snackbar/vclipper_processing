package com.vclipper.processing.infrastructure.adapters.persistence;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * MongoDB entity for video processing requests (single record approach)
 * Stores current state of video processing with basic audit information
 */
@Data
@Document(collection = "videoProcessingRequests")
public class VideoProcessingEntity {
    
    @Id
    private String videoId;
    
    @Indexed
    private String userId;
    
    // Video metadata
    private String originalFilename;
    private long fileSizeBytes;
    private String videoFormat;
    private String contentType;
    private String storageReference;
    
    // Processing status (current state)
    private String statusValue;
    private String statusDescription;
    private boolean statusIsTerminal;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Processing results
    private String errorMessage;
    private String processedFileReference;
    
    // Basic retry tracking
    private int retryCount;
    
    // Default constructor for MongoDB
    public VideoProcessingEntity() {}
    
    // Constructor for creating new entities
    public VideoProcessingEntity(String videoId, String userId, String originalFilename, 
                               long fileSizeBytes, String videoFormat, String contentType, 
                               String storageReference, String statusValue, String statusDescription, 
                               boolean statusIsTerminal) {
        this.videoId = videoId;
        this.userId = userId;
        this.originalFilename = originalFilename;
        this.fileSizeBytes = fileSizeBytes;
        this.videoFormat = videoFormat;
        this.contentType = contentType;
        this.storageReference = storageReference;
        this.statusValue = statusValue;
        this.statusDescription = statusDescription;
        this.statusIsTerminal = statusIsTerminal;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.retryCount = 0;
    }
    
    /**
     * Update status and timestamp
     */
    public void updateStatus(String statusValue, String statusDescription, boolean statusIsTerminal, 
                           String errorMessage, String processedFileReference) {
        this.statusValue = statusValue;
        this.statusDescription = statusDescription;
        this.statusIsTerminal = statusIsTerminal;
        this.errorMessage = errorMessage;
        this.processedFileReference = processedFileReference;
        this.updatedAt = LocalDateTime.now();
        
        // Increment retry count if moving from FAILED to PENDING
        if ("PENDING".equals(statusValue) && this.retryCount > 0) {
            this.retryCount++;
        }
    }
    
    /**
     * Mark as retry attempt
     */
    public void incrementRetryCount() {
        this.retryCount++;
        this.updatedAt = LocalDateTime.now();
    }
}
