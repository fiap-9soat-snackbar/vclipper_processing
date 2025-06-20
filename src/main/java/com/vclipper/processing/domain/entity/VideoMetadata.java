package com.vclipper.processing.domain.entity;

import com.vclipper.processing.domain.enums.VideoFormat;

/**
 * Value object containing video file metadata (clean architecture - no infrastructure details)
 */
public record VideoMetadata(
    String originalFilename,
    long fileSizeBytes,
    VideoFormat format,
    String contentType,
    String storageReference
) {
    /**
     * Get file size in MB for display purposes
     */
    public double getFileSizeMB() {
        return fileSizeBytes / (1024.0 * 1024.0);
    }
    
    /**
     * Get file extension from original filename
     */
    public String getFileExtension() {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Validate that metadata is consistent and complete
     */
    public boolean isValid() {
        return originalFilename != null && !originalFilename.trim().isEmpty()
                && fileSizeBytes > 0
                && format != null
                && storageReference != null && !storageReference.trim().isEmpty();
    }
    
    /**
     * Check if file size exceeds maximum allowed size
     */
    public boolean exceedsMaxSize(long maxSizeBytes) {
        return fileSizeBytes > maxSizeBytes;
    }
}
