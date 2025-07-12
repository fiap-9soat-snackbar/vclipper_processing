# Architecture Decision Records - VClipping Video Processing Service

## Service Overview
**Service Name:** vclipping  
**Purpose:** Core video processing service for frame extraction and video manipulation  
**Architecture Pattern:** Clean Architecture with Hexagonal Architecture principles  
**Primary Responsibility:** FFmpeg-based video processing and frame extraction  

---

## ADR-001: Programming Language and Runtime
**Decision:** Java 21 with Spring Boot 3.4.1  
**Status:** Accepted  
**Context:** Need for high-performance video processing service with system integration capabilities  
**Decision Rationale:**
- Java 21 provides latest LTS features and performance improvements
- Spring Boot 3.4.1 offers mature ecosystem for microservices
- Strong integration with system processes (FFmpeg)
- Excellent memory management for video processing workloads
- Team expertise and consistency with other services

**Memory Requirements:**
- **Minimum RAM:** 1.5GB required for video processing operations
- **Constraint:** Video processing workload drives infrastructure sizing
- **Impact:** Influenced EKS instance sizing (t3.large) across entire platform

**Consequences:**
- ✅ Mature ecosystem and extensive library support
- ✅ Strong system process integration capabilities
- ✅ Excellent memory management for large video files
- ✅ Consistent technology stack across services
- ❌ Higher memory footprint compared to native solutions
- ❌ JVM startup overhead

---

## ADR-002: Video Processing Technology
**Decision:** FFmpeg as primary video processing engine  
**Status:** Accepted  
**Context:** Need for robust, industry-standard video processing capabilities  
**Decision Rationale:**
- FFmpeg is the industry standard for video processing
- Supports wide range of video formats and codecs
- High performance and reliability
- Extensive documentation and community support
- Command-line interface suitable for system integration

**FFmpeg Integration:**
```java
public class FFmpegVideoProcessor implements VideoProcessorPort {
    private final String ffmpegPath;
    private final String ffprobePath;
    private final long timeoutSeconds = 300; // 5 minutes
    
    // Real FFmpeg command execution
    private ProcessBuilder createFFmpegCommand(String inputPath, String outputPath, 
                                             FrameExtractionSettings settings) {
        return new ProcessBuilder(
            ffmpegPath,
            "-i", inputPath,
            "-vf", String.format("fps=%d", settings.getFps()),
            "-q:v", String.valueOf(settings.getJpegQuality()),
            outputPath
        );
    }
}
```

**Supported Operations:**
- Frame extraction at configurable FPS
- Video format validation and conversion
- Video metadata extraction (duration, resolution, codec)
- Batch processing with ZIP archive creation
- Quality control and compression settings

**Configuration:**
```yaml
vclipping:
  ffmpeg:
    path: ${FFMPEG_PATH}              # /usr/bin/ffmpeg
    probe-path: ${FFPROBE_PATH}       # /usr/bin/ffprobe
  processing:
    video:
      allowed-formats: ${VCLIPPING_ALLOWED_FORMATS}    # mp4,avi,mov,mkv
      max-file-size-mb: ${VCLIPPING_MAX_FILE_SIZE_MB}  # 100
      max-duration-seconds: ${VCLIPPING_MAX_DURATION_SECONDS}  # 600
    frames:
      default-fps: ${VCLIPPING_DEFAULT_FPS}            # 1
      jpeg-quality: ${VCLIPPING_JPEG_QUALITY}          # 85
```

**Consequences:**
- ✅ Industry-standard video processing capabilities
- ✅ High performance and reliability
- ✅ Wide format support
- ✅ Extensive configuration options
- ❌ External dependency on FFmpeg binary
- ❌ System-level integration complexity
- ❌ Platform-specific deployment requirements

---

## ADR-003: Architecture Pattern
**Decision:** Clean Architecture with Hexagonal Architecture (Ports & Adapters)  
**Status:** Accepted  
**Context:** Need for maintainable, testable video processing service with external integrations  

**Architecture Layers:**
```
├── domain/                    # Business entities and value objects
│   ├── entity/               # VideoProcessingTask, ProcessingResult, VideoFile
│   ├── valueobject/          # FrameExtractionSettings, ProcessingStatus, OutputFormat
│   └── exceptions/           # Domain-specific exceptions
├── application/              # Use cases and business logic
│   ├── usecases/            # ProcessVideoUseCase, ConsumeProcessingMessageUseCase
│   ├── ports/               # Interface definitions
│   └── common/              # Shared application logic
└── infrastructure/           # External adapters and configurations
    ├── adapters/            # FFmpeg, S3, SQS, API adapters
    ├── controllers/         # REST API controllers
    ├── messaging/           # Message queue listeners
    └── config/              # Configuration classes
```

**Key Domain Entities:**
- **VideoProcessingTask:** Represents a video processing job
- **ProcessingResult:** Contains processing outcomes and metadata
- **VideoFile:** Encapsulates video file information and validation
- **FrameExtractionSettings:** Configuration for frame extraction operations

**Port Interfaces:**
- **VideoProcessorPort:** Video processing operations
- **FileStoragePort:** File storage and retrieval
- **MessageQueuePort:** Asynchronous messaging
- **ProcessingApiPort:** External API communication

**Consequences:**
- ✅ High testability and maintainability
- ✅ Technology independence for business logic
- ✅ Clear separation of concerns
- ✅ Easy mocking for testing
- ❌ Initial complexity overhead
- ❌ More boilerplate code

---

## ADR-004: Message Queue Integration
**Decision:** AWS SQS for asynchronous video processing workflows  
**Status:** Accepted  
**Context:** Need for reliable, scalable message processing for video jobs  

**Message Flow Architecture:**
1. **Processing Queue:** Receives video processing requests from vclipper_processing
2. **Message Consumption:** VClipping service consumes messages asynchronously
3. **Result Queue:** Sends processing results back to vclipper_processing
4. **Error Handling:** Dead letter queues for failed processing attempts

**SQS Configuration:**
```yaml
aws:
  sqs:
    processing-queue-url: ${AWS_SQS_PROCESSING_QUEUE_URL}
    result-queue-url: ${AWS_SQS_RESULT_QUEUE_URL}
```

**Message Processing:**
```java
@SqsListener("${aws.sqs.processing-queue-url}")
public void handleProcessingMessage(String message) {
    try {
        VideoProcessingMessage request = objectMapper.readValue(message, VideoProcessingMessage.class);
        Result<ProcessingResult> result = consumeProcessingMessageUseCase.execute(request);
        
        if (result.isSuccess()) {
            sendResultMessage(result.getValue());
        } else {
            handleProcessingFailure(request, result.getError());
        }
    } catch (Exception e) {
        logger.error("Failed to process message: {}", message, e);
        throw new MessageProcessingException("Message processing failed", e);
    }
}
```

**Message Types:**
- **VideoProcessingMessage:** Video processing job requests
- **ProcessingResultMessage:** Processing completion notifications
- **ProcessingFailureMessage:** Error notifications

**Reliability Features:**
- Message visibility timeout: 300 seconds (5 minutes)
- Dead letter queue after 3 failed attempts
- Exponential backoff for retries
- Message deduplication for FIFO queues

**Consequences:**
- ✅ Reliable asynchronous processing
- ✅ Decoupled service communication
- ✅ Built-in retry and error handling
- ✅ Scalable message processing
- ❌ Eventual consistency challenges
- ❌ Message ordering complexity

---

## ADR-005: File Storage Strategy
**Decision:** AWS S3 for video file storage with local temporary processing  
**Status:** Accepted  
**Context:** Need for scalable file storage with local processing capabilities  

**Storage Architecture:**
1. **S3 Input Storage:** Original video files uploaded by users
2. **Local Temporary Processing:** Files downloaded to local filesystem for FFmpeg processing
3. **S3 Output Storage:** Generated frames and ZIP archives uploaded back to S3
4. **Cleanup:** Local temporary files cleaned up after processing

**S3 Configuration:**
```yaml
aws:
  s3:
    bucket: ${AWS_S3_BUCKET}
    input-prefix: videos/          # Original video files
    output-prefix: clips/          # Generated frame images
    temp-prefix: temp/             # Temporary processing files
```

**File Processing Workflow:**
```java
public Result<ProcessingResult> processVideo(VideoProcessingTask task) {
    try {
        // 1. Download video from S3 to local temp directory
        Path localVideoPath = downloadVideoFromS3(task.getVideoKey());
        
        // 2. Process video with FFmpeg locally
        List<Path> extractedFrames = extractFrames(localVideoPath, task.getSettings());
        
        // 3. Create ZIP archive of frames
        Path zipArchive = createZipArchive(extractedFrames, task.getTaskId());
        
        // 4. Upload ZIP to S3
        String s3Key = uploadZipToS3(zipArchive, task.getTaskId());
        
        // 5. Cleanup local files
        cleanupLocalFiles(localVideoPath, extractedFrames, zipArchive);
        
        return Result.success(new ProcessingResult(task.getTaskId(), s3Key));
    } catch (Exception e) {
        return Result.failure(new ProcessingError("Video processing failed", e));
    }
}
```

**Local Processing Requirements:**
- **Temporary Directory:** Configurable local storage for processing
- **Disk Space:** Sufficient space for video files and extracted frames
- **Cleanup Strategy:** Automatic cleanup after processing completion
- **Error Handling:** Cleanup on processing failures

**Consequences:**
- ✅ Scalable cloud storage for large video files
- ✅ Local processing performance with FFmpeg
- ✅ Automatic cleanup and space management
- ✅ Separation of input and output storage
- ❌ Network overhead for file transfers
- ❌ Local disk space requirements
- ❌ Complexity in error handling and cleanup

---

## ADR-006: Error Handling and Resilience
**Decision:** Comprehensive error handling with Result pattern and retry mechanisms  
**Status:** Accepted  
**Context:** Need for robust error handling in video processing workflows  

**Exception Hierarchy:**
```java
domain/exceptions/
├── VideoProcessingException (base)
├── FFmpegSystemException (FFmpeg execution errors)
└── StorageException (file storage errors)
```

**Result Pattern Implementation:**
```java
public class Result<T, E> {
    private final T value;
    private final E error;
    private final boolean success;
    
    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(value, null, true);
    }
    
    public static <T, E> Result<T, E> failure(E error) {
        return new Result<>(null, error, false);
    }
}
```

**Error Categories:**
1. **Processing Errors:** FFmpeg execution failures, invalid video formats
2. **Resource Errors:** File not found, insufficient disk space, network issues
3. **Validation Errors:** Invalid parameters, unsupported formats
4. **System Errors:** Infrastructure failures, timeout errors

**Retry Strategy:**
- **FFmpeg Failures:** Single retry with different parameters
- **Network Failures:** Exponential backoff (3 attempts)
- **Storage Failures:** Immediate retry (2 attempts)
- **System Failures:** No retry, immediate failure notification

**Timeout Configuration:**
- **FFmpeg Processing:** 5 minutes per video
- **File Downloads:** 2 minutes per file
- **File Uploads:** 3 minutes per file
- **API Calls:** 30 seconds per request

**Consequences:**
- ✅ Comprehensive error handling and recovery
- ✅ Clear error communication to clients
- ✅ Resilient processing workflows
- ✅ Proper resource cleanup on failures
- ❌ Increased complexity in error flow handling
- ❌ Potential for cascading failures

---

## ADR-007: Configuration Management
**Decision:** Environment-based configuration with extensive externalization  
**Status:** Accepted  
**Context:** Need for flexible configuration across environments and deployment scenarios  

**Configuration Strategy:**
- All configuration values externalized via environment variables
- No hardcoded defaults in application code
- Profile-based configuration for different environments
- Comprehensive configuration validation at startup

**Key Configuration Areas:**
```yaml
# FFmpeg Configuration
vclipping:
  ffmpeg:
    path: ${FFMPEG_PATH}
    probe-path: ${FFPROBE_PATH}
  
# Processing Limits
  processing:
    video:
      max-file-size-mb: ${VCLIPPING_MAX_FILE_SIZE_MB}
      max-duration-seconds: ${VCLIPPING_MAX_DURATION_SECONDS}
      allowed-formats: ${VCLIPPING_ALLOWED_FORMATS}
    
# AWS Integration
aws:
  region: ${AWS_REGION}
  s3:
    bucket: ${AWS_S3_BUCKET}
  sqs:
    processing-queue-url: ${AWS_SQS_PROCESSING_QUEUE_URL}
    result-queue-url: ${AWS_SQS_RESULT_QUEUE_URL}
```

**Environment Profiles:**
- **development:** Local development with debug logging
- **test:** Automated testing with mock services
- **mock:** Vertical slice testing with real FFmpeg
- **production:** Production deployment with full AWS integration

**Configuration Validation:**
- FFmpeg binary availability check at startup
- AWS service connectivity validation
- Directory permissions and disk space checks
- Configuration parameter validation

**Consequences:**
- ✅ Environment-specific flexibility
- ✅ No hardcoded configuration values
- ✅ Easy deployment across environments
- ✅ Configuration validation and error reporting
- ❌ Configuration complexity
- ❌ Potential for configuration drift

---

## ADR-008: Container Deployment Strategy
**Decision:** Multi-stage Docker build with FFmpeg integration  
**Status:** Accepted  
**Context:** Need for containerized deployment with video processing capabilities  

**Dockerfile Strategy:**
```dockerfile
# Build stage
FROM maven:3.9.9-eclipse-temurin-21-noble AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Runtime stage
FROM eclipse-temurin:21-jre-noble
RUN apt-get update && \
    apt-get install -y ffmpeg zip && \
    rm -rf /var/lib/apt/lists/* && \
    apt-get clean

WORKDIR /app
COPY --from=builder /app/target/vclipping-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8081
CMD ["java", "-jar", "app.jar"]
```

**Container Features:**
- **Multi-stage Build:** Separate build and runtime stages for smaller image
- **FFmpeg Installation:** System-level FFmpeg and zip utilities
- **Java 21 Runtime:** Eclipse Temurin JRE for production deployment
- **Layer Optimization:** Maven dependency caching for faster builds

**Resource Requirements:**
- **Memory:** Minimum 1.5GB for video processing operations
- **CPU:** Variable based on video complexity and duration
- **Storage:** Temporary disk space for video processing
- **Network:** AWS service connectivity for S3 and SQS

**Deployment Configuration:**
- **Port:** 8081 for HTTP API
- **Health Checks:** Spring Boot Actuator endpoints
- **Environment Variables:** All configuration externalized
- **Volume Mounts:** Temporary processing directory

**Consequences:**
- ✅ Consistent deployment across environments
- ✅ Self-contained video processing capabilities
- ✅ Optimized container size with multi-stage build
- ✅ System-level FFmpeg integration
- ❌ Larger container size due to FFmpeg dependencies
- ❌ Platform-specific container builds

---

## ADR-009: Testing Strategy
**Decision:** Multi-layer testing with real FFmpeg integration  
**Status:** Accepted  
**Context:** Need for comprehensive testing of video processing functionality  

**Testing Layers:**
1. **Unit Tests:** Domain logic and use cases with mocked dependencies
2. **Integration Tests:** FFmpeg integration with real video files
3. **Contract Tests:** API endpoint testing with mock services
4. **End-to-End Tests:** Full workflow testing with test videos

**Test Configuration:**
```yaml
# Test Profile
spring:
  config:
    activate:
      on-profile: test

vclipping:
  test:
    videos-dir: ${VCLIPPING_TEST_VIDEOS_DIR}
    output-dir: ${VCLIPPING_OUTPUT_DIR}
    temp-dir: ${VCLIPPING_TEMP_DIR}
    video-file: ${VCLIPPING_TEST_VIDEO}
```

**Testing Approach:**
- **Real FFmpeg:** Integration tests use actual FFmpeg binary
- **Test Videos:** Small sample videos for processing tests
- **Mock Services:** AWS services mocked for unit testing
- **Test Containers:** Docker containers for integration testing

**Test Coverage Areas:**
- Video format validation and processing
- Frame extraction with various settings
- Error handling and recovery scenarios
- File storage and cleanup operations
- Message queue processing workflows

**Consequences:**
- ✅ Comprehensive testing of video processing functionality
- ✅ Real FFmpeg integration validation
- ✅ Fast unit tests with mocked dependencies
- ✅ Reliable integration testing
- ❌ Test complexity with real video processing
- ❌ Test execution time for integration tests

---

## ADR-010: Monitoring and Observability
**Decision:** Spring Boot Actuator with Prometheus metrics  
**Status:** Accepted  
**Context:** Need for comprehensive monitoring of video processing operations  

**Monitoring Configuration:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus
  endpoint:
    prometheus:
      access: UNRESTRICTED
  health:
    mongo:
      enabled: false  # Disabled as not using MongoDB
```

**Key Metrics Monitored:**
- **Processing Metrics:** Video processing duration, success/failure rates
- **Resource Metrics:** Memory usage, CPU utilization, disk space
- **Queue Metrics:** Message processing rates, queue depth
- **Error Metrics:** Error rates by type, retry attempts
- **Business Metrics:** Videos processed, frames extracted, file sizes

**Custom Metrics:**
```java
@Component
public class ProcessingMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter processedVideos;
    private final Timer processingDuration;
    private final Gauge queueDepth;
    
    // Custom metrics for video processing operations
}
```

**Health Checks:**
- **FFmpeg Availability:** Validates FFmpeg binary accessibility
- **Disk Space:** Monitors available disk space for processing
- **AWS Connectivity:** Checks S3 and SQS service availability
- **Processing Queue:** Monitors message queue health

**Consequences:**
- ✅ Comprehensive video processing monitoring
- ✅ Prometheus integration for metrics collection
- ✅ Custom business metrics tracking
- ✅ Proactive health monitoring
- ❌ Monitoring overhead on processing performance
- ❌ Metrics storage and retention costs

---

## ADR-011: Performance Optimization
**Decision:** Memory-efficient processing with configurable limits  
**Status:** Accepted  
**Context:** Need for optimal performance within memory constraints  

**Performance Strategies:**
1. **Memory Management:** Careful memory allocation for large video files
2. **Processing Limits:** Configurable limits on file size and duration
3. **Temporary File Cleanup:** Immediate cleanup after processing
4. **Batch Processing:** Efficient frame extraction and ZIP creation
5. **Timeout Management:** Prevents resource exhaustion from long-running processes

**Resource Limits:**
```yaml
vclipping:
  processing:
    video:
      max-file-size-mb: 500        # Maximum video file size (500MB)
      max-duration-seconds: 3600   # Maximum video duration (1 hour)
    frames:
      default-fps: 1               # Conservative frame extraction rate
      jpeg-quality: 85             # Balance between quality and size
```

**Memory Optimization:**
- **Streaming Processing:** Process videos without loading entirely into memory
- **Temporary File Management:** Use local filesystem for intermediate processing
- **Garbage Collection:** Explicit cleanup of large objects
- **Resource Pooling:** Reuse processing resources where possible

**Processing Optimization:**
- **FFmpeg Parameters:** Optimized FFmpeg commands for performance
- **Parallel Processing:** Multiple videos processed concurrently (with limits)
- **Compression Settings:** Balanced quality vs. processing speed
- **Early Validation:** Validate video files before expensive processing

**Consequences:**
- ✅ Efficient memory usage for large video files
- ✅ Predictable resource consumption
- ✅ Configurable performance tuning
- ✅ Prevention of resource exhaustion
- ❌ Processing limitations may restrict functionality
- ❌ Complexity in resource management

---

## Summary of Key Architectural Decisions

1. **Java 21 + Spring Boot 3.4.1** for high-performance video processing service
2. **FFmpeg integration** as industry-standard video processing engine
3. **Clean Architecture** with hexagonal pattern for maintainability and testability
4. **AWS SQS messaging** for reliable asynchronous video processing workflows
5. **S3 + local processing** strategy for scalable file storage with performance
6. **Comprehensive error handling** with Result pattern and retry mechanisms
7. **Environment-based configuration** with complete externalization
8. **Multi-stage Docker build** with FFmpeg system integration
9. **Multi-layer testing** with real FFmpeg integration validation
10. **Spring Boot Actuator + Prometheus** for comprehensive monitoring
11. **Memory-efficient processing** with configurable resource limits
12. **1.5GB minimum RAM requirement** driving infrastructure sizing decisions

## Critical Architectural Trade-offs

1. **Memory vs. Performance:** 1.5GB minimum RAM requirement for video processing capability
2. **Local vs. Cloud Processing:** Local FFmpeg processing with S3 storage for optimal performance
3. **Real vs. Mock Dependencies:** Real FFmpeg integration in tests vs. test execution speed
4. **Resource Limits vs. Functionality:** Conservative processing limits (500MB files, 1-hour duration) to prevent resource exhaustion
5. **Container Size vs. Capabilities:** Larger container due to FFmpeg dependencies
6. **Processing Speed vs. Quality:** Balanced FFmpeg settings for performance and output quality
7. **Synchronous vs. Asynchronous:** Asynchronous message processing with eventual consistency
8. **Configuration Complexity vs. Flexibility:** Extensive externalization for deployment flexibility

## Video Processing Constraints and Assumptions

**File Constraints:**
- Maximum video file size: 500MB
- Maximum video duration: 1 hour (3600 seconds)
- Supported formats: MP4, AVI, MOV, WMV
- Frame extraction rate: 1 FPS (configurable)
- JPEG quality: 85% (balance of quality vs. size)

**Processing Assumptions:**
- FFmpeg binary available in container environment
- Sufficient local disk space for temporary processing
- Network connectivity to AWS services (S3, SQS)
- Processing timeout: 5 minutes per video
- Memory requirement: 1.5GB minimum for video operations
