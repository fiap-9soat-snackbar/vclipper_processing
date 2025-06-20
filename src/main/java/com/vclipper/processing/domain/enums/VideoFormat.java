package com.vclipper.processing.domain.enums;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Supported video formats for processing
 */
public enum VideoFormat {
    MP4("mp4", "video/mp4"),
    AVI("avi", "video/x-msvideo"),
    MOV("mov", "video/quicktime"),
    WMV("wmv", "video/x-ms-wmv"),
    FLV("flv", "video/x-flv"),
    WEBM("webm", "video/webm");
    
    private final String extension;
    private final String mimeType;
    
    VideoFormat(String extension, String mimeType) {
        this.extension = extension;
        this.mimeType = mimeType;
    }
    
    public String getExtension() {
        return extension;
    }
    
    public String getMimeType() {
        return mimeType;
    }
    
    /**
     * Get all supported file extensions
     */
    public static Set<String> getSupportedExtensions() {
        return Arrays.stream(values())
                .map(VideoFormat::getExtension)
                .collect(Collectors.toSet());
    }
    
    /**
     * Get all supported MIME types
     */
    public static Set<String> getSupportedMimeTypes() {
        return Arrays.stream(values())
                .map(VideoFormat::getMimeType)
                .collect(Collectors.toSet());
    }
    
    /**
     * Check if file extension is supported
     */
    public static boolean isSupported(String extension) {
        if (extension == null) return false;
        String cleanExtension = extension.toLowerCase().replaceFirst("^\\.", "");
        return getSupportedExtensions().contains(cleanExtension);
    }
    
    /**
     * Check if MIME type is supported
     */
    public static boolean isSupportedMimeType(String mimeType) {
        return mimeType != null && getSupportedMimeTypes().contains(mimeType.toLowerCase());
    }
    
    /**
     * Get VideoFormat from file extension
     */
    public static VideoFormat fromExtension(String extension) {
        if (extension == null) {
            throw new IllegalArgumentException("Extension cannot be null");
        }
        
        String cleanExtension = extension.toLowerCase().replaceFirst("^\\.", "");
        return Arrays.stream(values())
                .filter(format -> format.getExtension().equals(cleanExtension))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported video format: " + extension));
    }
}
