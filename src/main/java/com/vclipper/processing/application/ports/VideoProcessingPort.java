package com.vclipper.processing.application.ports;

/**
 * Port for video processing operations
 * Abstracts the actual processing implementation (ZIP creation, frame extraction, etc.)
 * 
 * This follows Clean Architecture principles by defining the business interface
 * without exposing implementation details to the application layer.
 */
public interface VideoProcessingPort {
    
    /**
     * Process video and return processed file reference
     * 
     * The implementation could be:
     * - ZIP creation (current simulation)
     * - Frame extraction (future real processing)
     * - Video transcoding
     * - Any other video processing workflow
     * 
     * @param videoId Video identifier for organizing processed files
     * @param originalFileS3Key S3 key of the original uploaded video file
     * @param originalFilename Original filename for reference and naming
     * @return S3 key of the processed file that can be used for download URL generation
     * @throws RuntimeException if processing fails
     */
    String processVideo(String videoId, String originalFileS3Key, String originalFilename);
}
