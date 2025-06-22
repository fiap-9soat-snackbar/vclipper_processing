package com.vclipper.processing.infrastructure.config;

import com.vclipper.processing.application.ports.*;
import com.vclipper.processing.infrastructure.adapters.messaging.MockSQSMessageAdapter;
import com.vclipper.processing.infrastructure.adapters.notification.MockSNSNotificationAdapter;
import com.vclipper.processing.infrastructure.adapters.storage.MockS3FileStorageAdapter;
import com.vclipper.processing.infrastructure.adapters.user.MockUserServiceAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure configuration for wiring adapters with ports
 * This is where we connect the clean architecture layers with Spring dependency injection
 */
@Configuration
public class InfrastructureConfiguration {
    
    /**
     * Create FileStoragePort implementation using Mock S3 adapter
     */
    @Bean
    public FileStoragePort fileStoragePort() {
        return new MockS3FileStorageAdapter();
    }
    
    /**
     * Create MessageQueuePort implementation using Mock SQS adapter
     */
    @Bean
    public MessageQueuePort messageQueuePort() {
        return new MockSQSMessageAdapter();
    }
    
    /**
     * Create NotificationPort implementation using Mock SNS adapter
     */
    @Bean
    public NotificationPort notificationPort() {
        return new MockSNSNotificationAdapter();
    }
    
    /**
     * Create UserServicePort implementation using Mock User Service adapter
     */
    @Bean
    public UserServicePort userServicePort() {
        return new MockUserServiceAdapter();
    }
}
