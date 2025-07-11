package com.vclipper.processing.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.ports.*;
import com.vclipper.processing.application.usecases.ProcessVclippingResultUseCase;
import com.vclipper.processing.infrastructure.adapters.messaging.SQSMessageAdapter;
import com.vclipper.processing.infrastructure.adapters.notification.SNSNotificationAdapter;
import com.vclipper.processing.infrastructure.adapters.storage.S3FileStorageAdapter;
import com.vclipper.processing.infrastructure.adapters.user.MockUserServiceAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InfrastructureConfigurationTest {

    private InfrastructureConfiguration configuration;
    private S3Client mockS3Client;
    private S3Presigner mockS3Presigner;
    private SqsClient mockSqsClient;
    private SnsClient mockSnsClient;
    private ObjectMapper mockObjectMapper;
    private ProcessResultPort mockProcessResultPort;

    private static final String TEST_REGION = "us-east-1";
    private static final String TEST_BUCKET = "test-bucket";
    private static final String TEST_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue";
    private static final String TEST_TOPIC_ARN = "arn:aws:sns:us-east-1:123456789012:test-topic";

    @BeforeEach
    void setUp() {
        // Create mocks
        mockS3Client = mock(S3Client.class);
        mockS3Presigner = mock(S3Presigner.class);
        mockSqsClient = mock(SqsClient.class);
        mockSnsClient = mock(SnsClient.class);
        mockObjectMapper = mock(ObjectMapper.class);
        mockProcessResultPort = mock(ProcessResultPort.class);

        // Initialize configuration
        configuration = new InfrastructureConfiguration();
        
        // Set required properties using reflection
        ReflectionTestUtils.setField(configuration, "awsRegion", TEST_REGION);
        ReflectionTestUtils.setField(configuration, "s3BucketName", TEST_BUCKET);
        ReflectionTestUtils.setField(configuration, "sqsQueueUrl", TEST_QUEUE_URL);
        ReflectionTestUtils.setField(configuration, "snsTopicArn", TEST_TOPIC_ARN);
    }

    @Test
    void testFileStoragePortConfiguration() {
        // Act
        FileStoragePort fileStoragePort = configuration.fileStoragePort(mockS3Client, mockS3Presigner);
        
        // Assert
        assertNotNull(fileStoragePort, "FileStoragePort should not be null");
        assertTrue(fileStoragePort instanceof S3FileStorageAdapter, "FileStoragePort should be S3FileStorageAdapter");
        
        // Verify S3FileStorageAdapter is configured correctly
        S3FileStorageAdapter adapter = (S3FileStorageAdapter) fileStoragePort;
        assertEquals(mockS3Client, ReflectionTestUtils.getField(adapter, "s3Client"), "S3Client should be set correctly");
        assertEquals(mockS3Presigner, ReflectionTestUtils.getField(adapter, "s3Presigner"), "S3Presigner should be set correctly");
        assertEquals(TEST_BUCKET, ReflectionTestUtils.getField(adapter, "bucketName"), "Bucket name should be set correctly");
    }

    @Test
    void testMessageQueuePortConfiguration() {
        // Act
        MessageQueuePort messageQueuePort = configuration.messageQueuePort(mockSqsClient, mockObjectMapper);
        
        // Assert
        assertNotNull(messageQueuePort, "MessageQueuePort should not be null");
        assertTrue(messageQueuePort instanceof SQSMessageAdapter, "MessageQueuePort should be SQSMessageAdapter");
        
        // Verify SQSMessageAdapter is configured correctly
        SQSMessageAdapter adapter = (SQSMessageAdapter) messageQueuePort;
        assertEquals(mockSqsClient, ReflectionTestUtils.getField(adapter, "sqsClient"), "SQSClient should be set correctly");
        assertEquals(TEST_QUEUE_URL, ReflectionTestUtils.getField(adapter, "queueUrl"), "Queue URL should be set correctly");
        assertEquals(mockObjectMapper, ReflectionTestUtils.getField(adapter, "objectMapper"), "ObjectMapper should be set correctly");
    }

    @Test
    void testNotificationPortConfiguration() {
        // Act
        NotificationPort notificationPort = configuration.notificationPort(mockSnsClient, mockObjectMapper);
        
        // Assert
        assertNotNull(notificationPort, "NotificationPort should not be null");
        assertTrue(notificationPort instanceof SNSNotificationAdapter, "NotificationPort should be SNSNotificationAdapter");
        
        // Verify SNSNotificationAdapter is configured correctly
        SNSNotificationAdapter adapter = (SNSNotificationAdapter) notificationPort;
        assertEquals(mockSnsClient, ReflectionTestUtils.getField(adapter, "snsClient"), "SNSClient should be set correctly");
        assertEquals(TEST_TOPIC_ARN, ReflectionTestUtils.getField(adapter, "topicArn"), "Topic ARN should be set correctly");
        assertEquals(mockObjectMapper, ReflectionTestUtils.getField(adapter, "objectMapper"), "ObjectMapper should be set correctly");
    }

    @Test
    void testUserServicePortConfiguration() {
        // Act
        UserServicePort userServicePort = configuration.userServicePort();
        
        // Assert
        assertNotNull(userServicePort, "UserServicePort should not be null");
        assertTrue(userServicePort instanceof MockUserServiceAdapter, "UserServicePort should be MockUserServiceAdapter");
    }

    @Test
    void testProcessVclippingResultUseCaseConfiguration() {
        // Act
        ProcessVclippingResultUseCase useCase = configuration.processVclippingResultUseCase(
            mockProcessResultPort, mockObjectMapper);
        
        // Assert
        assertNotNull(useCase, "ProcessVclippingResultUseCase should not be null");
        
        // Verify dependencies are correctly injected
        assertEquals(mockProcessResultPort, ReflectionTestUtils.getField(useCase, "processResultPort"), 
            "ProcessResultPort should be set correctly");
        assertEquals(mockObjectMapper, ReflectionTestUtils.getField(useCase, "objectMapper"), 
            "ObjectMapper should be set correctly");
    }
    
    // AWS clients creation tests are not included as they would require mocking
    // the static builders, which is difficult and less valuable to test
}
