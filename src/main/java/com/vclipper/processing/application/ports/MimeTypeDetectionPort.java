package com.vclipper.processing.application.ports;

import java.io.InputStream;

/**
 * Port for MIME type detection from file content
 * Provides reliable MIME type detection using file signatures (magic bytes)
 * rather than relying solely on client-provided content types
 */
public interface MimeTypeDetectionPort {
    
    /**
     * Detect MIME type from file content and metadata
     * 
     * @param inputStream The file content stream
     * @param filename The original filename (for extension fallback)
     * @param clientProvidedMimeType The MIME type provided by the client
     * @return The detected MIME type, or null if cannot be determined
     */
    String detectMimeType(InputStream inputStream, String filename, String clientProvidedMimeType);
    
    /**
     * Check if the detected MIME type is supported for video processing
     * 
     * @param mimeType The MIME type to validate
     * @return true if the MIME type is supported for video processing
     */
    boolean isSupportedVideoMimeType(String mimeType);
}
