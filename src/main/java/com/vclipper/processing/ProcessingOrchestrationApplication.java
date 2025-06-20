package com.vclipper.processing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * VClipper Processing Orchestration Service
 * 
 * This service handles:
 * - Video upload and validation
 * - Processing workflow orchestration
 * - Status tracking and notifications
 * - Integration with AWS services (S3, SQS, SNS)
 */
@SpringBootApplication
public class ProcessingOrchestrationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProcessingOrchestrationApplication.class, args);
    }
}
