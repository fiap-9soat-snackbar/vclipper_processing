package com.vclipper.processing.infrastructure.adapters.storage;

import com.vclipper.processing.application.ports.FileStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.InputStream;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Real S3 implementation of FileStoragePort
 * Handles actual file storage operations with Amazon S3
 */
public class S3FileStorageAdapter implements FileStoragePort {
    
    private static final Logger logger = LoggerFactory.getLogger(S3FileStorageAdapter.class);
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    
    public S3FileStorageAdapter(S3Client s3Client, S3Presigner s3Presigner, String bucketName) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
    }
    
    @Override
    public String store(InputStream inputStream, String filename, String contentType, long fileSizeBytes) {
        logger.info("🗂️  S3: Storing file to bucket: {}", bucketName);
        logger.info("   📁 Filename: {}", filename);
        logger.info("   📊 Size: {} bytes ({} MB)", fileSizeBytes, String.format("%.2f", fileSizeBytes / (1024.0 * 1024.0)));
        logger.info("   🏷️  Content Type: {}", contentType);
        
        try {
            // Generate storage key with date-based prefix
            String storageKey = generateStorageKey(filename);
            
            // Create put object request
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(storageKey)
                .contentType(contentType)
                .contentLength(fileSizeBytes)
                .build();
            
            // Upload file to S3
            PutObjectResponse response = s3Client.putObject(putObjectRequest, 
                RequestBody.fromInputStream(inputStream, fileSizeBytes));
            
            logger.info("   ✅ Successfully stored in S3");
            logger.info("   🔗 S3 Key: {}", storageKey);
            logger.info("   📋 ETag: {}", response.eTag());
            
            return storageKey;
            
        } catch (Exception e) {
            logger.error("   ❌ Error storing file in S3: {}", filename, e);
            throw new RuntimeException("Failed to store file in S3: " + filename, e);
        }
    }
    
    @Override
    public String generateDownloadUrl(String storageReference, int expirationMinutes) {
        logger.info("🔗 S3: Generating presigned download URL");
        logger.info("   📁 Storage Key: {}", storageReference);
        logger.info("   ⏰ Expiration: {} minutes", expirationMinutes);
        
        try {
            // Create get object request
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageReference)
                .build();
            
            // Create presign request
            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();
            
            // Generate presigned URL
            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String presignedUrl = presignedRequest.url().toString();
            
            logger.info("   ✅ Generated presigned URL");
            logger.info("   ⏰ Expires at: {}", LocalDateTime.now().plusMinutes(expirationMinutes));
            
            return presignedUrl;
            
        } catch (Exception e) {
            logger.error("   ❌ Error generating presigned URL for: {}", storageReference, e);
            throw new RuntimeException("Failed to generate download URL: " + storageReference, e);
        }
    }
    
    @Override
    public boolean exists(String storageReference) {
        logger.debug("🔍 S3: Checking if file exists: {}", storageReference);
        
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(storageReference)
                .build();
            
            s3Client.headObject(headObjectRequest);
            logger.debug("   ✅ File exists: {}", storageReference);
            return true;
            
        } catch (NoSuchKeyException e) {
            logger.debug("   ❌ File not found: {}", storageReference);
            return false;
        } catch (Exception e) {
            logger.error("   ❌ Error checking file existence: {}", storageReference, e);
            return false;
        }
    }
    
    @Override
    public boolean delete(String storageReference) {
        logger.info("🗑️  S3: Deleting file: {}", storageReference);
        
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(storageReference)
                .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            logger.info("   ✅ Successfully deleted: {}", storageReference);
            return true;
            
        } catch (Exception e) {
            logger.error("   ❌ Error deleting file: {}", storageReference, e);
            return false;
        }
    }
    
    @Override
    public FileMetadata getMetadata(String storageReference) {
        logger.debug("📋 S3: Getting metadata for: {}", storageReference);
        
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(storageReference)
                .build();
            
            HeadObjectResponse response = s3Client.headObject(headObjectRequest);
            
            FileMetadata metadata = new FileMetadata(
                response.contentLength(),
                response.contentType(),
                response.lastModified().toString()
            );
            
            logger.debug("   ✅ Found metadata: {} bytes, {}", metadata.sizeBytes(), metadata.contentType());
            return metadata;
            
        } catch (NoSuchKeyException e) {
            logger.warn("   ⚠️  No metadata found for: {}", storageReference);
            return null;
        } catch (Exception e) {
            logger.error("   ❌ Error getting metadata: {}", storageReference, e);
            return null;
        }
    }
    
    @Override
    public InputStream downloadFile(String storageReference) {
        logger.info("⬇️  S3: Downloading file: {}", storageReference);
        
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(storageReference)
                .build();
            
            InputStream inputStream = s3Client.getObject(getObjectRequest);
            logger.info("   ✅ Successfully downloaded: {}", storageReference);
            return inputStream;
            
        } catch (Exception e) {
            logger.error("   ❌ Error downloading file: {}", storageReference, e);
            throw new RuntimeException("Failed to download file: " + storageReference, e);
        }
    }
    
    @Override
    public String storeProcessedFile(InputStream inputStream, String videoId, String originalFilename) {
        logger.info("📦 S3: Storing processed file for video: {}", videoId);
        logger.info("   📁 Original filename: {}", originalFilename);
        
        try {
            // Generate processed file key with standardized naming
            String filenameWithoutExt = getFilenameWithoutExtension(originalFilename);
            String processedKey = String.format("processed-videos/%s/%s_frames.zip", videoId, filenameWithoutExt);
            
            logger.info("   🗂️  Processed key: {}", processedKey);
            
            // Read the input stream into a byte array to get the content length
            byte[] zipBytes = inputStream.readAllBytes();
            logger.info("   📊 ZIP file size: {} bytes", zipBytes.length);
            
            // Create put object request
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(processedKey)
                .contentType("application/zip")
                .contentLength((long) zipBytes.length)
                .build();
            
            // Upload processed file with correct content length
            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(zipBytes));
            
            logger.info("   ✅ Successfully stored processed file: {}", processedKey);
            return processedKey;
            
        } catch (Exception e) {
            logger.error("   ❌ Error storing processed file for video: {}", videoId, e);
            throw new RuntimeException("Failed to store processed file for video: " + videoId, e);
        }
    }
    
    /**
     * Helper method to extract filename without extension
     */
    private String getFilenameWithoutExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
    }
    
    /**
     * Generate a unique storage key with date-based prefix
     */
    private String generateStorageKey(String originalFilename) {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return String.format("videos/%s/%s-%s", datePrefix, uniqueId, originalFilename);
    }
}
