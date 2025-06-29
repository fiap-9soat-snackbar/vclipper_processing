package com.vclipper.processing.application.common;

/**
 * Business error for video upload validation failures
 * This represents expected business validation, not an exceptional condition
 */
public record VideoUploadError(
    String errorCode,
    String message,
    String field
) {
    
    public static VideoUploadError invalidFormat(String format) {
        return new VideoUploadError(
            "INVALID_VIDEO_FORMAT",
            String.format("Invalid or unsupported video format: %s", format),
            "file"
        );
    }
    
    public static VideoUploadError fileTooLarge(long actualSize, long maxSize) {
        return new VideoUploadError(
            "FILE_TOO_LARGE",
            String.format("File size %d bytes exceeds maximum allowed size %d bytes", actualSize, maxSize),
            "file"
        );
    }
    
    public static VideoUploadError emptyFile() {
        return new VideoUploadError(
            "EMPTY_FILE",
            "File is empty or has no content",
            "file"
        );
    }
    
    public static VideoUploadError invalidUser(String userId) {
        return new VideoUploadError(
            "INVALID_USER",
            String.format("User %s is not found or inactive", userId),
            "userId"
        );
    }
}
