package com.vclipper.processing.infrastructure.config;

import com.vclipper.processing.application.ports.*;
import com.vclipper.processing.application.usecases.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for UseCaseConfiguration
 * Verifies that all use cases are properly wired with their dependencies
 */
@ExtendWith(MockitoExtension.class)
class UseCaseConfigurationTest {

    @Mock
    private VideoRepositoryPort videoRepository;

    @Mock
    private FileStoragePort fileStorage;

    @Mock
    private MessageQueuePort messageQueue;

    @Mock
    private NotificationPort notification;

    @Mock
    private UserServicePort userService;

    @Mock
    private MimeTypeDetectionPort mimeTypeDetection;

    private UseCaseConfiguration configuration;
    private ProcessingProperties properties;

    @BeforeEach
    void setUp() {
        // Create test properties
        ProcessingProperties.Video videoProps = new ProcessingProperties.Video(100_000_000L, "mp4,avi,mov");
        ProcessingProperties.Download downloadProps = new ProcessingProperties.Download(60);
        ProcessingProperties.Retry retryProps = new ProcessingProperties.Retry(3, 10, 300);
        properties = new ProcessingProperties(videoProps, downloadProps, retryProps);

        // Initialize configuration
        configuration = new UseCaseConfiguration();
    }

    @Test
    void shouldCreateSubmitVideoProcessingUseCase() {
        // Create UpdateVideoStatusUseCase first since it's a dependency
        UpdateVideoStatusUseCase updateVideoStatusUseCase =
            configuration.updateVideoStatusUseCase(videoRepository, notification);

        // Create and test SubmitVideoProcessingUseCase
        SubmitVideoProcessingUseCase useCase = configuration.submitVideoProcessingUseCase(
            videoRepository, fileStorage, messageQueue, notification,
            userService, mimeTypeDetection, updateVideoStatusUseCase, properties);

        assertNotNull(useCase, "SubmitVideoProcessingUseCase should be created");
    }

    @Test
    void shouldCreateGetProcessingStatusUseCase() {
        GetProcessingStatusUseCase useCase =
            configuration.getProcessingStatusUseCase(videoRepository);

        assertNotNull(useCase, "GetProcessingStatusUseCase should be created");
    }

    @Test
    void shouldCreateListUserVideosUseCase() {
        ListUserVideosUseCase useCase =
            configuration.listUserVideosUseCase(videoRepository);

        assertNotNull(useCase, "ListUserVideosUseCase should be created");
    }

    @Test
    void shouldCreateGetVideoDownloadUrlUseCase() {
        GetVideoDownloadUrlUseCase useCase =
            configuration.getVideoDownloadUrlUseCase(
                videoRepository, fileStorage, properties);

        assertNotNull(useCase, "GetVideoDownloadUrlUseCase should be created");
    }

    @Test
    void shouldCreateUpdateVideoStatusUseCase() {
        UpdateVideoStatusUseCase useCase =
            configuration.updateVideoStatusUseCase(videoRepository, notification);

        assertNotNull(useCase, "UpdateVideoStatusUseCase should be created");
    }
}
