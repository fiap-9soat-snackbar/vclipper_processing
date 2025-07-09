package com.vclipper.processing.infrastructure.adapters.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vclipper.processing.application.ports.MessageQueuePort.VideoProcessingMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SQSMessageAdapterTest {

    private static final String QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue";
    private static final String FIFO_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/123456789012/test-queue.fifo";
    private static final String TEST_MESSAGE_ID = "test-message-id";
    
    @Mock
    private SqsClient sqsClient;
    
    @Mock
    private ObjectMapper objectMapper;
    
    private SQSMessageAdapter adapter;
    private VideoProcessingMessage testMessage;
    private String serializedMessage;
    
    @BeforeEach
    void setUp() {
        // Create test message
        testMessage = VideoProcessingMessage.withDefaults(
            "video-123",
            "user-456",
            "s3://bucket/video-123.mp4",
            "original.mp4",
            1024L,
            "video/mp4"
        );
        
        serializedMessage = "{\"messageId\":\"" + testMessage.messageId() + "\",\"videoId\":\"video-123\"}";
        
        // Standard adapter with standard queue
        adapter = new SQSMessageAdapter(sqsClient, QUEUE_URL, objectMapper);
    }
    
    @Test
    void sendProcessingMessage_StandardQueue_Success() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(testMessage)).thenReturn(serializedMessage);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId(TEST_MESSAGE_ID).build());
        
        // When
        String result = adapter.sendProcessingMessage(testMessage);
        
        // Then
        assertEquals(TEST_MESSAGE_ID, result);
        
        // Verify correct request was built
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(requestCaptor.capture());
        
        SendMessageRequest capturedRequest = requestCaptor.getValue();
        assertEquals(QUEUE_URL, capturedRequest.queueUrl());
        assertEquals(serializedMessage, capturedRequest.messageBody());
        
        // Verify FIFO attributes are not set
        assertNull(capturedRequest.messageGroupId());
        assertNull(capturedRequest.messageDeduplicationId());
    }
    
    @Test
    void sendProcessingMessage_FifoQueue_Success() throws JsonProcessingException {
        // Given
        adapter = new SQSMessageAdapter(sqsClient, FIFO_QUEUE_URL, objectMapper);
        when(objectMapper.writeValueAsString(testMessage)).thenReturn(serializedMessage);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId(TEST_MESSAGE_ID).build());
        
        // When
        String result = adapter.sendProcessingMessage(testMessage);
        
        // Then
        assertEquals(TEST_MESSAGE_ID, result);
        
        // Verify correct request was built
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(requestCaptor.capture());
        
        SendMessageRequest capturedRequest = requestCaptor.getValue();
        assertEquals(FIFO_QUEUE_URL, capturedRequest.queueUrl());
        assertEquals(serializedMessage, capturedRequest.messageBody());
        
        // Verify FIFO attributes are set
        assertEquals("video-processing", capturedRequest.messageGroupId());
        assertEquals(testMessage.videoId(), capturedRequest.messageDeduplicationId());
    }
    
    @Test
    void sendProcessingMessage_SerializationFailure() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(testMessage)).thenThrow(new JsonProcessingException("Serialization error") {});
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adapter.sendProcessingMessage(testMessage);
        });
        
        assertTrue(exception.getMessage().contains("Failed to serialize processing message"));
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }
    
    @Test
    void sendProcessingMessage_SqsException() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(testMessage)).thenReturn(serializedMessage);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(SqsException.builder().message("SQS error").build());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adapter.sendProcessingMessage(testMessage);
        });
        
        assertTrue(exception.getMessage().contains("Failed to send message to SQS"));
    }
    
    @Test
    void sendDelayedMessage_Success() throws JsonProcessingException {
        // Given
        int delaySeconds = 60;
        when(objectMapper.writeValueAsString(testMessage)).thenReturn(serializedMessage);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId(TEST_MESSAGE_ID).build());
        
        // When
        String result = adapter.sendDelayedMessage(testMessage, delaySeconds);
        
        // Then
        assertEquals(TEST_MESSAGE_ID, result);
        
        // Verify correct request was built
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(requestCaptor.capture());
        
        SendMessageRequest capturedRequest = requestCaptor.getValue();
        assertEquals(QUEUE_URL, capturedRequest.queueUrl());
        assertEquals(serializedMessage, capturedRequest.messageBody());
        assertEquals(delaySeconds, capturedRequest.delaySeconds());
    }
    
    @Test
    void sendDelayedMessage_FifoQueue_Success() throws JsonProcessingException {
        // Given
        adapter = new SQSMessageAdapter(sqsClient, FIFO_QUEUE_URL, objectMapper);
        int delaySeconds = 60;
        when(objectMapper.writeValueAsString(testMessage)).thenReturn(serializedMessage);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(SendMessageResponse.builder().messageId(TEST_MESSAGE_ID).build());
        
        // When
        String result = adapter.sendDelayedMessage(testMessage, delaySeconds);
        
        // Then
        assertEquals(TEST_MESSAGE_ID, result);
        
        // Verify correct request was built
        ArgumentCaptor<SendMessageRequest> requestCaptor = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(sqsClient).sendMessage(requestCaptor.capture());
        
        SendMessageRequest capturedRequest = requestCaptor.getValue();
        assertEquals(FIFO_QUEUE_URL, capturedRequest.queueUrl());
        assertEquals(serializedMessage, capturedRequest.messageBody());
        assertEquals(delaySeconds, capturedRequest.delaySeconds());
        
        // Verify FIFO attributes are set correctly for delayed messages
        assertEquals("video-processing-delayed", capturedRequest.messageGroupId());
        assertEquals(testMessage.videoId() + "-" + delaySeconds, capturedRequest.messageDeduplicationId());
    }
    
    @Test
    void sendDelayedMessage_SerializationFailure() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(testMessage)).thenThrow(new JsonProcessingException("Serialization error") {});
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adapter.sendDelayedMessage(testMessage, 60);
        });
        
        assertTrue(exception.getMessage().contains("Failed to serialize delayed processing message"));
        verify(sqsClient, never()).sendMessage(any(SendMessageRequest.class));
    }
    
    @Test
    void sendDelayedMessage_SqsException() throws JsonProcessingException {
        // Given
        when(objectMapper.writeValueAsString(testMessage)).thenReturn(serializedMessage);
        when(sqsClient.sendMessage(any(SendMessageRequest.class)))
            .thenThrow(SqsException.builder().message("SQS error").build());
        
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            adapter.sendDelayedMessage(testMessage, 60);
        });
        
        assertTrue(exception.getMessage().contains("Failed to send delayed message to SQS"));
    }
    
    @Test
    void isHealthy_Success() {
        // Given
        when(sqsClient.getQueueAttributes(any(Consumer.class)))
            .thenReturn(GetQueueAttributesResponse.builder().build());
        
        // When
        boolean result = adapter.isHealthy();
        
        // Then
        assertTrue(result);
        verify(sqsClient).getQueueAttributes(any(Consumer.class));
    }
    
    @Test
    void isHealthy_Failure() {
        // Given
        when(sqsClient.getQueueAttributes(any(Consumer.class)))
            .thenThrow(AwsServiceException.builder().message("Connection error").build());
        
        // When
        boolean result = adapter.isHealthy();
        
        // Then
        assertFalse(result);
        verify(sqsClient).getQueueAttributes(any(Consumer.class));
    }
}
