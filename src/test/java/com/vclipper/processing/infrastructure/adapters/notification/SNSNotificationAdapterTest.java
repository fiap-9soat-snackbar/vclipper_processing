package com.vclipper.processing.infrastructure.adapters.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.ports.NotificationPort;
import com.vclipper.processing.domain.entity.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesRequest;
import software.amazon.awssdk.services.sns.model.GetTopicAttributesResponse;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;
import software.amazon.awssdk.services.sns.model.SnsException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SNSNotificationAdapterTest {

    @Mock
    private SnsClient snsClient;

    @Mock
    private ObjectMapper objectMapper;

    private SNSNotificationAdapter notificationAdapter;
    private final String topicArn = "arn:aws:sns:us-east-1:123456789012:MyTopic";
    private final String mockMessageId = "mock-message-id-12345";
    private final String userId = "user-123";

    @BeforeEach
    void setUp() {
        notificationAdapter = new SNSNotificationAdapter(snsClient, topicArn, objectMapper);
    }

    @Test
    void sendNotification_Success() throws JsonProcessingException {
        // Arrange
        NotificationType notificationType = mock(NotificationType.class);
        when(notificationType.type()).thenReturn("PROCESSING_COMPLETE");
        when(notificationType.subject()).thenReturn("Processing Complete");

        // Fix: Use eq() for exact matches and anyString() for more flexible matching
        when(notificationType.formatMessage(
                eq("test.mp4"),
                eq("https://example.com/video.mp4"),
                eq(null)))
                .thenReturn("Your video test.mp4 has been processed successfully");

        NotificationPort.NotificationData notificationData =
                NotificationPort.NotificationData.forCompletion("test.mp4", "https://example.com/video.mp4");

        String serializedMessage = "{\"userId\":\"user-123\",\"type\":\"PROCESSING_COMPLETE\"}";
        when(objectMapper.writeValueAsString(any(SNSNotificationAdapter.NotificationMessage.class)))
                .thenReturn(serializedMessage);

        PublishResponse publishResponse = mock(PublishResponse.class);
        when(publishResponse.messageId()).thenReturn(mockMessageId);
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);

        // Act
        String result = notificationAdapter.sendNotification(userId, notificationType, notificationData);

        // Assert
        assertEquals(mockMessageId, result);

        ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(requestCaptor.capture());

        PublishRequest capturedRequest = requestCaptor.getValue();
        assertEquals(topicArn, capturedRequest.topicArn());
        assertEquals("Processing Complete", capturedRequest.subject());
        assertEquals(serializedMessage, capturedRequest.message());

        ArgumentCaptor<SNSNotificationAdapter.NotificationMessage> messageCaptor =
                ArgumentCaptor.forClass(SNSNotificationAdapter.NotificationMessage.class);
        verify(objectMapper).writeValueAsString(messageCaptor.capture());

        SNSNotificationAdapter.NotificationMessage capturedMessage = messageCaptor.getValue();
        assertEquals(userId, capturedMessage.userId());
        assertEquals("PROCESSING_COMPLETE", capturedMessage.type());
        assertEquals("Processing Complete", capturedMessage.subject());
        assertEquals("test.mp4", capturedMessage.videoName());
        assertEquals("https://example.com/video.mp4", capturedMessage.downloadUrl());
        assertNull(capturedMessage.errorMessage());
    }

    @Test
    void sendNotification_JsonProcessingException_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        NotificationType notificationType = mock(NotificationType.class);
        when(notificationType.type()).thenReturn("PROCESSING_COMPLETE");
        when(notificationType.subject()).thenReturn("Processing Complete");

        NotificationPort.NotificationData notificationData =
                NotificationPort.NotificationData.forCompletion("test.mp4", "https://example.com/video.mp4");

        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test error") {});

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                notificationAdapter.sendNotification(userId, notificationType, notificationData));

        assertEquals("Failed to serialize notification message", exception.getMessage());
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void sendNotification_SnsException_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        NotificationType notificationType = mock(NotificationType.class);
        when(notificationType.type()).thenReturn("PROCESSING_COMPLETE");
        when(notificationType.subject()).thenReturn("Processing Complete");

        NotificationPort.NotificationData notificationData =
                NotificationPort.NotificationData.forCompletion("test.mp4", "https://example.com/video.mp4");

        String serializedMessage = "{\"userId\":\"user-123\",\"type\":\"PROCESSING_COMPLETE\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(serializedMessage);

        when(snsClient.publish(any(PublishRequest.class))).thenThrow(
                SnsException.builder().message("SNS service error").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                notificationAdapter.sendNotification(userId, notificationType, notificationData));

        assertEquals("Failed to send notification via SNS", exception.getMessage());
    }

    @Test
    void sendCustomNotification_Success() throws JsonProcessingException {
        // Arrange
        String subject = "Custom Notification";
        String message = "This is a custom message";

        String serializedMessage = "{\"userId\":\"user-123\",\"type\":\"CUSTOM\"}";
        when(objectMapper.writeValueAsString(any(SNSNotificationAdapter.NotificationMessage.class)))
                .thenReturn(serializedMessage);

        PublishResponse publishResponse = mock(PublishResponse.class);
        when(publishResponse.messageId()).thenReturn(mockMessageId);
        when(snsClient.publish(any(PublishRequest.class))).thenReturn(publishResponse);

        // Act
        String result = notificationAdapter.sendCustomNotification(userId, subject, message);

        // Assert
        assertEquals(mockMessageId, result);

        ArgumentCaptor<PublishRequest> requestCaptor = ArgumentCaptor.forClass(PublishRequest.class);
        verify(snsClient).publish(requestCaptor.capture());

        PublishRequest capturedRequest = requestCaptor.getValue();
        assertEquals(topicArn, capturedRequest.topicArn());
        assertEquals(subject, capturedRequest.subject());
        assertEquals(serializedMessage, capturedRequest.message());

        ArgumentCaptor<SNSNotificationAdapter.NotificationMessage> messageCaptor =
                ArgumentCaptor.forClass(SNSNotificationAdapter.NotificationMessage.class);
        verify(objectMapper).writeValueAsString(messageCaptor.capture());

        SNSNotificationAdapter.NotificationMessage capturedMessage = messageCaptor.getValue();
        assertEquals(userId, capturedMessage.userId());
        assertEquals("CUSTOM", capturedMessage.type());
        assertEquals(subject, capturedMessage.subject());
        assertEquals(message, capturedMessage.message());
        assertNull(capturedMessage.videoName());
        assertNull(capturedMessage.downloadUrl());
        assertNull(capturedMessage.errorMessage());
    }

    @Test
    void sendCustomNotification_JsonProcessingException_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        String subject = "Custom Notification";
        String message = "This is a custom message";

        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("Test error") {});

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                notificationAdapter.sendCustomNotification(userId, subject, message));

        assertEquals("Failed to serialize custom notification message", exception.getMessage());
        verify(snsClient, never()).publish(any(PublishRequest.class));
    }

    @Test
    void sendCustomNotification_SnsException_ThrowsRuntimeException() throws JsonProcessingException {
        // Arrange
        String subject = "Custom Notification";
        String message = "This is a custom message";

        String serializedMessage = "{\"userId\":\"user-123\",\"type\":\"CUSTOM\"}";
        when(objectMapper.writeValueAsString(any())).thenReturn(serializedMessage);

        when(snsClient.publish(any(PublishRequest.class))).thenThrow(
                SnsException.builder().message("SNS service error").build());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                notificationAdapter.sendCustomNotification(userId, subject, message));

        assertEquals("Failed to send custom notification via SNS", exception.getMessage());
    }


    @Test
    void isHealthy_Exception_ReturnsFalse() {
        // Arrange
        when(snsClient.getTopicAttributes(any(GetTopicAttributesRequest.class)))
                .thenThrow(SnsException.builder().message("Service unavailable").build());

        // Act
        boolean result = notificationAdapter.isHealthy();

        // Assert
        assertFalse(result);
    }
}
