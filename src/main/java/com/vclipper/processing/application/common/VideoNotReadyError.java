package com.vclipper.processing.application.common;

import com.vclipper.processing.domain.entity.ProcessingStatus;

/**
 * Business error for when a video is not ready for a requested operation
 * This represents expected business state, not an exceptional condition
 */
public record VideoNotReadyError(
    String videoId,
    ProcessingStatus currentStatus,
    String operation,
    String message
) {
    
    public static VideoNotReadyError forDownload(String videoId, ProcessingStatus currentStatus) {
        return new VideoNotReadyError(
            videoId,
            currentStatus,
            "download",
            String.format("Video %s is not ready for download. Current status: %s", 
                videoId, currentStatus.value())
        );
    }
    
    public static VideoNotReadyError forOperation(String videoId, ProcessingStatus currentStatus, String operation) {
        return new VideoNotReadyError(
            videoId,
            currentStatus,
            operation,
            String.format("Video %s is not ready for %s. Current status: %s", 
                videoId, operation, currentStatus.value())
        );
    }
}
