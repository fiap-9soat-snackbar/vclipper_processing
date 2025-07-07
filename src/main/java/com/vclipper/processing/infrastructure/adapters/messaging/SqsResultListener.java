package com.vclipper.processing.infrastructure.adapters.messaging;

import com.vclipper.processing.application.common.ProcessingError;
import com.vclipper.processing.application.usecases.ProcessVclippingResultUseCase;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Infrastructure layer SQS Result Message Listener.
 * 
 * Responsibilities:
 * - Receive SQS result messages via @SqsListener from vclipping service
 * - Delegate to Application layer for processing
 * - Handle infrastructure-specific concerns (logging, error propagation)
 * 
 * This component follows clean architecture principles:
 * - No business logic (delegated to Application layer)
 * - No domain knowledge (uses Application layer abstractions)
 * - Infrastructure concerns only (SQS integration)
 * 
 * Pattern matches vclipping's VideoProcessingMessageListener exactly.
 */
@Component
public class SqsResultListener {
    
    private static final Logger logger = LoggerFactory.getLogger(SqsResultListener.class);
    
    private final ProcessVclippingResultUseCase processResultUseCase;
    
    public SqsResultListener(ProcessVclippingResultUseCase processResultUseCase) {
        this.processResultUseCase = processResultUseCase;
    }
    
    /**
     * SQS result message listener entry point.
     * 
     * Delegates all processing to the Application layer use case.
     * The use case handles message parsing, validation, and business logic.
     * 
     * @param messageBody Raw JSON result message body from SQS
     */
    @SqsListener("${AWS_SQS_RESULT_QUEUE_URL}")
    public void processResultMessage(String messageBody) {
        logger.info("📨 Received SQS result message from vclipping service");
        logger.debug("📄 Raw result message: {}", messageBody);
        
        try {
            // Delegate to Application layer - no business logic here
            var result = processResultUseCase.executeFromRawMessage(messageBody);
            
            if (result.isSuccess()) {
                logger.info("✅ Vclipping result message processed successfully");
            } else {
                ProcessingError error = result.getError().orElse(
                    new ProcessingError("UNKNOWN_ERROR", "Unknown result processing error", null)
                );
                logger.error("❌ Vclipping result processing failed: {}", error.message());
                
                // Re-throw to trigger SQS retry mechanism
                // Spring Cloud AWS SQS handles retry and DLQ automatically
                throw new SqsResultProcessingException("Result processing failed: " + error.message());
            }
            
        } catch (Exception e) {
            logger.error("❌ Error processing SQS result message: {}", e.getMessage(), e);
            
            // Re-throw to trigger SQS retry mechanism
            throw new SqsResultProcessingException("Failed to process vclipping result message", e);
        }
    }
    
    /**
     * Infrastructure-specific exception for SQS result message processing failures.
     * Used to trigger SQS retry mechanisms.
     * 
     * Pattern matches vclipping's SqsMessageProcessingException exactly.
     */
    public static class SqsResultProcessingException extends RuntimeException {
        public SqsResultProcessingException(String message) {
            super(message);
        }
        
        public SqsResultProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
