package com.vclipper.processing.infrastructure.controllers.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for video upload requests
 * Handles multipart file upload with validation
 * User ID is now passed via X-User-Id header instead of form data
 */
public record VideoUploadRequest(
    @NotNull(message = "Video file is required")
    MultipartFile file
) {
    /**
     * Validate file is not empty
     */
    public boolean isFileValid() {
        return file != null && !file.isEmpty() && file.getOriginalFilename() != null;
    }
    
    /**
     * Get file size in bytes
     */
    public long getFileSizeBytes() {
        return file != null ? file.getSize() : 0;
    }
    
    /**
     * Get original filename
     */
    public String getOriginalFilename() {
        return file != null ? file.getOriginalFilename() : "";
    }
    
    /**
     * Get content type
     */
    public String getContentType() {
        return file != null ? file.getContentType() : "";
    }
}
