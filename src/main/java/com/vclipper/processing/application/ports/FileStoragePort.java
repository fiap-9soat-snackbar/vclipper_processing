package com.vclipper.processing.application.ports;

import java.io.InputStream;

/**
 * Port interface for file storage operations
 * Abstracts file storage implementation (S3, local filesystem, etc.)
 */
public interface FileStoragePort {
    
    /**
     * Store a file and return storage reference
     * 
     * @param inputStream File content stream
     * @param filename Original filename
     * @param contentType MIME type of the file
     * @param fileSizeBytes Size of the file in bytes
     * @return Storage reference (implementation-specific identifier)
     */
    String store(InputStream inputStream, String filename, String contentType, long fileSizeBytes);
    
    /**
     * Generate a presigned URL for file download
     * 
     * @param storageReference Storage reference returned by store()
     * @param expirationMinutes URL expiration time in minutes
     * @return Presigned download URL
     */
    String generateDownloadUrl(String storageReference, int expirationMinutes);
    
    /**
     * Check if a file exists in storage
     * 
     * @param storageReference Storage reference to check
     * @return true if file exists, false otherwise
     */
    boolean exists(String storageReference);
    
    /**
     * Delete a file from storage
     * 
     * @param storageReference Storage reference to delete
     * @return true if deletion was successful, false otherwise
     */
    boolean delete(String storageReference);
    
    /**
     * Get file metadata
     * 
     * @param storageReference Storage reference
     * @return File metadata (size, last modified, etc.)
     */
    FileMetadata getMetadata(String storageReference);
    
    /**
     * Download file content from storage
     * 
     * @param storageReference Storage reference to download
     * @return InputStream of file content
     */
    InputStream downloadFile(String storageReference);
    
    /**
     * Store processed file with standardized naming convention
     * 
     * @param inputStream Processed file content stream
     * @param videoId Video identifier for organizing processed files
     * @param originalFilename Original filename for reference
     * @return Storage reference for the processed file
     */
    String storeProcessedFile(InputStream inputStream, String videoId, String originalFilename);
    
    /**
     * File metadata information
     */
    record FileMetadata(
        long sizeBytes,
        String contentType,
        String lastModified
    ) {}
}
