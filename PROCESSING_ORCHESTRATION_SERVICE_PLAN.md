# VClipper Processing Orchestration Service - Implementation Plan

## 🎯 Project Overview

The Processing Orchestration Service is responsible for handling video uploads, managing processing workflows, tracking status, and coordinating with external services (S3, SQS, SNS) using a simplified clean architecture approach.

## 🏗️ Simplified Clean Architecture Structure

```
src/main/java/com/vclipper/processing/
├── domain/
│   ├── entity/
│   │   ├── VideoProcessingRequest.java
│   │   ├── ProcessingStatus.java
│   │   ├── VideoMetadata.java
│   │   └── User.java
│   ├── exceptions/
│   │   ├── VideoProcessingException.java
│   │   ├── InvalidVideoFormatException.java
│   │   ├── VideoNotFoundException.java
│   │   ├── VideoUploadException.java
│   │   └── ProcessingStatusException.java
│   ├── enums/
│   │   ├── ProcessingStatusEnum.java
│   │   ├── VideoFormat.java
│   │   └── NotificationType.java
│   └── event/
│       ├── DomainEvent.java
│       ├── VideoUploadedEvent.java
│       ├── ProcessingStartedEvent.java
│       ├── ProcessingCompletedEvent.java
│       └── ProcessingFailedEvent.java
├── application/
│   ├── ports/
│   │   ├── VideoRepositoryPort.java
│   │   ├── FileStoragePort.java
│   │   ├── MessageQueuePort.java
│   │   ├── NotificationPort.java
│   │   └── UserServicePort.java
│   └── usecases/
│       ├── SubmitVideoProcessingUseCase.java
│       ├── GetProcessingStatusUseCase.java
│       ├── ListUserVideosUseCase.java
│       ├── GetVideoDownloadUrlUseCase.java
│       └── UpdateProcessingStatusUseCase.java
├── infrastructure/
│   ├── config/
│   │   ├── ProcessingConfig.java
│   │   ├── S3Config.java
│   │   ├── SQSConfig.java
│   │   ├── SNSConfig.java
│   │   └── MongoConfig.java
│   ├── adapters/
│   │   ├── persistence/
│   │   │   ├── VideoRepositoryAdapter.java
│   │   │   ├── VideoProcessingEntity.java
│   │   │   └── VideoProcessingRepository.java
│   │   ├── storage/
│   │   │   └── S3FileStorageAdapter.java
│   │   ├── messaging/
│   │   │   ├── SQSMessageAdapter.java
│   │   │   └── SNSNotificationAdapter.java
│   │   └── external/
│   │       └── UserServiceAdapter.java
│   └── controllers/
│       ├── VideoProcessingController.java
│       ├── VideoStatusController.java
│       ├── GlobalExceptionHandler.java
│       └── dto/
│           ├── VideoUploadRequest.java
│           ├── VideoUploadResponse.java
│           ├── ProcessingStatusResponse.java
│           ├── VideoListResponse.java
│           ├── VideoDownloadResponse.java
│           └── ErrorResponse.java
└── ProcessingOrchestrationApplication.java
```

## 📦 Enhanced Dependencies (pom.xml)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.4.1</version>
        <relativePath/>
    </parent>
    
    <groupId>com.vclipper</groupId>
    <artifactId>processing-orchestration</artifactId>
    <version>0.0.1-SNAPSHOT</version>
    <name>VClipper Processing Orchestration Service</name>
    <description>Video processing orchestration and workflow management service</description>
    
    <properties>
        <java.version>21</java.version>
        <aws.sdk.version>2.31.25</aws.sdk.version>
        <jwt.version>0.11.5</jwt.version>
        <lombok.version>1.18.34</lombok.version>
        <springdoc.version>2.8.8</springdoc.version>
        <spring.cloud.version>4.2.0</spring.cloud.version>
        <spring.security.version>5.8.4</spring.security.version>
        <mockito.version>5.11.0</mockito.version>
        <commons.fileupload.version>1.5</commons.fileupload.version>
        <commons.io.version>2.15.1</commons.io.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Core Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>${lombok.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- JWT Dependencies -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>${jwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>${jwt.version}</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>${jwt.version}</version>
        </dependency>

        <!-- Spring Security OAuth2 -->
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-oauth2-jose</artifactId>
            <version>${spring.security.version}</version>
        </dependency>

        <!-- AWS SDK Dependencies -->
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>s3</artifactId>
            <version>${aws.sdk.version}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>sqs</artifactId>
            <version>${aws.sdk.version}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>sns</artifactId>
            <version>${aws.sdk.version}</version>
        </dependency>
        <dependency>
            <groupId>software.amazon.awssdk</groupId>
            <artifactId>apache-client</artifactId>
            <version>${aws.sdk.version}</version>
        </dependency>

        <!-- File Upload Dependencies -->
        <dependency>
            <groupId>commons-fileupload</groupId>
            <artifactId>commons-fileupload</artifactId>
            <version>${commons.fileupload.version}</version>
        </dependency>
        <dependency>
            <groupId>commons-io</groupId>
            <artifactId>commons-io</artifactId>
            <version>${commons.io.version}</version>
        </dependency>

        <!-- OpenAPI Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>${springdoc.version}</version>
        </dependency>

        <!-- Spring Cloud -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
            <version>${spring.cloud.version}</version>
        </dependency>

        <!-- Jackson Dependencies -->
        <dependency>
            <groupId>com.fasterxml.jackson.datatype</groupId>
            <artifactId>jackson-datatype-jsr310</artifactId>
        </dependency>

        <!-- Testing Dependencies -->
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
            <version>${mockito.version}</version>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>mongodb</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <version>0.8.10</version>
                <executions>
                    <execution>
                        <id>prepare-agent</id>
                        <goals>
                            <goal>prepare-agent</goal>
                        </goals>
                    </execution>
                    <execution>
                        <id>report</id>
                        <phase>prepare-package</phase>
                        <goals>
                            <goal>report</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.10.1</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>${lombok.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-surefire-plugin</artifactId>
                <version>3.0.0-M5</version>
            </plugin>
        </plugins>
    </build>
</project>
```

## 🚀 Implementation Plan

### **Phase 1: Project Setup (30 minutes) ✅ COMPLETED**

#### **1.1 Initialize Project Structure ✅**
- [x] Create Maven project with optimized pom.xml (removed Spring Security, JWT, Spring Cloud)
- [x] Set up directory structure following simplified clean architecture
- [x] Configure application.yml with AWS and MongoDB settings (port 8080, MongoDB 8.0.10)
- [x] Create Docker Compose for local development environment (no LocalStack, direct AWS)

#### **1.2 Basic Configuration ✅**
- [x] Configure Spring Boot main application class
- [x] Set up basic logging configuration
- [x] Configure Spring Boot Actuator for health/info endpoints (no custom health controller)
- [x] Add build-info plugin for automatic version management
- [x] Create multi-stage Dockerfile with debug support
- [x] Verify build with `mvn clean compile` - SUCCESS

### **Phase 2: Domain Layer (1 hour) ✅ COMPLETED**

#### **2.1 Core Entities ✅**
- [x] `ProcessingStatus.java` - Status value object with business rules (record)
- [x] `VideoMetadata.java` - Video file information (record, clean architecture compliant)
- [x] `VideoProcessingRequest.java` - Main aggregate root with business logic
- [x] `User.java` - User reference entity (record)

#### **2.2 Domain Exceptions ✅**
- [x] `VideoProcessingException.java` - Base exception
- [x] `InvalidVideoFormatException.java` - Format validation
- [x] `VideoNotFoundException.java` - Not found scenarios
- [x] `VideoUploadException.java` - Upload failures
- [x] `ProcessingStatusException.java` - Status update failures

#### **2.3 Enums and Value Objects ✅**
- [x] `VideoFormat.java` - Supported video formats (enum)
- [x] `NotificationType.java` - SNS notification types (record with message templates)

#### **2.4 Domain Events ✅**
- [x] `DomainEvent.java` - Base event class
- [x] `VideoUploadedEvent.java` - Upload completion
- [x] `ProcessingStartedEvent.java` - Processing initiation
- [x] `ProcessingCompletedEvent.java` - Processing success
- [x] `ProcessingFailedEvent.java` - Processing failure

### **Phase 3: Application Layer (1.5 hours)**

#### **3.1 Port Interfaces**
- [ ] `VideoRepositoryPort.java` - Data persistence operations
- [ ] `FileStoragePort.java` - S3 file operations
- [ ] `MessageQueuePort.java` - SQS messaging operations
- [ ] `NotificationPort.java` - SNS notification operations
- [ ] `UserServicePort.java` - User service integration

#### **3.2 Use Cases**
- [ ] `SubmitVideoProcessingUseCase.java` - Handle video upload and queue processing
- [ ] `GetProcessingStatusUseCase.java` - Retrieve processing status
- [ ] `ListUserVideosUseCase.java` - List user's videos with status
- [ ] `GetVideoDownloadUrlUseCase.java` - Generate presigned download URLs
- [ ] `UpdateProcessingStatusUseCase.java` - Update status from processing service

### **Phase 4: Infrastructure Layer (2 hours)**

#### **4.1 Configuration Classes**
- [ ] `ProcessingConfig.java` - Main configuration
- [ ] `S3Config.java` - S3 client configuration
- [ ] `SQSConfig.java` - SQS client configuration
- [ ] `SNSConfig.java` - SNS client configuration
- [ ] `MongoConfig.java` - MongoDB configuration

#### **4.2 Persistence Adapters**
- [ ] `VideoRepositoryAdapter.java` - MongoDB repository implementation
- [ ] `VideoProcessingEntity.java` - MongoDB document entity
- [ ] `VideoProcessingRepository.java` - Spring Data MongoDB repository

#### **4.3 External Service Adapters**
- [ ] `S3FileStorageAdapter.java` - S3 operations (upload, download, presigned URLs)
- [ ] `SQSMessageAdapter.java` - SQS message publishing
- [ ] `SNSNotificationAdapter.java` - SNS notification sending
- [ ] `UserServiceAdapter.java` - User service integration (if needed)

### **Phase 5: API Layer (1 hour)**

#### **5.1 REST Controllers**
- [ ] `VideoProcessingController.java` - Video upload and processing endpoints
- [ ] `VideoStatusController.java` - Status tracking endpoints
- [ ] `GlobalExceptionHandler.java` - Centralized error handling

#### **5.2 DTOs and Validation**
- [ ] `VideoUploadRequest.java` - Multipart file upload request
- [ ] `VideoUploadResponse.java` - Upload confirmation response
- [ ] `ProcessingStatusResponse.java` - Status information response
- [ ] `VideoListResponse.java` - User videos list response
- [ ] `VideoDownloadResponse.java` - Download URL response
- [ ] `ErrorResponse.java` - Standardized error response

### **Phase 6: Testing & Integration (1 hour)**

#### **6.1 Unit Tests**
- [ ] Domain entity tests with validation scenarios
- [ ] Use case tests with mocked dependencies
- [ ] Adapter tests with mocked external services

#### **6.2 Integration Tests**
- [ ] Controller integration tests
- [ ] MongoDB integration tests with Testcontainers
- [ ] AWS service integration tests (with LocalStack if needed)

#### **6.3 Local Testing Scripts**
- [ ] Create `test-processing-service.sh` script
- [ ] Docker Compose setup for local testing
- [ ] Postman collection for API testing

## 🔧 Configuration Files

### **application.yml**
```yaml
server:
  port: 8082

spring:
  application:
    name: vclipper-processing-orchestration
  data:
    mongodb:
      uri: ${MONGODB_URI:mongodb://localhost:27017/vclipper}
  servlet:
    multipart:
      max-file-size: 500MB
      max-request-size: 500MB

aws:
  region: ${AWS_REGION:us-east-1}
  s3:
    bucket-name: ${AWS_S3_BUCKET:vclipper-videos}
  sqs:
    processing-queue-url: ${AWS_SQS_PROCESSING_QUEUE_URL}
  sns:
    notification-topic-arn: ${AWS_SNS_NOTIFICATION_TOPIC_ARN}

vclipper:
  video:
    max-size-mb: 500
    allowed-formats: mp4,avi,mov,wmv,flv,webm
  processing:
    timeout-minutes: 30

logging:
  level:
    com.vclipper: DEBUG
    org.springframework.web: INFO
```

### **docker-compose.yml**
```yaml
services:
  processing-service:
    build: .
    ports:
      - "8082:8082"
      - "5006:5005"  # Debug port
    environment:
      - MONGODB_URI=mongodb://mongodb:27017/vclipper
      - AWS_REGION=us-east-1
      - AWS_S3_BUCKET=vclipper-videos-local
      - AWS_SQS_PROCESSING_QUEUE_URL=http://localstack:4566/000000000000/vclipper-processing
      - AWS_SNS_NOTIFICATION_TOPIC_ARN=arn:aws:sns:us-east-1:000000000000:vclipper-notifications
      - AWS_ENDPOINT_URL=http://localstack:4566
    depends_on:
      - mongodb
      - localstack
    volumes:
      - .:/app
      - ~/.aws:/root/.aws:ro

  mongodb:
    image: mongo:8.0.1
    ports:
      - "27017:27017"
    environment:
      - MONGO_INITDB_DATABASE=vclipper
    volumes:
      - mongodb_data:/data/db

  localstack:
    image: localstack/localstack:latest
    ports:
      - "4566:4566"
    environment:
      - SERVICES=s3,sqs,sns
      - DEBUG=1
      - DATA_DIR=/tmp/localstack/data
    volumes:
      - localstack_data:/tmp/localstack

volumes:
  mongodb_data:
  localstack_data:
```

## 📋 API Endpoints

### **Video Processing Endpoints**
- `POST /api/videos/upload` - Upload video for processing
- `GET /api/videos/{videoId}/status` - Get processing status
- `GET /api/videos/{videoId}/download` - Get download URL
- `GET /api/videos` - List user's videos
- `PUT /api/videos/{videoId}/status` - Update processing status (internal)

### **Health & Monitoring**
- `GET /actuator/health` - Health check
- `GET /actuator/metrics` - Application metrics
- `GET /swagger-ui.html` - API documentation

## 🎯 Success Criteria

### **Functional Requirements**
- [ ] Video upload with validation (format, size)
- [ ] SQS message publishing for processing
- [ ] SNS notifications for status updates
- [ ] Processing status tracking
- [ ] Secure file download URLs
- [ ] User video listing

### **Non-Functional Requirements**
- [ ] 80%+ test coverage
- [ ] Sub-second response times for status queries
- [ ] Proper error handling and logging
- [ ] Security with JWT validation
- [ ] Docker containerization
- [ ] API documentation

### **Integration Requirements**
- [ ] MongoDB persistence working
- [ ] S3 file operations working
- [ ] SQS message publishing working
- [ ] SNS notifications working
- [ ] Frontend API compatibility

## 🚀 Next Steps

1. **Start with Phase 1** - Project setup and basic configuration
2. **Implement Domain Layer** - Core business entities and rules
3. **Build Application Layer** - Use cases and business logic
4. **Create Infrastructure** - AWS integrations and persistence
5. **Develop API Layer** - REST endpoints and validation
6. **Test & Validate** - Comprehensive testing and integration

This plan provides a clear roadmap for implementing the Processing Orchestration Service using simplified clean architecture principles while maintaining high quality and testability.
