package com.vclipper.processing.infrastructure.controllers;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.common.Result;
import com.vclipper.processing.application.common.VideoNotReadyError;
import com.vclipper.processing.application.common.VideoUploadError;
import com.vclipper.processing.application.usecases.*;
import com.vclipper.processing.domain.entity.ProcessingStatus;
import com.vclipper.processing.domain.exceptions.VideoNotFoundException;
import com.vclipper.processing.infrastructure.controllers.dto.VideoStatusUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.view.json.MappingJackson2JsonView;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class VideoProcessingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SubmitVideoProcessingUseCase submitVideoProcessingUseCase;

    @Mock
    private GetProcessingStatusUseCase getProcessingStatusUseCase;

    @Mock
    private GetVideoDownloadUrlUseCase getVideoDownloadUrlUseCase;

    @Mock
    private ListUserVideosUseCase listUserVideosUseCase;

    @Mock
    private UpdateVideoStatusUseCase updateVideoStatusUseCase;

    @InjectMocks
    private VideoProcessingController videoProcessingController;

    private static final String USER_ID = "user123";
    private static final String VIDEO_ID = "video123";
    private static final String X_USER_ID_HEADER = "X-User-Id";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        // Add mix-in to handle ProcessingStatus serialization
        objectMapper.addMixIn(ProcessingStatus.class, ProcessingStatusMixin.class);

        // Add GlobalExceptionHandler to handle exceptions properly
        mockMvc = MockMvcBuilders
                .standaloneSetup(videoProcessingController)
                .setControllerAdvice(new GlobalExceptionHandler()) // Add exception handler
                .setViewResolvers((viewName, locale) -> new MappingJackson2JsonView())
                .build();
    }

    // Add this inner class to handle ProcessingStatus serialization in tests
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private abstract static class ProcessingStatusMixin {
    }

    @Test
    void uploadVideo_Success() throws Exception {
        // Arrange
        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );

        SubmitVideoProcessingUseCase.VideoProcessingResponse successResponse =
            new SubmitVideoProcessingUseCase.VideoProcessingResponse(
                VIDEO_ID,
                ProcessingStatus.PENDING,
                "Video uploaded successfully",
                true
            );

        when(submitVideoProcessingUseCase.execute(any()))
                .thenReturn(Result.success(successResponse));

        // Act & Assert
        mockMvc.perform(multipart("/api/videos/upload")
                .file(videoFile)
                .header(X_USER_ID_HEADER, USER_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.videoId").value(VIDEO_ID))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.originalFilename").value("test-video.mp4"));
    }

    @Test
    void uploadVideo_InvalidFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "",
                "video/mp4",
                new byte[0]
        );

        // Act & Assert
        mockMvc.perform(multipart("/api/videos/upload")
                .file(emptyFile)
                .header(X_USER_ID_HEADER, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void uploadVideo_BusinessValidationFailure() throws Exception {
        // Arrange
        MockMultipartFile videoFile = new MockMultipartFile(
                "file",
                "test-video.mp4",
                "video/mp4",
                "test video content".getBytes()
        );

        VideoUploadError error = VideoUploadError.fileTooLarge(1000000, 500000);
        when(submitVideoProcessingUseCase.execute(any()))
                .thenReturn(Result.failure(error));

        // Act & Assert
        mockMvc.perform(multipart("/api/videos/upload")
                .file(videoFile)
                .header(X_USER_ID_HEADER, USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(error.message()));
    }

    @Test
    void getProcessingStatus_Success() throws Exception {
        // Arrange
        GetProcessingStatusUseCase.ProcessingStatusResponse response =
            new GetProcessingStatusUseCase.ProcessingStatusResponse(
                VIDEO_ID,
                USER_ID,
                ProcessingStatus.PROCESSING,
                "test-video.mp4",
                10.5,
                LocalDateTime.now(),
                LocalDateTime.now(),
                null,
                false,
                false
            );

        when(getProcessingStatusUseCase.execute(VIDEO_ID, USER_ID))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(get("/api/videos/{videoId}/status", VIDEO_ID)
                .header(X_USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value(VIDEO_ID));
    }

    @Test
    void getProcessingStatus_VideoNotFound() throws Exception {
        // Arrange
        when(getProcessingStatusUseCase.execute(VIDEO_ID, USER_ID))
                .thenThrow(new VideoNotFoundException(VIDEO_ID));

        // Act & Assert
        mockMvc.perform(get("/api/videos/{videoId}/status", VIDEO_ID)
                .header(X_USER_ID_HEADER, USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("VIDEO_NOT_FOUND"))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void listUserVideos_Success() throws Exception {
        // Arrange
        List<ListUserVideosUseCase.VideoSummary> videos = List.of(
            new ListUserVideosUseCase.VideoSummary(
                VIDEO_ID,
                "test-video.mp4",
                ProcessingStatus.COMPLETED,
                10.5,
                LocalDateTime.now(),
                LocalDateTime.now(),
                true,
                false,
                null
            ),
            new ListUserVideosUseCase.VideoSummary(
                "video456",
                "another-video.mp4",
                ProcessingStatus.PROCESSING,
                15.2,
                LocalDateTime.now(),
                LocalDateTime.now(),
                false,
                false,
                null
            )
        );

        when(listUserVideosUseCase.execute(USER_ID)).thenReturn(videos);

        // Act & Assert
        mockMvc.perform(get("/api/videos")
                .header(X_USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.videos.length()").value(2))
                .andExpect(jsonPath("$.videos[0].videoId").value(VIDEO_ID))
                .andExpect(jsonPath("$.videos[1].videoId").value("video456"));
    }

    @Test
    void getVideoDownloadUrl_Success() throws Exception {
        // Arrange
        GetVideoDownloadUrlUseCase.DownloadUrlResponse response =
            new GetVideoDownloadUrlUseCase.DownloadUrlResponse(
                VIDEO_ID,
                "test-video.mp4",
                "https://example.com/download/123",
                30
            );

        when(getVideoDownloadUrlUseCase.execute(VIDEO_ID, USER_ID))
                .thenReturn(Result.success(response));

        // Act & Assert
        mockMvc.perform(get("/api/videos/{videoId}/download", VIDEO_ID)
                .header(X_USER_ID_HEADER, USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value(VIDEO_ID))
                .andExpect(jsonPath("$.downloadUrl").value("https://example.com/download/123"))
                .andExpect(jsonPath("$.expirationMinutes").value(30));
    }

    @Test
    void getVideoDownloadUrl_VideoNotReady() throws Exception {
        // Arrange
        VideoNotReadyError error = VideoNotReadyError.forDownload(
            VIDEO_ID,
            ProcessingStatus.PROCESSING
        );

        when(getVideoDownloadUrlUseCase.execute(VIDEO_ID, USER_ID))
                .thenReturn(Result.failure(error));

        // Act & Assert
        mockMvc.perform(get("/api/videos/{videoId}/download", VIDEO_ID)
                .header(X_USER_ID_HEADER, USER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.videoId").value(VIDEO_ID))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void updateVideoStatus_Success() throws Exception {
        // Arrange
        VideoStatusUpdateRequest request = new VideoStatusUpdateRequest(
            ProcessingStatus.COMPLETED,
            "s3://bucket/processed-file.mp4",
            LocalDateTime.now(),
            null
        );

        UpdateVideoStatusUseCase.VideoStatusUpdateResponse response =
            new UpdateVideoStatusUseCase.VideoStatusUpdateResponse(
                VIDEO_ID,
                USER_ID,
                ProcessingStatus.PROCESSING,
                ProcessingStatus.COMPLETED,
                "s3://bucket/processed-file.mp4",
                LocalDateTime.now()
            );

        when(updateVideoStatusUseCase.execute(
                eq(VIDEO_ID),
                eq(ProcessingStatus.COMPLETED),
                eq("s3://bucket/processed-file.mp4"),
                eq(null)))
                .thenReturn(response);

        // Act & Assert
        mockMvc.perform(put("/api/videos/{videoId}/status", VIDEO_ID)
                .header(X_USER_ID_HEADER, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.videoId").value(VIDEO_ID))
                .andExpect(jsonPath("$.processedFileS3Key").value("s3://bucket/processed-file.mp4"));
    }

    @Test
    void updateVideoStatus_InvalidRequest() throws Exception {
        // Arrange
        VideoStatusUpdateRequest request = new VideoStatusUpdateRequest(
            ProcessingStatus.COMPLETED,
            null, // Missing required S3 key for COMPLETED status
            LocalDateTime.now(),
            null
        );

        // Act & Assert
        mockMvc.perform(put("/api/videos/{videoId}/status", VIDEO_ID)
                .header(X_USER_ID_HEADER, USER_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    @Test
    void missingUserIdHeader_ShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/videos"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("MISSING_HEADER"));
    }
}
