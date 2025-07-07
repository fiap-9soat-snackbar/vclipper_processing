package com.vclipper.processing.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.ports.*;
import com.vclipper.processing.application.usecases.ProcessVclippingResultUseCase;
import com.vclipper.processing.infrastructure.adapters.messaging.SQSMessageAdapter;
import com.vclipper.processing.infrastructure.adapters.notification.SNSNotificationAdapter;
import com.vclipper.processing.infrastructure.adapters.storage.S3FileStorageAdapter;
import com.vclipper.processing.infrastructure.adapters.user.MockUserServiceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Infrastructure configuration for wiring adapters with ports
 * This is where we connect the clean architecture layers with Spring dependency injection
 */
@Configuration
public class InfrastructureConfiguration {
    
    private static final Logger logger = LoggerFactory.getLogger(InfrastructureConfiguration.class);
    
    @Value("${AWS_REGION}")
    private String awsRegion;
    
    @Value("${AWS_S3_BUCKET}")
    private String s3BucketName;
    
    @Value("${AWS_SQS_PROCESSING_QUEUE_URL}")
    private String sqsQueueUrl;
    
    @Value("${AWS_SNS_NOTIFICATION_TOPIC_ARN}")
    private String snsTopicArn;
    
    // ========== AWS Client Configuration ==========
    
    /**
     * Configure S3 client for file storage operations
     */
    @Bean
    public S3Client s3Client() {
        logger.info("🔧 Configuring S3 client for region: {}", awsRegion);
        return S3Client.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
    
    /**
     * Configure S3 presigner for generating download URLs
     */
    @Bean
    public S3Presigner s3Presigner() {
        logger.info("🔧 Configuring S3 presigner for region: {}", awsRegion);
        return S3Presigner.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
    
    /**
     * Configure SQS client for message queue operations
     */
    @Bean
    public SqsClient sqsClient() {
        logger.info("🔧 Configuring SQS client for region: {}", awsRegion);
        return SqsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
    
    /**
     * Configure SNS client for notification operations
     */
    @Bean
    public SnsClient snsClient() {
        logger.info("🔧 Configuring SNS client for region: {}", awsRegion);
        return SnsClient.builder()
            .region(Region.of(awsRegion))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    }
    
    // ========== Port Implementation Configuration ==========
    
    /**
     * Create FileStoragePort implementation using real S3 adapter
     */
    @Bean
    public FileStoragePort fileStoragePort(S3Client s3Client, S3Presigner s3Presigner) {
        logger.info("✅ Configuring FileStoragePort with real S3 integration");
        return new S3FileStorageAdapter(s3Client, s3Presigner, s3BucketName);
    }
    
    /**
     * Create MessageQueuePort implementation using real SQS adapter
     */
    @Bean
    public MessageQueuePort messageQueuePort(SqsClient sqsClient, ObjectMapper objectMapper) {
        logger.info("✅ Configuring MessageQueuePort with real SQS integration - Queue: {}", sqsQueueUrl);
        return new SQSMessageAdapter(sqsClient, sqsQueueUrl, objectMapper);
    }
    
    /**
     * Create NotificationPort implementation using real SNS adapter
     */
    @Bean
    public NotificationPort notificationPort(SnsClient snsClient, ObjectMapper objectMapper) {
        logger.info("✅ Configuring NotificationPort with real SNS integration - Topic: {}", snsTopicArn);
        return new SNSNotificationAdapter(snsClient, snsTopicArn, objectMapper);
    }
    
    /**
     * Create UserServicePort implementation using Mock User Service adapter
     */
    @Bean
    public UserServicePort userServicePort() {
        logger.info("⚠️ Configuring UserServicePort with Mock adapter (will be replaced with real integration)");
        return new MockUserServiceAdapter();
    }
    
    // ========== SQS Result Processing Configuration ==========
    
    /**
     * Configure ProcessVclippingResultUseCase for handling vclipping service results
     */
    @Bean
    public ProcessVclippingResultUseCase processVclippingResultUseCase(
            ProcessResultPort processResultPort, 
            ObjectMapper objectMapper) {
        logger.info("🔧 Configuring ProcessVclippingResultUseCase for SQS result processing");
        return new ProcessVclippingResultUseCase(processResultPort, objectMapper);
    }
}
