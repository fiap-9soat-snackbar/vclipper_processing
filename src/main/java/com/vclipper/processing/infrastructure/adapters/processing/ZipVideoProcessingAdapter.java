package com.vclipper.processing.infrastructure.adapters.processing;

import com.vclipper.processing.application.ports.FileStoragePort;
import com.vclipper.processing.application.ports.VideoProcessingPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Infrastructure adapter that implements video processing by creating ZIP files
 * 
 * This is a simulation of video processing that:
 * 1. Downloads the original MP4 from S3
 * 2. Creates a ZIP file containing the MP4
 * 3. Uploads the ZIP back to S3
 * 4. Returns the S3 key for the ZIP file
 * 
 * In a real implementation, this could be replaced with actual frame extraction,
 * video transcoding, or other processing workflows.
 */
@Component
public class ZipVideoProcessingAdapter implements VideoProcessingPort {
    
    private static final Logger logger = LoggerFactory.getLogger(ZipVideoProcessingAdapter.class);
    
    private final FileStoragePort fileStorage;
    
    public ZipVideoProcessingAdapter(FileStoragePort fileStorage) {
        this.fileStorage = fileStorage;
    }
    
    @Override
    public String processVideo(String videoId, String originalFileS3Key, String originalFilename) {
        logger.info("🎬 Starting ZIP processing for video: {}", videoId);
        logger.info("   📁 Original file: {}", originalFilename);
        logger.info("   🗂️  Original S3 key: {}", originalFileS3Key);
        
        try {
            // Step 1: Download original MP4 from S3
            logger.info("   ⬇️  Downloading original video from S3...");
            InputStream originalVideoStream = fileStorage.downloadFile(originalFileS3Key);
            
            // Step 2: Create ZIP containing the MP4
            logger.info("   📦 Creating ZIP file...");
            ByteArrayInputStream zipStream = createZipWithMp4(originalVideoStream, originalFilename);
            
            // Step 3: Upload ZIP back to S3
            logger.info("   ⬆️  Uploading ZIP to S3...");
            String processedS3Key = fileStorage.storeProcessedFile(zipStream, videoId, originalFilename);
            
            logger.info("   ✅ ZIP processing completed successfully");
            logger.info("   🗂️  Processed S3 key: {}", processedS3Key);
            
            return processedS3Key;
            
        } catch (Exception e) {
            logger.error("   ❌ Error processing video to ZIP: {}", videoId, e);
            throw new RuntimeException("Failed to process video to ZIP: " + videoId, e);
        }
    }
    
    /**
     * Create a ZIP file containing the MP4 video
     * 
     * @param mp4Stream Input stream of the MP4 video
     * @param originalFilename Original filename for the ZIP entry
     * @return ByteArrayInputStream containing the ZIP file
     */
    private ByteArrayInputStream createZipWithMp4(InputStream mp4Stream, String originalFilename) throws IOException {
        logger.debug("   📦 Creating ZIP with MP4: {}", originalFilename);
        
        ByteArrayOutputStream zipBuffer = new ByteArrayOutputStream();
        
        try (ZipOutputStream zipOut = new ZipOutputStream(zipBuffer)) {
            // Create ZIP entry for the MP4 file
            ZipEntry zipEntry = new ZipEntry(originalFilename);
            zipOut.putNextEntry(zipEntry);
            
            // Copy MP4 content to ZIP
            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            
            while ((bytesRead = mp4Stream.read(buffer)) != -1) {
                zipOut.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;
            }
            
            zipOut.closeEntry();
            logger.debug("   ✅ ZIP created successfully, total bytes: {}", totalBytes);
            
        } finally {
            // Close the original stream
            try {
                mp4Stream.close();
            } catch (IOException e) {
                logger.warn("   ⚠️  Error closing MP4 stream: {}", e.getMessage());
            }
        }
        
        // Return ZIP as input stream
        return new ByteArrayInputStream(zipBuffer.toByteArray());
    }
}
