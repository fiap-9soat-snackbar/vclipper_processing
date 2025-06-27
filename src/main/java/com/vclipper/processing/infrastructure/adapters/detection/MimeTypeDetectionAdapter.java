package com.vclipper.processing.infrastructure.adapters.detection;

import com.vclipper.processing.application.ports.MimeTypeDetectionPort;
import com.vclipper.processing.domain.enums.VideoFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

/**
 * Adapter for MIME type detection using magic bytes (file signatures)
 * Implements reliable file type detection independent of client-provided headers
 */
@Component
public class MimeTypeDetectionAdapter implements MimeTypeDetectionPort {
    
    private static final Logger logger = LoggerFactory.getLogger(MimeTypeDetectionAdapter.class);
    
    // MP4 magic bytes patterns
    private static final byte[] MP4_FTYP = {0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}; // "....ftyp"
    private static final byte[] MP4_FTYP_ALT = {0x66, 0x74, 0x79, 0x70}; // "ftyp" at offset 4
    
    // AVI magic bytes
    private static final byte[] AVI_RIFF = {0x52, 0x49, 0x46, 0x46}; // "RIFF"
    private static final byte[] AVI_AVI = {0x41, 0x56, 0x49, 0x20}; // "AVI "
    
    // MOV/QuickTime magic bytes
    private static final byte[] MOV_FTYP = {0x66, 0x74, 0x79, 0x70, 0x71, 0x74}; // "ftypqt"
    
    // WMV magic bytes
    private static final byte[] WMV_ASF = {0x30, 0x26, (byte)0xB2, 0x75, (byte)0x8E, 0x66, (byte)0xCF, 0x11}; // ASF header
    
    // FLV magic bytes
    private static final byte[] FLV_HEADER = {0x46, 0x4C, 0x56}; // "FLV"
    
    // WEBM magic bytes (EBML header)
    private static final byte[] WEBM_EBML = {0x1A, 0x45, (byte)0xDF, (byte)0xA3}; // EBML header
    
    @Override
    public String detectMimeType(InputStream inputStream, String filename, String clientProvidedMimeType) {
        logger.debug("Detecting MIME type for file: {}, client provided: {}", filename, clientProvidedMimeType);
        
        try {
            // First, try to detect from magic bytes
            String detectedFromContent = detectFromMagicBytes(inputStream);
            if (detectedFromContent != null) {
                logger.debug("MIME type detected from content: {}", detectedFromContent);
                return detectedFromContent;
            }
            
            // If client provided a valid MIME type and it matches the extension, trust it
            if (isSupportedVideoMimeType(clientProvidedMimeType) && 
                mimeTypeMatchesExtension(clientProvidedMimeType, filename)) {
                logger.debug("Using client-provided MIME type: {}", clientProvidedMimeType);
                return clientProvidedMimeType;
            }
            
            // Fall back to extension-based detection
            String detectedFromExtension = detectFromExtension(filename);
            logger.debug("MIME type detected from extension: {}", detectedFromExtension);
            return detectedFromExtension;
            
        } catch (IOException e) {
            logger.warn("Error reading file content for MIME detection, falling back to extension: {}", e.getMessage());
            // If we can't read the stream, fall back to extension
            return detectFromExtension(filename);
        }
    }
    
    @Override
    public boolean isSupportedVideoMimeType(String mimeType) {
        return VideoFormat.isSupportedMimeType(mimeType);
    }
    
    /**
     * Detect MIME type from magic bytes in the file content
     */
    private String detectFromMagicBytes(InputStream inputStream) throws IOException {
        // Mark the stream so we can reset it
        if (!inputStream.markSupported()) {
            logger.debug("InputStream does not support mark/reset, cannot detect from magic bytes");
            return null;
        }
        
        // Read first 32 bytes for magic byte detection
        inputStream.mark(32);
        byte[] header = new byte[32];
        int bytesRead = inputStream.read(header);
        inputStream.reset(); // Reset stream to beginning
        
        if (bytesRead < 8) {
            logger.debug("Not enough bytes read ({}) for magic byte detection", bytesRead);
            return null;
        }
        
        logger.debug("Read {} bytes for magic byte detection", bytesRead);
        
        // Check for MP4 signatures
        if (startsWithPattern(header, MP4_FTYP) || 
            (bytesRead >= 8 && Arrays.equals(Arrays.copyOfRange(header, 4, 8), MP4_FTYP_ALT))) {
            logger.debug("Detected MP4 format from magic bytes");
            return VideoFormat.MP4.getMimeType();
        }
        
        // Check for AVI signature
        if (startsWithPattern(header, AVI_RIFF) && bytesRead >= 12) {
            byte[] aviCheck = Arrays.copyOfRange(header, 8, 12);
            if (Arrays.equals(aviCheck, AVI_AVI)) {
                logger.debug("Detected AVI format from magic bytes");
                return VideoFormat.AVI.getMimeType();
            }
        }
        
        // Check for MOV/QuickTime
        if (bytesRead >= 10 && Arrays.equals(Arrays.copyOfRange(header, 4, 10), MOV_FTYP)) {
            logger.debug("Detected MOV format from magic bytes");
            return VideoFormat.MOV.getMimeType();
        }
        
        // Check for WMV/ASF
        if (startsWithPattern(header, WMV_ASF)) {
            logger.debug("Detected WMV format from magic bytes");
            return VideoFormat.WMV.getMimeType();
        }
        
        // Check for FLV
        if (startsWithPattern(header, FLV_HEADER)) {
            logger.debug("Detected FLV format from magic bytes");
            return VideoFormat.FLV.getMimeType();
        }
        
        // Check for WEBM
        if (startsWithPattern(header, WEBM_EBML)) {
            logger.debug("Detected WEBM format from magic bytes");
            return VideoFormat.WEBM.getMimeType();
        }
        
        logger.debug("No recognized magic bytes found");
        return null; // No recognized magic bytes
    }
    
    /**
     * Detect MIME type from file extension
     */
    private String detectFromExtension(String filename) {
        if (filename == null) {
            return null;
        }
        
        String extension = getFileExtension(filename);
        if (VideoFormat.isSupported(extension)) {
            try {
                return VideoFormat.fromExtension(extension).getMimeType();
            } catch (IllegalArgumentException e) {
                logger.debug("Error getting MIME type from extension {}: {}", extension, e.getMessage());
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Check if the MIME type matches the file extension
     */
    private boolean mimeTypeMatchesExtension(String mimeType, String filename) {
        if (mimeType == null || filename == null) {
            return false;
        }
        
        String extension = getFileExtension(filename);
        if (!VideoFormat.isSupported(extension)) {
            return false;
        }
        
        try {
            VideoFormat format = VideoFormat.fromExtension(extension);
            return format.getMimeType().equalsIgnoreCase(mimeType);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * Extract file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
    
    /**
     * Check if byte array starts with a specific pattern
     */
    private boolean startsWithPattern(byte[] data, byte[] pattern) {
        if (data.length < pattern.length) {
            return false;
        }
        
        for (int i = 0; i < pattern.length; i++) {
            if (data[i] != pattern[i]) {
                return false;
            }
        }
        return true;
    }
}
