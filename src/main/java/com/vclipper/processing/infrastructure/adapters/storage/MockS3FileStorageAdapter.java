package com.vclipper.processing.infrastructure.adapters.storage;

import com.vclipper.processing.application.ports.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of S3 file storage for development and testing
 * Simulates S3 operations with console logging and in-memory metadata storage
 */
public class MockS3FileStorageAdapter implements FileStoragePort {
    
    private static final Logger logger = LoggerFactory.getLogger(MockS3FileStorageAdapter.class);
    
    // In-memory storage for file metadata simulation
    private final Map<String, FileMetadata> fileMetadataStore = new ConcurrentHashMap<>();
    
    @Override
    public String store(InputStream inputStream, String filename, String contentType, long fileSizeBytes) {
        logger.info("🗂️  MOCK S3: Storing file");
        logger.info("   📁 Filename: {}", filename);
        logger.info("   📊 Size: {} bytes ({} MB)", fileSizeBytes, String.format("%.2f", fileSizeBytes / (1024.0 * 1024.0)));
        logger.info("   🏷️  Content Type: {}", contentType);
        
        try {
            // Simulate reading the input stream (but don't actually store it)
            byte[] buffer = new byte[8192];
            long totalBytesRead = 0;
            int bytesRead;
            
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                totalBytesRead += bytesRead;
                // Simulate processing time
                if (totalBytesRead % (1024 * 1024) == 0) { // Every MB
                    logger.debug("   📤 Uploaded: {} MB", totalBytesRead / (1024 * 1024));
                }
            }
            
            // Generate mock storage reference
            String storageReference = String.format("videos/%s/%s-%s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")),
                UUID.randomUUID().toString().substring(0, 8),
                filename);
            
            // Store metadata for later retrieval
            FileMetadata metadata = new FileMetadata(
                fileSizeBytes,
                contentType,
                LocalDateTime.now().toString()
            );
            fileMetadataStore.put(storageReference, metadata);
            
            logger.info("   ✅ Successfully stored as: {}", storageReference);
            logger.info("   🔗 Mock S3 URL: s3://vclipper-videos-dev/{}", storageReference);
            
            return storageReference;
            
        } catch (IOException e) {
            logger.error("   ❌ Error reading input stream for file: {}", filename, e);
            throw new RuntimeException("Failed to store file: " + filename, e);
        }
    }
    
    @Override
    public String generateDownloadUrl(String storageReference, int expirationMinutes) {
        logger.info("🔗 MOCK S3: Generating download URL");
        logger.info("   📁 Storage Reference: {}", storageReference);
        logger.info("   ⏰ Expiration: {} minutes", expirationMinutes);
        
        if (!fileMetadataStore.containsKey(storageReference)) {
            logger.warn("   ⚠️  File not found in mock storage: {}", storageReference);
            throw new RuntimeException("File not found: " + storageReference);
        }
        
        // Generate mock presigned URL
        String presignedUrl = String.format(
            "https://vclipper-videos-dev.s3.amazonaws.com/%s?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Expires=%d&X-Amz-SignedHeaders=host&X-Amz-Signature=mock-signature-%s",
            storageReference,
            expirationMinutes * 60,
            UUID.randomUUID().toString().substring(0, 16)
        );
        
        logger.info("   ✅ Generated URL: {}", presignedUrl);
        logger.info("   ⏰ Expires at: {}", LocalDateTime.now().plusMinutes(expirationMinutes));
        
        return presignedUrl;
    }
    
    @Override
    public boolean exists(String storageReference) {
        boolean exists = fileMetadataStore.containsKey(storageReference);
        logger.debug("🔍 MOCK S3: Checking if file exists: {} -> {}", storageReference, exists);
        return exists;
    }
    
    @Override
    public boolean delete(String storageReference) {
        logger.info("🗑️  MOCK S3: Deleting file: {}", storageReference);
        
        boolean existed = fileMetadataStore.remove(storageReference) != null;
        
        if (existed) {
            logger.info("   ✅ Successfully deleted: {}", storageReference);
        } else {
            logger.warn("   ⚠️  File not found for deletion: {}", storageReference);
        }
        
        return existed;
    }
    
    @Override
    public FileMetadata getMetadata(String storageReference) {
        logger.debug("📋 MOCK S3: Getting metadata for: {}", storageReference);
        
        FileMetadata metadata = fileMetadataStore.get(storageReference);
        
        if (metadata != null) {
            logger.debug("   ✅ Found metadata: {} bytes, {}", metadata.sizeBytes(), metadata.contentType());
        } else {
            logger.warn("   ⚠️  No metadata found for: {}", storageReference);
        }
        
        return metadata;
    }
}
