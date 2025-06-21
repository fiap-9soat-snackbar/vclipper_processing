package com.vclipper.processing.infrastructure.adapters.messaging;

import com.vclipper.processing.application.ports.MessageQueuePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Mock implementation of SQS message queue for development and testing
 * Simulates SQS operations with console logging and delayed message simulation
 */
@Component
public class MockSQSMessageAdapter implements MessageQueuePort {
    
    private static final Logger logger = LoggerFactory.getLogger(MockSQSMessageAdapter.class);
    
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<String, VideoProcessingMessage> messageStore = new ConcurrentHashMap<>();
    
    @Override
    public String sendProcessingMessage(VideoProcessingMessage processingMessage) {
        String messageId = UUID.randomUUID().toString();
        
        logger.info("📨 MOCK SQS: Sending processing message");
        logger.info("   🆔 Message ID: {}", messageId);
        logger.info("   🎬 Video ID: {}", processingMessage.videoId());
        logger.info("   👤 User ID: {}", processingMessage.userId());
        logger.info("   📁 Filename: {}", processingMessage.originalFilename());
        logger.info("   📊 File Size: {} bytes ({} MB)", 
            processingMessage.fileSizeBytes(), 
            String.format("%.2f", processingMessage.fileSizeBytes() / (1024.0 * 1024.0)));
        logger.info("   🗂️  Storage Reference: {}", processingMessage.storageReference());
        logger.info("   🏷️  Content Type: {}", processingMessage.contentType());
        logger.info("   📤 Queue: vclipper-processing-queue");
        logger.info("   ⏰ Sent at: {}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        
        // Store message for tracking
        messageStore.put(messageId, processingMessage);
        
        // Simulate message being picked up by processing service after a short delay
        scheduler.schedule(() -> {
            logger.info("🔄 MOCK SQS: Message {} would be consumed by video processing service", messageId);
            logger.info("   🎬 Processing would start for video: {}", processingMessage.videoId());
            messageStore.remove(messageId);
        }, 2, TimeUnit.SECONDS);
        
        logger.info("   ✅ Message successfully sent to SQS");
        
        return messageId;
    }
    
    @Override
    public String sendDelayedMessage(VideoProcessingMessage processingMessage, int delaySeconds) {
        String messageId = UUID.randomUUID().toString();
        
        logger.info("⏰ MOCK SQS: Sending delayed processing message");
        logger.info("   🆔 Message ID: {}", messageId);
        logger.info("   🎬 Video ID: {}", processingMessage.videoId());
        logger.info("   👤 User ID: {}", processingMessage.userId());
        logger.info("   📁 Filename: {}", processingMessage.originalFilename());
        logger.info("   ⏳ Delay: {} seconds", delaySeconds);
        logger.info("   📅 Will be available at: {}", 
            LocalDateTime.now().plusSeconds(delaySeconds).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        logger.info("   💡 Reason: Retry mechanism / Budget management / Throttling");
        
        // Store message for tracking
        messageStore.put(messageId, processingMessage);
        
        // Simulate delayed message delivery
        scheduler.schedule(() -> {
            logger.info("📨 MOCK SQS: Delayed message {} is now available for processing", messageId);
            logger.info("   🎬 Video: {} can now be processed", processingMessage.videoId());
            
            // Simulate processing service picking up the message after delay
            scheduler.schedule(() -> {
                logger.info("🔄 MOCK SQS: Delayed message {} consumed by processing service", messageId);
                messageStore.remove(messageId);
            }, 2, TimeUnit.SECONDS);
            
        }, delaySeconds, TimeUnit.SECONDS);
        
        logger.info("   ✅ Delayed message successfully queued");
        
        return messageId;
    }
    
    @Override
    public boolean isHealthy() {
        logger.debug("💚 MOCK SQS: Health check - Always healthy in mock mode");
        return true;
    }
    
    /**
     * Get current queue size (for monitoring/debugging)
     */
    public int getQueueSize() {
        int size = messageStore.size();
        logger.debug("📊 MOCK SQS: Current queue size: {} messages", size);
        return size;
    }
    
    /**
     * Shutdown scheduler when application stops
     */
    public void shutdown() {
        logger.info("🛑 MOCK SQS: Shutting down scheduler");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
