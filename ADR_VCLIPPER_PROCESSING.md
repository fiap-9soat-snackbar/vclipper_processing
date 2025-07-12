# Architecture Decision Records - VClipper Processing Service

## Service Overview
**Service Name:** vclipper_processing  
**Purpose:** Video processing orchestration and workflow management service  
**Architecture Pattern:** Clean Architecture with Hexagonal Architecture principles  

---

## ADR-001: Programming Language and Runtime
**Decision:** Java 21 with Spring Boot 3.4.1  
**Status:** Accepted  
**Context:** Need for enterprise-grade video processing orchestration service  
**Decision Rationale:**
- Java 21 provides latest LTS features including virtual threads for better concurrency
- Spring Boot 3.4.1 offers mature ecosystem for microservices
- Strong AWS SDK integration available
- Excellent tooling and monitoring capabilities
- Team expertise in Java ecosystem

**Consequences:**
- ✅ Mature ecosystem and extensive library support
- ✅ Strong AWS integration through Spring Cloud AWS
- ✅ Excellent observability and monitoring tools
- ❌ Higher memory footprint compared to Go/Node.js
- ❌ Longer startup times

---

## ADR-002: Architecture Pattern
**Decision:** Clean Architecture with Hexagonal Architecture (Ports & Adapters)  
**Status:** Accepted  
**Context:** Need for maintainable, testable, and technology-agnostic business logic  
**Decision Rationale:**
- Clear separation of concerns between business logic and infrastructure
- Testability through dependency inversion
- Technology independence for core business rules
- Scalable and maintainable codebase structure

**Architecture Layers:**
```
├── domain/                 # Business entities, events, exceptions
├── application/           # Use cases, ports, business logic
│   ├── usecases/         # Business use cases
│   ├── ports/            # Interface definitions
│   └── common/           # Shared application logic
└── infrastructure/        # External adapters, controllers, persistence
    ├── adapters/         # External service adapters
    ├── controllers/      # REST API controllers
    └── config/           # Configuration classes
```

**Consequences:**
- ✅ High testability and maintainability
- ✅ Technology independence
- ✅ Clear separation of concerns
- ❌ Initial complexity overhead
- ❌ More boilerplate code

---

## ADR-003: Database Technology
**Decision:** MongoDB as primary database  
**Status:** Accepted  
**Context:** Need for flexible document storage for video metadata and processing status  
**Decision Rationale:**
- Flexible schema for evolving video metadata requirements
- Good performance for read-heavy workloads (status queries)
- Native JSON support aligns with REST API responses
- Horizontal scaling capabilities
- Spring Data MongoDB integration

**Configuration:**
```yaml
spring:
  data:
    mongodb:
      uri: mongodb://${MONGODB_USER}:${MONGODB_PASSWORD}@${DB_HOST}:${DB_PORT}/${MONGODB_DATABASE}
      database: ${MONGODB_DATABASE}
```

**Consequences:**
- ✅ Flexible schema evolution
- ✅ Good read performance for status queries
- ✅ Easy JSON serialization
- ❌ No ACID transactions across documents
- ❌ Eventual consistency challenges

---

## ADR-004: AWS Cloud Services Integration
**Decision:** AWS-native services with Spring Cloud AWS  
**Status:** Accepted  
**Context:** Cloud-native video processing with scalable storage and messaging  

**AWS Services Used:**
- **S3:** Video file storage and processed output storage
- **SQS:** Asynchronous message queuing for processing workflows
- **SNS:** User notifications for processing status updates
- **Spring Cloud AWS:** Integration framework

**Configuration:**
```yaml
aws:
  region: ${AWS_REGION}
  s3:
    bucket-name: ${AWS_S3_BUCKET}
  sqs:
    processing-queue-url: ${AWS_SQS_PROCESSING_QUEUE_URL}
    result-queue-url: ${AWS_SQS_RESULT_QUEUE_URL}
  sns:
    notification-topic-arn: ${AWS_SNS_NOTIFICATION_TOPIC_ARN}
```

**Consequences:**
- ✅ Scalable cloud-native architecture
- ✅ Managed services reduce operational overhead
- ✅ Built-in reliability and durability
- ❌ Vendor lock-in to AWS
- ❌ Potential cost implications at scale

---

## ADR-005: Messaging and Communication Patterns
**Decision:** Asynchronous messaging with SQS and event-driven architecture  
**Status:** Accepted  
**Context:** Need for reliable, scalable inter-service communication  

**Messaging Patterns:**
- **Command Pattern:** Video processing requests via SQS
- **Event Pattern:** Domain events for status changes
- **Request-Reply:** Synchronous REST for status queries

**Domain Events:**
- `VideoUploadedEvent`
- `ProcessingStartedEvent`
- `ProcessingCompletedEvent`
- `ProcessingFailedEvent`

**Consequences:**
- ✅ Loose coupling between services
- ✅ Scalable and resilient communication
- ✅ Event-driven architecture benefits
- ❌ Eventual consistency challenges
- ❌ Increased complexity in error handling

---

## ADR-006: Error Handling Strategy
**Decision:** Comprehensive exception hierarchy with Result pattern  
**Status:** Accepted  
**Context:** Need for robust error handling in video processing workflows  

**Exception Hierarchy:**
```java
domain/exceptions/
├── VideoProcessingException (base)
├── VideoNotFoundException
├── InvalidVideoFormatException
├── VideoUploadException
├── ProcessingStatusException
└── VideoNotReadyException
```

**Result Pattern Implementation:**
```java
public class Result<T, E> {
    // Success/failure wrapper for use case responses
}
```

**Global Exception Handler:**
- Centralized error handling in `GlobalExceptionHandler`
- Structured error responses
- Proper HTTP status code mapping

**Consequences:**
- ✅ Consistent error handling across the application
- ✅ Clear error communication to clients
- ✅ Separation of business errors from technical errors
- ❌ Additional complexity in error flow handling

---

## ADR-007: File Upload and Processing Constraints
**Decision:** Configurable file size limits and format restrictions  
**Status:** Accepted  
**Context:** Need to balance functionality with resource constraints  

**Constraints Defined:**
```yaml
spring:
  servlet:
    multipart:
      max-file-size: ${MAX_FILE_SIZE}      # e.g., 100MB
      max-request-size: ${MAX_REQUEST_SIZE} # e.g., 105MB

vclipper:
  processing:
    video:
      max-size-bytes: ${VCLIPPER_MAX_FILE_SIZE_BYTES}  # e.g., 104857600 (100MB)
      allowed-formats: ${VCLIPPER_ALLOWED_FORMATS}      # e.g., mp4,avi,mov,mkv
```

**Supported Video Formats:**
- MP4, AVI, MOV, MKV, WMV, FLV, WEBM

**Consequences:**
- ✅ Prevents resource exhaustion
- ✅ Predictable processing times
- ✅ Cost control for cloud storage
- ❌ Limits user flexibility
- ❌ May require format conversion for some users

---

## ADR-008: Retry and Resilience Strategy
**Decision:** Configurable retry mechanism with exponential backoff  
**Status:** Accepted  
**Context:** Need for resilient processing in distributed environment  

**Retry Configuration:**
```yaml
vclipper:
  processing:
    retry:
      max-attempts: ${VCLIPPER_RETRY_MAX_ATTEMPTS}      # e.g., 3
      initial-delay-seconds: ${VCLIPPER_RETRY_INITIAL_DELAY}  # e.g., 5
      max-delay-seconds: ${VCLIPPER_RETRY_MAX_DELAY}    # e.g., 60
```

**Retry Scenarios:**
- Network failures to AWS services
- Temporary processing service unavailability
- Database connection issues

**Consequences:**
- ✅ Improved system resilience
- ✅ Better user experience during transient failures
- ✅ Configurable retry behavior
- ❌ Potential for increased latency
- ❌ Risk of cascading failures if not properly configured

---

## ADR-009: Security and Authentication
**Decision:** Mock authentication with placeholder for production integration  
**Status:** Accepted (Development), Needs Implementation (Production)  
**Context:** Focus on core functionality with authentication integration planned  

**Current Implementation:**
- `MockUserServiceAdapter` for development
- User ID passed in requests for processing context
- No actual authentication validation

**Production Requirements:**
- JWT token validation
- Integration with AWS Cognito or similar
- Role-based access control

**Consequences:**
- ✅ Rapid development and testing
- ✅ Clear separation of concerns
- ❌ Security vulnerability in current state
- ❌ Requires production implementation

---

## ADR-010: Monitoring and Observability
**Decision:** Spring Boot Actuator with health checks and metrics  
**Status:** Accepted  
**Context:** Need for operational visibility and health monitoring  

**Monitoring Configuration:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics
  endpoint:
    health:
      show-details: always
```

**Observability Features:**
- Health checks for dependencies (MongoDB, AWS services)
- Application metrics via Micrometer
- Structured logging with configurable levels
- Info endpoint with application metadata

**Consequences:**
- ✅ Operational visibility
- ✅ Proactive issue detection
- ✅ Integration with monitoring tools (Prometheus, etc.)
- ❌ Additional overhead
- ❌ Potential security exposure if not properly secured

---

## ADR-011: Testing Strategy
**Decision:** Multi-layer testing with mocked external dependencies  
**Status:** Accepted  
**Context:** Need for comprehensive testing without external service dependencies  

**Testing Layers:**
- **Unit Tests:** Domain logic and use cases
- **Integration Tests:** Repository and adapter layers
- **Contract Tests:** API endpoint testing
- **Mock Strategy:** All external services (AWS, MongoDB) mocked

**Test Configuration:**
- JUnit 5 for test framework
- Mockito for mocking
- TestContainers for integration testing (when needed)
- Separate test profiles

**Consequences:**
- ✅ Fast, reliable test execution
- ✅ No external dependencies for testing
- ✅ Clear test isolation
- ❌ May miss integration issues
- ❌ Requires careful mock maintenance

---

## ADR-012: Configuration Management
**Decision:** Environment-based configuration with Spring profiles  
**Status:** Accepted  
**Context:** Need for flexible configuration across environments  

**Configuration Strategy:**
- Environment variables for all configurable values
- `.env` files for local development
- Spring profiles for environment-specific settings
- Externalized configuration for sensitive values

**Key Configuration Areas:**
- Database connection settings
- AWS service configurations
- File upload limits
- Retry and timeout settings
- Logging levels

**Consequences:**
- ✅ Environment-specific flexibility
- ✅ Security through externalized secrets
- ✅ Easy deployment across environments
- ❌ Configuration complexity
- ❌ Potential for configuration drift

---

## Summary of Key Architectural Decisions

1. **Java 21 + Spring Boot 3.4.1** for enterprise-grade microservice
2. **Clean Architecture** with hexagonal pattern for maintainability
3. **MongoDB** for flexible document storage
4. **AWS-native services** (S3, SQS, SNS) for cloud scalability
5. **Event-driven architecture** with asynchronous messaging
6. **Comprehensive error handling** with Result pattern
7. **Configurable constraints** for file size and format limitations
8. **Retry mechanisms** with exponential backoff for resilience
9. **Mock authentication** (development) with production integration planned
10. **Spring Boot Actuator** for monitoring and health checks
11. **Multi-layer testing** with mocked external dependencies
12. **Environment-based configuration** for deployment flexibility
