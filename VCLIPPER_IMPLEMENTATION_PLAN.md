# VClipper System Rearchitecture Plan - FINAL VERSION (5 Developers, 27 Days)

## Team Structure & Roles

### Team Composition
- **Infrastructure Engineer (Dev 1)**: CI/CD pipelines, Terraform, EKS cluster, monitoring, AWS services
- **Backend Developer 1 (Dev 2)**: User Profile Service + Processing Orchestration Service + API contracts
- **Backend Developer 2 (Dev 3)**: Video Processing Service + API contracts + FFmpeg integration
- **Frontend Developer (Dev 4)**: React application + AWS Amplify + Cognito integration + API contracts
- **Documentation Engineer (Dev 5)**: Event Storming/DDD, C4 diagrams, infrastructure documentation

## Application Workflow

### Expected User Journey
1. **User Access**: User navigates to the application URL (hosted on AWS Amplify)
2. **Authentication**: User is redirected to AWS Cognito for login/registration
3. **Home Page**: After successful authentication, user is redirected to the main dashboard
4. **Video Upload**: User clicks "Upload Video" button
5. **File Selection**: User browses and selects a video file from their device
6. **Upload Submission**: User clicks "Submit" to start the upload process
7. **Upload Confirmation**: System displays "Video processing has been scheduled" message
8. **Queue Processing**: Processing orchestrator sends a message to SQS queue
9. **Video Processing**: VClipping service reads SQS message and starts processing
10. **Frame Extraction**: FFmpeg extracts frames from the video and creates ZIP file
11. **Completion Notification**: SNS sends notification to user when processing completes
12. **Download Access**: User can download the processed ZIP file from the video list
13. **Error Handling**: If processing fails, user receives SNS notification with error details

### System Architecture Flow
```
Frontend (Amplify) → Cognito (Auth) → API Gateway → Processing Service → SQS → VClipping Service
                                                                              ↓
User Profile Service ← MongoDB Atlas                                    S3 (Storage)
                                                                              ↓
                                                                        SNS (Notifications)
```

## Implementation Plan - Revised

### Week 1 (Days 1-7): Foundation & Core Development

#### Dev 1 (Infrastructure) - Days 1-7
**Priority: Critical Path - Enable immediate development**

**Days 1-3: Core AWS Infrastructure**
- [ ] Setup AWS Cognito User Pool and Identity Pool
- [ ] Configure S3 buckets with proper IAM policies
- [ ] Setup SQS queues for video processing
- [ ] Setup SNS topics for user notifications
- [ ] Create MongoDB Atlas cluster
- [ ] Setup basic CloudWatch monitoring

**Days 4-7: Development Environment & CI/CD**
- [ ] Create Docker Compose for local development
- [ ] Setup GitHub Actions workflow templates
- [ ] Configure SonarCloud integration
- [ ] Setup Terraform modules structure
- [ ] Create shared configuration files

#### Dev 2 (Backend - User & Processing) - Days 1-7
**Focus: User Profile Service + API Contracts**

```java
// User Profile Service Architecture
src/main/java/com/vclipper/user/
├── application/
│   ├── usecases/
│   │   ├── GetUserProfileUseCase.java
│   │   ├── UpdateUserProfileUseCase.java
│   │   └── CreateUserProfileUseCase.java
│   └── ports/
│       └── UserProfileRepository.java
├── domain/
│   ├── entities/
│   │   └── UserProfile.java
│   └── exceptions/
│       └── UserProfileNotFoundException.java
├── infrastructure/
│   ├── adapters/
│   │   └── persistence/
│   │       ├── MongoUserProfileRepository.java
│   │       └── UserProfileDocument.java
│   └── configuration/
│       ├── MongoConfiguration.java
│       └── SecurityConfiguration.java
└── interfaces/
    ├── controllers/
    │   └── UserProfileController.java
    └── dto/
        ├── UserProfileRequest.java
        └── UserProfileResponse.java
```

**Tasks:**
- [ ] Define API contracts for User Profile Service
- [ ] Implement user profile CRUD operations
- [ ] Integrate with Cognito for JWT validation
- [ ] Write unit tests (target: 80%+ coverage)
- [ ] Create local testing script

#### Dev 3 (Backend - Video Processing) - Days 1-7
**Focus: FFmpeg Integration + Core Processing**

```java
// Video Processing Service Core
src/main/java/com/vclipper/vclipping/
├── application/
│   ├── usecases/
│   │   └── ProcessVideoUseCase.java
│   └── ports/
│       ├── VideoProcessorGateway.java
│       ├── FileStorageGateway.java
│       ├── MessageQueueGateway.java
│       └── NotificationGateway.java
├── domain/
│   ├── entities/
│   │   ├── VideoFile.java
│   │   └── ProcessingJob.java
│   ├── valueobjects/
│   │   ├── FrameExtractionSettings.java    // Predefined: 1 frame per second
│   │   └── OutputFormat.java               // Predefined: PNG + ZIP
│   └── services/
│       └── VideoProcessingDomainService.java
├── infrastructure/
│   ├── adapters/
│   │   ├── processing/
│   │   │   └── FFmpegVideoProcessor.java
│   │   ├── storage/
│   │   │   └── S3FileStorageAdapter.java
│   │   ├── messaging/
│   │   │   └── SQSMessageAdapter.java
│   │   └── notification/
│   │       └── SNSNotificationAdapter.java
│   └── configuration/
└── interfaces/
    └── listeners/
        └── VideoProcessingMessageListener.java  // SQS message consumer
```

**Frame Extraction Settings & Output Format Handling:**
- **FrameExtractionSettings**: Predefined value object with 1 frame per second extraction rate
- **OutputFormat**: Predefined value object specifying PNG format and ZIP compression
- **Location**: Domain layer as value objects, configured in application properties
- **Flexibility**: Can be made configurable later if needed

**VideoProcessingMessageListener Purpose:**
- Listens to SQS messages sent by the Processing Orchestration Service
- Triggers video processing when a new message arrives
- Handles message acknowledgment and error scenarios
- Manages processing job lifecycle

**Tasks:**
- [ ] Define API contracts for Video Processing Service
- [ ] **PRIORITY**: Complete FFmpeg video processing pipeline (Days 1-3)
- [ ] Implement S3 file handling
- [ ] Setup SQS message consumption
- [ ] Integrate SNS for notifications
- [ ] Write unit tests for processing logic
- [ ] Create local testing script

#### Dev 4 (Frontend) - Days 1-7
**Focus: React Application + Early Amplify Deployment**

```typescript
// React Application Structure
src/
├── components/
│   ├── auth/
│   │   └── AuthWrapper.tsx
│   ├── video/
│   │   ├── VideoUpload.tsx
│   │   ├── VideoList.tsx
│   │   ├── VideoStatus.tsx
│   │   └── VideoDownload.tsx        // Download functionality integrated in VideoList
│   └── common/
│       ├── Layout.tsx
│       └── LoadingSpinner.tsx
├── hooks/
│   ├── useAuth.ts
│   ├── useVideoUpload.ts
│   └── useVideoStatus.ts
├── services/
│   ├── api.ts
│   ├── cognitoService.ts
│   └── videoService.ts
├── store/
│   ├── authSlice.ts
│   └── videoSlice.ts
└── App.tsx
```

**VideoDownload Handling:**
- **Integration**: Download functionality is integrated within VideoList component
- **Approach**: Each video item in the list has a download button/link
- **Implementation**: Uses presigned S3 URLs for secure downloads
- **UX**: Eliminates need for separate download page, streamlines user experience

**Tasks:**
- [ ] Define API contracts for Frontend-Backend integration
- [ ] Setup React app with TypeScript
- [ ] Integrate AWS Amplify and Cognito
- [ ] **PRIORITY**: Deploy to Amplify (Days 3-5)
- [ ] Create authentication components
- [ ] Setup Redux Toolkit for state management
- [ ] Create video upload interface with download integration

#### Dev 5 (Documentation) - Days 1-7
**Focus: Architecture Documentation**

**Tasks:**
- [ ] Create Event Storming documentation
- [ ] Develop DDD (Domain-Driven Design) documentation
- [ ] Create C4 architecture diagrams (Context, Container, Component, Code)
- [ ] Document infrastructure architecture
- [ ] Create system integration documentation

### Week 2 (Days 8-14): Service Completion & Integration

#### Dev 1 (Infrastructure) - Days 8-14
**Focus: Production Infrastructure & Monitoring**

- [ ] Setup EKS cluster with node groups
- [ ] Configure Application Load Balancer
- [ ] Setup Prometheus + Grafana containers
- [ ] Create Terraform modules for all environments
- [ ] Configure AWS API Gateway
- [ ] Complete CI/CD pipelines for all services

#### Dev 2 (Backend) - Days 8-14
**Focus: Processing Orchestration Service**

```java
// Processing Orchestration Service
src/main/java/com/vclipper/processing/
├── application/
│   ├── usecases/
│   │   ├── SubmitVideoProcessingUseCase.java
│   │   ├── GetProcessingStatusUseCase.java
│   │   └── ListUserVideosUseCase.java
│   └── ports/
│       ├── VideoRepository.java
│       ├── FileStorageGateway.java
│       ├── MessageQueueGateway.java
│       └── NotificationGateway.java
├── domain/
│   ├── entities/
│   │   ├── VideoProcessingRequest.java
│   │   └── ProcessingStatus.java
│   └── enums/
│       └── ProcessingStatusEnum.java
├── infrastructure/
│   ├── adapters/
│   │   ├── persistence/
│   │   ├── storage/
│   │   │   └── S3FileStorageAdapter.java
│   │   ├── messaging/
│   │   │   └── SQSMessageAdapter.java
│   │   └── notification/
│   │       └── SNSNotificationAdapter.java
│   └── configuration/
└── interfaces/
    ├── controllers/
    │   ├── VideoProcessingController.java
    │   └── VideoStatusController.java
    └── dto/
```

**Tasks:**
- [ ] Implement video upload and validation
- [ ] Create processing status tracking
- [ ] Integrate with SQS for job queuing
- [ ] Integrate SNS for user notifications
- [ ] Implement file download functionality
- [ ] Complete integration with user profile service
- [ ] Create local testing script

#### Dev 3 (Backend) - Days 8-14
**Focus: Video Processing Service Completion**

**Tasks:**
- [ ] Complete error handling and recovery
- [ ] Add progress reporting via SNS
- [ ] Optimize performance and memory usage
- [ ] Complete integration tests
- [ ] Enhance local testing script

#### Dev 4 (Frontend) - Days 8-14
**Focus: Complete React Application**

**Tasks:**
- [ ] Complete video upload with progress tracking
- [ ] Implement video status monitoring
- [ ] Add integrated download functionality in video list
- [ ] Create responsive UI design
- [ ] Implement error handling and notifications
- [ ] Handle SNS notifications display

#### Dev 5 (Documentation) - Days 8-14
**Focus: Complete Architecture Documentation**

- [ ] Finalize Event Storming documentation
- [ ] Complete DDD documentation
- [ ] Finalize C4 diagrams
- [ ] Complete infrastructure documentation
- [ ] Document notification flows (SNS integration)

### Week 3 (Days 15-21): Integration & Testing

#### Dev 1 (Infrastructure) - Days 15-21
**Focus: Production Deployment**

- [ ] Deploy all services to production environment
- [ ] Configure production monitoring and alerting
- [ ] Setup log aggregation and analysis
- [ ] Perform load testing and optimization

#### Dev 2 & 3 (Backend) - Days 15-21
**Focus: Integration and Testing**

**Joint Tasks:**
- [ ] Complete service-to-service integration
- [ ] Perform end-to-end testing
- [ ] Implement comprehensive error handling
- [ ] Test SNS notification flows
- [ ] Enhance local testing scripts

#### Dev 4 (Frontend) - Days 15-21
**Focus: Production Optimization**

- [ ] Configure CloudFront distribution
- [ ] Implement performance optimizations
- [ ] Test SNS notification integration
- [ ] Complete responsive design

#### Dev 5 (Documentation) - Days 15-21
**Focus: Final Documentation**

- [ ] Complete all architecture documentation
- [ ] Document testing procedures
- [ ] Create operational guides

### Final Week (Days 22-27): Final Testing & Launch

#### All Team Members - Days 22-27
**Focus: Final integration and launch preparation**

**Shared Tasks:**
- [ ] Complete end-to-end system testing
- [ ] Perform performance and load testing
- [ ] Finalize all documentation
- [ ] Prepare launch presentation and demo

## Local Testing Scripts

### User Profile Service Testing Script
```bash
#!/bin/bash
# test-user-service.sh

echo "Testing User Profile Service..."

# Health check
curl -X GET http://localhost:8081/actuator/health

# Create user profile
curl -X POST http://localhost:8081/api/users/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -d '{"name":"Test User","email":"test@example.com"}'

# Get user profile
curl -X GET http://localhost:8081/api/users/profile \
  -H "Authorization: Bearer $JWT_TOKEN"

echo "User service tests completed"
```

### Processing Orchestration Service Testing Script
```bash
#!/bin/bash
# test-processing-service.sh

echo "Testing Processing Orchestration Service..."

# Health check
curl -X GET http://localhost:8082/actuator/health

# Upload video
curl -X POST http://localhost:8082/api/videos/upload \
  -H "Authorization: Bearer $JWT_TOKEN" \
  -F "video=@test-video.mp4"

# Check processing status
curl -X GET http://localhost:8082/api/videos/status/VIDEO_ID \
  -H "Authorization: Bearer $JWT_TOKEN"

# List user videos
curl -X GET http://localhost:8082/api/videos \
  -H "Authorization: Bearer $JWT_TOKEN"

echo "Processing service tests completed"
```

### Video Processing Service Testing Script
```bash
#!/bin/bash
# test-vclipping-service.sh

echo "Testing Video Processing Service..."

# Health check
curl -X GET http://localhost:8083/actuator/health

# Send test message to SQS (simulates processing trigger)
aws sqs send-message \
  --queue-url $SQS_QUEUE_URL \
  --message-body '{"videoId":"test-123","s3Key":"videos/test-video.mp4","userId":"user-123"}'

# Check processing logs
docker logs vclipper-vclipping

echo "VClipping service tests completed"
```

## SNS Notification Integration

### Notification Scenarios
1. **Processing Started**: User receives notification when video processing begins
2. **Processing Completed**: User receives notification with download link when processing succeeds
3. **Processing Failed**: User receives notification with error details when processing fails
4. **Upload Confirmed**: User receives notification confirming video upload and queuing

### SNS Architecture Integration
```
Processing Service → SNS Topic → User Email/SMS
VClipping Service → SNS Topic → User Email/SMS
```

### Implementation Points
- **Processing Service**: Sends notifications for upload confirmation and status updates
- **VClipping Service**: Sends notifications for processing completion/failure
- **Frontend**: Displays notification history and preferences
- **Configuration**: SNS topics configured per environment (dev/prod)

## Integration Points

- **Day 7**: Infrastructure ready, basic services functional, frontend deployed to Amplify
- **Day 14**: All services integrated, end-to-end workflow functional
- **Day 21**: Production-ready system with monitoring
- **Day 27**: Final testing and launch preparation complete

## Success Metrics by Team Member

### Dev 1 (Infrastructure)
- [ ] All AWS services configured and accessible
- [ ] CI/CD pipelines functional for all repositories
- [ ] Monitoring and alerting operational
- [ ] Production environment ready

### Dev 2 (Backend - User/Processing)
- [ ] User profile service: 80%+ test coverage
- [ ] Processing service: 80%+ test coverage
- [ ] SonarCloud quality gates passed
- [ ] Local testing scripts functional

### Dev 3 (Backend - Video Processing)
- [ ] Video processing service: 80%+ test coverage
- [ ] FFmpeg integration working with multiple formats
- [ ] SNS notifications implemented
- [ ] Local testing scripts functional

### Dev 4 (Frontend)
- [ ] React application deployed on Amplify
- [ ] Cognito authentication integrated
- [ ] All user workflows functional
- [ ] SNS notification display implemented

### Dev 5 (Documentation)
- [ ] Event Storming documentation complete
- [ ] DDD documentation complete
- [ ] C4 architecture diagrams complete
- [ ] Infrastructure documentation complete

This revised plan addresses all your feedback points and provides a more focused, practical approach to delivering the VClipper system within the 27-day timeline.
