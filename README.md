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

### **Phase 3: Application Layer** (In Progress)
- [ ] Port interfaces for external dependencies
- [ ] Use cases for business orchestration
- [ ] Business logic coordination

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
│   └── controllers/       # REST API controllers
└── ProcessingOrchestrationApplication.java
```

## 🌐 Available Endpoints

### Health & Monitoring (Current)
- `GET /actuator/health` - Health check with MongoDB status
- `GET /actuator/info` - Application information with build details
- `GET /actuator/metrics` - Application metrics

### Video Processing Endpoints (Planned)
- `POST /api/videos/upload` - Upload video for processing
- `GET /api/videos/{videoId}/status` - Get processing status
- `GET /api/videos/{videoId}/download` - Get download URL
- `GET /api/videos` - List user's videos
- `PUT /api/videos/{videoId}/status` - Update processing status (internal)

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

### Current Test Results
- ✅ **Build Status**: All 17 source files compile successfully
- ✅ **Architecture Compliance**: No infrastructure dependencies in domain
- ✅ **Business Logic**: All transition rules and validation implemented
- ✅ **Package Structure**: Proper clean architecture organization

### Testing Philosophy
- **Fast Feedback Loop** - Quick validation (< 30 seconds)
- **Clean Architecture Validation** - Automatic compliance checking
- **Business Logic Testing** - Domain rules verification
- **Regression Detection** - Catches breaking changes

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
