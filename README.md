# VClipper Processing Orchestration Service

This is the backend processing orchestration service for VClipper - a video frame extraction service built with clean architecture principles and AWS services.

## 🎯 Project Overview

The Processing Orchestration Service handles video upload, processing workflow coordination, status tracking, and user notifications. Built with clean architecture principles, it provides a robust foundation for video processing operations while maintaining clear separation of concerns and high testability.

## ✨ Features

- 📤 **Video Upload & Validation** - Multipart file uploads with format and size validation
- 🔄 **Processing Workflow Orchestration** - Coordinate video processing pipeline with SQS
- 📊 **Status Tracking** - Real-time processing status with state transitions
- 📧 **User Notifications** - SNS integration for processing updates
- 🗄️ **Data Persistence** - MongoDB integration for processing requests
- 🏗️ **Clean Architecture** - Domain-driven design with clear layer separation
- 🧪 **Comprehensive Testing** - Automated validation scripts and test coverage

## 🛠️ Tech Stack

- **Language**: Java 21 with modern language features
- **Framework**: Spring Boot 3.4.1
- **Database**: MongoDB 8.0.10
- **Cloud Services**: AWS SDK 2.31.25 (S3, SQS, SNS)
- **Build Tool**: Maven 3.9.9
- **Containerization**: Docker with multi-stage builds
- **Testing**: JUnit 5, Mockito, Testcontainers
- **Documentation**: OpenAPI 3, Spring Boot Actuator

## 📊 **CURRENT IMPLEMENTATION STATUS**

### **✅ COMPLETED: Vertical Slice 1 (VS1.5)**

**Major Achievement**: Complete end-to-end video processing orchestration service with comprehensive testing suite.

#### **What We Built**:
- [x] **Complete End-to-End Flow**: Upload → Status → List with MongoDB persistence
- [x] **API Layer**: VideoProcessingController with validation and error handling
- [x] **MongoDB Integration**: Full persistence with authentication and health monitoring
- [x] **Mock AWS Services**: S3, SQS, SNS adapters with realistic console logging
- [x] **Domain & Application Layers**: Complete business logic and use cases
- [x] **Comprehensive Testing**: 12-section integration test suite (ALL PASSING ✅)
- [x] **Docker Environment**: MongoDB 8.0.10 with proper networking

#### **Test Results**: 12/12 Integration Test Sections Passing ✅
- ✅ Environment Setup & Cleanup
- ✅ Health Check & Service Validation (MongoDB health endpoint)
- ✅ Video Upload Testing
- ✅ Video Status Retrieval Testing
- ✅ Video Listing Testing
- ✅ Error Handling Testing
- ✅ Mock Service Validation
- ✅ Database Validation (Fixed MongoDB authentication & persistence)
- ✅ Configuration Validation
- ✅ Application Logs Analysis (Fixed log level detection)
- ✅ Performance & Resource Usage
- ✅ Environment Cleanup

### **🔴 CRITICAL GAPS FOR PRODUCTION**

**Current Status**: **DEVELOPMENT PROTOTYPE** ✅ | **PRODUCTION READY** ❌

**What We're Missing**:
- ❌ **Real AWS Integration**: All AWS operations are mocked (0% real integration)
- ❌ **Download Functionality**: Users cannot retrieve processed videos (missing presigned URLs)
- ❌ **Unit Test Coverage**: Only integration tests exist (0% unit test coverage)

**Honest Assessment**: We have a sophisticated mock system with excellent integration testing, but it's **NOT FUNCTIONAL** for real users because:
1. Videos aren't actually stored anywhere retrievable
2. Users cannot download their processed videos
3. No real processing pipeline exists

### **🔴 CRITICAL PHASES NEEDED**

#### **Phase 7: Real AWS Integration (2-3 hours) 🔴 CRITICAL**
- [ ] Replace all mock adapters with real AWS implementations
- [ ] Add AWS configuration classes (S3Config, SQSConfig, SNSConfig)
- [ ] Implement AWS error handling and retry logic

#### **Phase 8: Download URLs (1-2 hours) 🔴 CRITICAL**
- [ ] GetVideoDownloadUrlUseCase + download endpoint
- [ ] S3 presigned URL generation with security
- [ ] Complete upload → process → download flow

#### **Phase 9: Unit Testing (1-2 hours) 🔴 CRITICAL**
- [ ] Domain layer unit tests
- [ ] Use case unit tests with mocked ports
- [ ] Achieve 80%+ test coverage

**Time to Production**: ~5-7 hours of focused development

## ✅ Implementation Progress

### **Phase 1: Project Setup** ✅ **COMPLETED**
- [x] **Maven Project Structure** - Clean architecture with optimized dependencies
- [x] **Spring Boot 3.4.1** - Java 21, MongoDB 8.0.10, AWS SDK 2.31.25
- [x] **Docker Configuration** - Multi-stage build with debug support
- [x] **Application Configuration** - Port 8080, Spring Boot Actuator, build-info plugin
- [x] **Development Environment** - Docker Compose with MongoDB (no LocalStack)

### **Phase 2: Domain Layer** ✅ **COMPLETED**
- [x] **Core Entities** - VideoProcessingRequest (aggregate root), ProcessingStatus, VideoMetadata, User
- [x] **Business Rules** - Status transitions, video format validation, file size limits
- [x] **Value Objects** - ProcessingStatus (record), VideoMetadata (record), NotificationType (record)
- [x] **Enumerations** - VideoFormat with validation logic
- [x] **Domain Exceptions** - Complete hierarchy for error handling
- [x] **Domain Events** - Event-driven architecture support

### **Phase 3: Application Layer** ✅ **COMPLETED**
- [x] **Port Interfaces** - 5 complete interfaces for external dependencies
- [x] **Use Cases** - 5 complete business orchestration use cases
- [x] **Configuration** - Externalized properties with type safety
- [x] **Clean Architecture** - No framework annotations in application layer
- [x] **Dependency Injection** - Spring configuration in infrastructure layer

### **Vertical Slice 1: Video Upload & Status Flow** 🔄 **IN PROGRESS**
- [x] **API Layer** - REST controllers with upload, status, list endpoints
- [x] **DTOs** - Request/response objects with validation
- [x] **Global Exception Handler** - Centralized error handling
- [x] **MongoDB Persistence** - Entity, repository, adapter with single record approach
- [x] **Mock AWS Adapters** - Console logging implementations for S3, SQS, SNS, User Service
- [ ] **Configuration & Wiring** - Spring configuration to connect all layers
- [ ] **Testing & Validation** - End-to-end testing scripts and Docker validation

### **Phase 4: Infrastructure Layer** (Planned)
- [ ] AWS S3 file storage adapter
- [ ] SQS message publishing adapter
- [ ] SNS notification adapter
- [ ] MongoDB persistence adapter

### **Phase 5: API Layer** (Planned)
- [ ] REST controllers with validation
- [ ] DTO mapping and serialization
- [ ] Global exception handling
- [ ] OpenAPI documentation

## 🚀 Getting Started

### Prerequisites

- Java 21+ (OpenJDK or Oracle JDK)
- Maven 3.9+
- Docker & Docker Compose
- AWS CLI configured (for production)
- MongoDB (via Docker Compose)

### Installation

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd vclipper_processing
   ```

2. **Run quick validation**
   ```bash
   ./scripts/quick-test.sh
   ```

3. **Build the application**
   ```bash
   mvn clean compile
   ```

4. **Start with Docker Compose**
   ```bash
   docker-compose up --build
   ```

   The application will start at `http://localhost:8080`

5. **Generate test video files (for testing)**
   ```bash
   # Create test MP4 files for upload testing
   ./create_better_test_video.sh
   ```

6. **Test video upload**
   ```bash
   # Upload a test video to verify functionality
   curl -X POST -H "Content-Type: multipart/form-data" \
     -F "file=@test_videos/better_test_video.mp4;type=video/mp4" \
     -F "userId=test-user-123" \
     http://localhost:8080/api/videos/upload
   ```

## 🔧 Environment Configuration

The application supports flexible environment-based configuration:

### Development Mode (Current Default)
```yaml
# application.yml
aws:
  region: us-east-1
  s3:
    bucket-name: vclipper-videos-dev
  sqs:
    processing-queue-url: ${AWS_SQS_PROCESSING_QUEUE_URL}
  sns:
    notification-topic-arn: ${AWS_SNS_NOTIFICATION_TOPIC_ARN}

spring:
  data:
    mongodb:
      uri: mongodb://localhost:27017/vclipper
```

### Production Mode (When deployed)
```bash
# Environment variables
MONGODB_URI=mongodb+srv://user:pass@cluster.mongodb.net/vclipper
AWS_S3_BUCKET=vclipper-videos-prod
AWS_SQS_PROCESSING_QUEUE_URL=https://sqs.us-east-1.amazonaws.com/123456789/vclipper-processing
AWS_SNS_NOTIFICATION_TOPIC_ARN=arn:aws:sns:us-east-1:123456789:vclipper-notifications
```

## 🏗️ Architecture

Built following **Clean Architecture** principles with clear separation of concerns:

```
src/main/java/com/vclipper/processing/
├── domain/                    # Business logic & rules (no external dependencies)
│   ├── entity/               # Core business entities
│   │   ├── ProcessingStatus.java      # Status with transition rules (record)
│   │   ├── VideoMetadata.java         # Video file information (record)
│   │   ├── VideoProcessingRequest.java # Aggregate root
│   │   ├── User.java                  # User reference (record)
│   │   └── NotificationType.java      # Notification templates (record)
│   ├── enums/               # Business enumerations
│   │   └── VideoFormat.java          # Supported formats with validation
│   ├── exceptions/          # Domain-specific exceptions
│   │   ├── VideoProcessingException.java
│   │   ├── InvalidVideoFormatException.java
│   │   ├── VideoNotFoundException.java
│   │   ├── VideoUploadException.java
│   │   └── ProcessingStatusException.java
│   └── event/               # Domain events
│       ├── DomainEvent.java
│       ├── VideoUploadedEvent.java
│       ├── ProcessingStartedEvent.java
│       ├── ProcessingCompletedEvent.java
│       └── ProcessingFailedEvent.java
├── application/             # Use cases & business orchestration
│   ├── ports/              # Interface definitions
│   └── usecases/           # Business use cases
├── infrastructure/         # External integrations & frameworks
│   ├── config/            # Configuration classes
│   ├── adapters/          # External service adapters
│   │   ├── persistence/   # MongoDB adapters
│   │   ├── storage/       # S3 file storage adapters
│   │   ├── messaging/     # SQS message queue adapters
│   │   ├── notification/  # SNS notification adapters
│   │   └── user/         # User service adapters
│   └── controllers/       # REST API controllers
└── ProcessingOrchestrationApplication.java
```

## 🌐 Available Endpoints

### Health & Monitoring (Current)
- `GET /actuator/health` - Health check with MongoDB status
- `GET /actuator/info` - Application information with build details
- `GET /actuator/metrics` - Application metrics

### Video Processing Endpoints
- `POST /api/videos/upload` - ✅ Upload video for processing
- `GET /api/videos/{videoId}/status` - ⚠️ Get processing status (has issues)
- `GET /api/videos/{videoId}/download` - ❌ Get download URL (not implemented)
- `GET /api/videos` - ✅ List user's videos
- `PUT /api/videos/{videoId}/status` - ❌ Update processing status (internal, not implemented)

## 🧪 Testing & Validation

### Quick Test Script
```bash
./scripts/quick-test.sh
```

**Validates:**
- ✅ Maven compilation
- ✅ Domain layer structure
- ✅ Clean architecture compliance
- ✅ Business logic implementation
- ✅ Package organization

### Test Video Generation

For testing video upload functionality, use the provided script to generate test MP4 files:

```bash
# Generate a test MP4 file with proper MIME type
./create_better_test_video.sh
```

**What the script does:**
- Creates a minimal but valid MP4 file with proper magic bytes
- Includes essential MP4 boxes: `ftyp` (file type), `moov` (movie metadata), `mdat` (media data)
- Generates a file that passes MIME type validation as `video/mp4`
- No external dependencies (pure bash implementation)
- Output: `test_videos/better_test_video.mp4`

### API Testing

#### Upload Video
```bash
# Upload a test video (requires explicit MIME type specification)
curl -X POST -H "Content-Type: multipart/form-data" \
  -F "file=@test_videos/better_test_video.mp4;type=video/mp4" \
  -F "userId=test-user-123" \
  http://localhost:8080/api/videos/upload
```

**Expected Response:**
```json
{
  "videoId": "512369db-70c7-47cc-b6bf-4ab90aed6fcf",
  "userId": "test-user-123",
  "originalFilename": "better_test_video.mp4",
  "status": {
    "value": "PENDING",
    "description": "Video uploaded and queued for processing",
    "isTerminal": false,
    "finished": false
  },
  "message": "Video uploaded successfully and queued for processing",
  "uploadedAt": "2025-06-27T03:08:40.73471691",
  "success": true
}
```

#### List User Videos
```bash
# List all videos for a user
curl -s "http://localhost:8080/api/videos?userId=test-user-123" | jq .
```

#### Check Application Health
```bash
# Verify application and MongoDB connectivity
curl -s http://localhost:8080/actuator/health | jq .
```

### Current Test Results
- ✅ **Build Status**: All 17 source files compile successfully
- ✅ **Architecture Compliance**: No infrastructure dependencies in domain
- ✅ **Business Logic**: All transition rules and validation implemented
- ✅ **Package Structure**: Proper clean architecture organization
- ✅ **Video Upload**: Successfully accepts MP4 files with proper MIME type
- ✅ **MongoDB Integration**: Persistent storage working correctly
- ✅ **User Validation**: Built-in user validation (test-user-123 is valid)
- ✅ **API Endpoints**: Upload and list endpoints fully functional

### Testing Philosophy
- **Fast Feedback Loop** - Quick validation (< 30 seconds)
- **Clean Architecture Validation** - Automatic compliance checking
- **Business Logic Testing** - Domain rules verification
- **End-to-End Testing** - Complete upload workflow validation
- **Regression Detection** - Catches breaking changes

### Known Testing Notes
- **MIME Type Detection**: Application requires explicit content-type specification in multipart uploads
- **User Validation**: Only predefined users are accepted (e.g., `test-user-123`)
- **File Format**: MP4 files must have proper structure and magic bytes for validation

## 🚀 Key Features Implemented

### **Clean Architecture Compliance**
- ✅ **Domain Purity** - No infrastructure dependencies in business logic
- ✅ **Dependency Inversion** - Interfaces define contracts, implementations in infrastructure
- ✅ **Single Responsibility** - Each layer has clear, focused responsibilities
- ✅ **Framework Independence** - Business logic independent of Spring/AWS

### **Rich Domain Model**
- ✅ **Status Transitions** - PENDING → PROCESSING → COMPLETED/FAILED with validation
- ✅ **Video Format Support** - MP4, AVI, MOV, WMV, FLV, WEBM with validation
- ✅ **Business Validation** - File size limits, format checking, status rules
- ✅ **Notification Templates** - Rich notification messages with placeholders

### **Modern Java Practices**
- ✅ **Records for Value Objects** - Immutable, clean, no boilerplate
- ✅ **Switch Expressions** - Modern Java 21 syntax
- ✅ **No Lombok in Domain** - Pure Java for business logic
- ✅ **Comprehensive Exception Handling** - Specific exceptions for different scenarios

## 📁 Project Structure

```
vclipper_processing/
├── src/main/java/com/vclipper/processing/
│   ├── domain/                          # ✅ COMPLETE - Pure business logic
│   │   ├── entity/                      # Core business entities
│   │   ├── enums/                       # Business enumerations
│   │   ├── exceptions/                  # Domain exceptions
│   │   └── event/                       # Domain events
│   ├── application/                     # 🔄 IN PROGRESS - Use cases
│   │   ├── ports/                       # Interface definitions
│   │   └── usecases/                    # Business orchestration
│   ├── infrastructure/                  # 📋 PLANNED - External integrations
│   │   ├── config/                      # Configuration classes
│   │   ├── adapters/                    # AWS service adapters
│   │   └── controllers/                 # REST API controllers
│   └── ProcessingOrchestrationApplication.java
├── src/main/resources/
│   └── application.yml                  # ✅ COMPLETE - Configuration
├── scripts/                             # ✅ COMPLETE - Testing scripts
│   ├── quick-test.sh                    # Rapid validation
│   └── README.md                        # Testing documentation
├── docker-compose.yml                   # ✅ COMPLETE - Local development
├── Dockerfile                           # ✅ COMPLETE - Multi-stage build
├── pom.xml                              # ✅ COMPLETE - Optimized dependencies
└── README.md                            # ✅ COMPLETE - This file
```

## 🔄 Development Workflow

### Local Development
```bash
# Quick validation
./scripts/quick-test.sh

# Build application
mvn clean compile

# Run tests
mvn test

# Start with Docker
docker-compose up --build
```

### Development with Debug
```bash
# Start with debug port exposed
docker-compose up --build
# Debug port available on localhost:5005
```

## 🐳 Docker Configuration

### Multi-stage Build
- **Build Stage**: `maven:3.9.9-eclipse-temurin-21-noble`
- **Runtime Stage**: `eclipse-temurin:21-jre-noble`
- **Debug Support**: Via `JAVA_OPTS` environment variable
- **Port Exposure**: 8080 (app), 5005 (debug)

### Docker Compose Services
- **processing-service**: Main application with debug support
- **mongodb**: MongoDB 8.0.10 with persistent volume
- **Volumes**: AWS credentials, application code, MongoDB data

## 🔒 Security Features

- **Environment-based configuration** - Separate settings for dev/prod
- **AWS IAM integration** - Secure service-to-service communication
- **Input validation** - Domain-level validation for all inputs
- **Error handling** - Secure error messages without sensitive data exposure
- **Clean architecture** - Security concerns isolated in infrastructure layer

## 📊 Performance Features

- **Efficient build process** - Multi-stage Docker builds
- **Connection pooling** - MongoDB connection optimization
- **Async processing** - SQS-based asynchronous workflow
- **Resource optimization** - JRE-only runtime container
- **Health monitoring** - Spring Boot Actuator metrics

## 🛠️ Available Scripts

- `./scripts/quick-test.sh` - Comprehensive validation and testing
- `mvn clean compile` - Build application
- `mvn test` - Run unit tests
- `mvn clean package` - Build JAR file
- `docker-compose up --build` - Start with Docker

## 🤝 Contributing

1. **Follow clean architecture principles**
   - Keep domain layer pure (no external dependencies)
   - Use dependency inversion for external services
   - Implement business logic in domain entities

2. **Run validation before committing**
   ```bash
   ./scripts/quick-test.sh
   ```

3. **Add tests for new business logic**
   - Unit tests for domain logic
   - Integration tests for adapters
   - Validation scripts for architecture compliance

4. **Update documentation**
   - Update README for new features
   - Document API changes
   - Update implementation progress

## 🔍 Troubleshooting

### Common Issues

1. **Build fails with compilation errors**
   - Ensure Java 21 is installed: `java --version`
   - Check Maven configuration: `mvn --version`
   - Verify all dependencies: `mvn dependency:resolve`

2. **Docker build fails**
   - Check Docker version: `docker --version`
   - Ensure sufficient disk space
   - Verify Dockerfile syntax

3. **MongoDB connection issues**
   - Ensure MongoDB container is running: `docker-compose ps`
   - Check connection string in application.yml
   - Verify network connectivity between containers

4. **Quick test script fails**
   - Check file permissions: `chmod +x scripts/quick-test.sh`
   - Ensure all domain files are present
   - Verify clean architecture compliance

5. **Video upload fails with "Unsupported MIME type" error**
   - **Issue**: Application detects MIME type as `application/octet-stream` instead of `video/mp4`
   - **Solution**: Explicitly specify content type in curl command:
     ```bash
     curl -X POST -H "Content-Type: multipart/form-data" \
       -F "file=@test_videos/better_test_video.mp4;type=video/mp4" \
       -F "userId=test-user-123" \
       http://localhost:8080/api/videos/upload
     ```
   - **Note**: The `;type=video/mp4` part is crucial for proper MIME type detection

6. **User validation errors**
   - **Issue**: "User not found or inactive" error
   - **Solution**: Use predefined valid user IDs like `test-user-123`
   - **Note**: The application has built-in user validation for testing

### Getting Help

- Run `./scripts/quick-test.sh` for comprehensive validation
- Check application logs: `docker-compose logs processing-service`
- Verify MongoDB status: `docker-compose logs mongodb`
- Review Spring Boot Actuator endpoints for health status

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

## 🙏 Acknowledgments

- Built with Spring Boot and clean architecture principles
- Domain-driven design patterns for business logic organization
- AWS SDK for cloud service integration
- Docker for containerization and development environment
- Testing scripts inspired by rapid feedback development practices
