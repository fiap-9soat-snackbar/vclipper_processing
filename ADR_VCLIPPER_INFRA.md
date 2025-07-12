# Architecture Decision Records - VClipper Infrastructure (vclipper_infra)

## Infrastructure Overview
**Repository:** vclipper_infra  
**Purpose:** Infrastructure as Code (IaC) for VClipper video processing platform  
**Technology:** Terraform with AWS Provider  
**Architecture Pattern:** Microservices with AWS-native services  

---

## ADR-001: Infrastructure as Code Technology
**Decision:** Terraform with AWS Provider  
**Status:** Accepted  
**Context:** Need for reproducible, version-controlled infrastructure deployment  
**Decision Rationale:**
- Terraform provides declarative infrastructure management
- AWS Provider offers comprehensive service coverage
- State management enables team collaboration
- Modular structure supports microservices architecture
- Educational environment compatibility (LabRole constraints)

**Project Structure:**
```
vclipper_infra/
├── global/                    # Global shared configuration
├── services/                  # Service-specific infrastructure
│   ├── cognito/              # Authentication service
│   ├── api-gateway/          # API Gateway (with limitations)
│   ├── compute/eks/          # Kubernetes cluster
│   ├── video-storage/        # S3 buckets for video storage
│   ├── sqs/                  # Message queuing
│   ├── sns-notifications/    # Notification service
│   ├── monitoring/           # CloudWatch monitoring
│   └── frontend-hosting/     # Static website hosting
└── terraform-state-bucket.tf # Remote state management
```

**Consequences:**
- ✅ Reproducible infrastructure deployments
- ✅ Version-controlled infrastructure changes
- ✅ Modular and maintainable code structure
- ❌ Learning curve for team members
- ❌ State management complexity

---

## ADR-002: Authentication Architecture
**Decision:** AWS Cognito User Pool with JWT tokens  
**Status:** Accepted  
**Context:** Need for secure user authentication and authorization  
**Decision Rationale:**
- AWS Cognito provides managed authentication service
- JWT tokens enable stateless authentication
- Integration with frontend React application
- Advanced security features (MFA, password policies)
- Reduced operational overhead

**Cognito Configuration:**
```hcl
resource "aws_cognito_user_pool" "main" {
  name = "${var.user_pool_name}-${environment}"
  
  password_policy {
    minimum_length        = 8
    require_lowercase     = true
    require_numbers       = true
    require_symbols       = true
    require_uppercase     = true
    password_history_size = 24
  }
  
  auto_verified_attributes = ["email"]
  mfa_configuration = "OPTIONAL"
  
  user_pool_add_ons {
    advanced_security_mode = "ENFORCED"
  }
}
```

**Authentication Flow:**
1. User authenticates via Cognito in React frontend
2. Frontend receives JWT tokens (access, ID, refresh)
3. Frontend passes X-User-Id header to backend services
4. Backend services validate user context (mock implementation)

**Consequences:**
- ✅ Managed authentication service
- ✅ Advanced security features
- ✅ Scalable user management
- ❌ AWS vendor lock-in
- ❌ Complex token management

---

## ADR-003: API Gateway Decision and Payload Limitations
**Decision:** API Gateway configured but bypassed for large file operations  
**Status:** Accepted (with constraints)  
**Context:** API Gateway 10MB payload limit conflicts with video upload requirements  

**Problem Identified:**
- API Gateway has hard 10MB payload limit
- Video files typically exceed 20MB+ in size
- Upload/download operations require direct backend access
- Time constraints prevented dual-routing architecture

**Current Architecture:**
```hcl
resource "aws_apigatewayv2_api" "vclipper_api" {
  name          = "vclipper-api"
  protocol_type = "HTTP"
  
  cors_configuration {
    allow_credentials = true
    allow_headers     = ["*"]
    allow_methods     = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allow_origins     = [frontend_urls]
  }
}

resource "aws_apigatewayv2_authorizer" "cognito_jwt" {
  api_id           = aws_apigatewayv2_api.vclipper_api.id
  authorizer_type  = "JWT"
  identity_sources = ["$request.header.Authorization"]
  
  jwt_configuration {
    audience = [cognito_user_pool_client_id]
    issuer   = cognito_issuer_url
  }
}
```

**Workaround Implemented:**
- Frontend communicates directly with backend services via ALB/EKS
- X-User-Id header passed for authentication context
- API Gateway reserved for future non-file operations

**Alternative Considered (Not Implemented):**
- Dual routing: API Gateway for small payloads, ALB for file operations
- Pre-signed S3 URLs for direct uploads
- Lambda functions for file processing triggers

**Consequences:**
- ✅ Supports large file uploads/downloads
- ✅ Reduced latency for file operations
- ✅ Simplified authentication flow
- ❌ Bypasses API Gateway benefits (throttling, monitoring)
- ❌ Direct backend exposure
- ❌ Technical debt for future refactoring

---

## ADR-004: Container Orchestration Platform
**Decision:** Amazon EKS (Elastic Kubernetes Service) with Helm-based deployments  
**Status:** Accepted  
**Context:** Need for scalable container orchestration for microservices with video processing requirements  

**EKS Configuration:**
```hcl
module "eks_vclipper" {
  source          = "terraform-aws-modules/eks/aws"
  version         = "18.27.1"
  cluster_name    = "vclipper-cluster"
  cluster_version = "1.32"
  
  vpc_id     = vpc_id
  subnet_ids = private_subnets
  
  cluster_endpoint_public_access  = true
  cluster_endpoint_private_access = true
  
  # AWS Academy LabRole constraints - cannot create custom IAM roles
  create_iam_role = false
  enable_irsa     = false
  iam_role_arn    = "arn:aws:iam::${account_id}:role/LabRole"
  
  eks_managed_node_groups = {
    application = {
      name           = "ng-vclipper-app"
      min_size       = 1
      max_size       = 3
      desired_size   = 1
      instance_types = ["t3.large"]  # Sized for video processing memory requirements
      
      create_iam_role = false
      iam_role_arn    = "arn:aws:iam::${account_id}:role/LabRole"
    }
  }
}
```

**Instance Sizing Decision:**
- **Original Plan:** Smaller instances (t3.small/medium) for cost optimization
- **Actual Decision:** t3.large instances required
- **Constraint:** VClipping service requires minimum 1.5GB RAM for video processing
- **Impact:** Higher infrastructure costs but necessary for functionality

**Deployment Strategy:**
- **Helm Charts:** Used for deploying all application containers
  - 2 Backend services (vclipper_processing, vclipping)
  - 1 Frontend service (vclipper_fe)
- **Fixed Resource Allocation:** Predefined container and EC2 instance counts
- **Container Management:** Kubernetes handles container lifecycle and scaling

**Auto-scaling Configuration:**
- CPU-based scaling policies (80% threshold)
- CloudWatch alarms for scale up/down decisions
- Managed node groups for simplified operations
- Resource constraints driven by video processing workload

**Consequences:**
- ✅ Managed Kubernetes service
- ✅ Helm-based deployment standardization
- ✅ High availability and resilience
- ✅ Meets video processing memory requirements
- ❌ Higher costs due to t3.large instance requirement
- ❌ AWS Academy LabRole limitations
- ❌ Complexity overhead for educational project

---

## ADR-005: Video Storage Architecture
**Decision:** Amazon S3 with lifecycle policies and CORS configuration  
**Status:** Accepted  
**Context:** Need for scalable, durable video file storage with web access  

**S3 Bucket Configuration:**
```hcl
resource "aws_s3_bucket" "video_storage" {
  bucket = "vclipper-video-storage-${environment}"
}

# Security configuration
resource "aws_s3_bucket_public_access_block" "video_storage" {
  bucket = aws_s3_bucket.video_storage.id
  
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

# Lifecycle management
resource "aws_s3_bucket_lifecycle_configuration" "video_storage" {
  bucket = aws_s3_bucket.video_storage.id
  
  rule {
    id     = "video-cleanup"
    status = "Enabled"
    filter { prefix = "videos/" }
    expiration { days = var.video_retention_days }
  }
  
  rule {
    id     = "clips-cleanup"
    status = "Enabled"
    filter { prefix = "clips/" }
    expiration { days = var.images_retention_days }
  }
  
  rule {
    id     = "temp-cleanup"
    status = "Enabled"
    filter { prefix = "temp/" }
    expiration { days = 1 }
  }
}
```

**Storage Structure:**
- `videos/` - Original uploaded video files
- `clips/` - Generated image clips/frames
- `temp/` - Temporary processing files (1-day retention)

**CORS Configuration:**
- Supports direct browser uploads via pre-signed URLs
- Configured for frontend domain origins
- Enables PUT, POST, GET, DELETE operations

**Consequences:**
- ✅ Scalable and durable storage
- ✅ Automatic lifecycle management
- ✅ Cost optimization through retention policies
- ✅ Direct browser upload capability
- ❌ Storage costs for large video files
- ❌ Data transfer costs

---

## ADR-006: Message Queue Architecture
**Decision:** Amazon SQS for asynchronous processing workflows  
**Status:** Accepted  
**Context:** Need for reliable, scalable message queuing between services  

**SQS Configuration:**
- **Processing Queue:** Video processing job requests
- **Result Queue:** Processing completion notifications
- **Dead Letter Queues:** Failed message handling
- **FIFO Queues:** Where order matters (status updates)

**Message Flow:**
1. Video upload triggers SQS message to processing queue
2. VClipping service consumes processing messages
3. Processing results sent to result queue
4. Processing service consumes results for status updates

**Queue Characteristics:**
- Visibility timeout: 300 seconds (5 minutes)
- Message retention: 14 days
- Dead letter queue after 3 failed attempts
- Server-side encryption enabled

**Consequences:**
- ✅ Decoupled service communication
- ✅ Reliable message delivery
- ✅ Built-in retry mechanisms
- ✅ Scalable message processing
- ❌ Eventual consistency
- ❌ Message ordering complexity

---

## ADR-007: Notification System
**Decision:** Amazon SNS for user notifications  
**Status:** Accepted  
**Context:** Need for multi-channel user notifications (email, SMS, etc.)  

**SNS Topics:**
- **Processing Status:** Video processing completion/failure
- **System Alerts:** Infrastructure and application alerts
- **User Notifications:** Account and processing updates

**Notification Channels:**
- Email notifications for processing status
- Future: SMS, push notifications
- Integration with CloudWatch for system alerts

**Consequences:**
- ✅ Multi-channel notification support
- ✅ Scalable message delivery
- ✅ Integration with other AWS services
- ❌ Limited customization options
- ❌ Potential delivery delays

---

## ADR-008: Monitoring and Observability
**Decision:** Dual monitoring stack - CloudWatch + Prometheus/Grafana  
**Status:** Accepted  
**Context:** Need for comprehensive system monitoring with both AWS-native and application-level observability  

**Monitoring Architecture:**
1. **AWS CloudWatch (Infrastructure Level):**
   - Custom dashboards for AWS service metrics
   - Log groups for application and infrastructure logs (12+ groups)
   - Metric alarms for proactive alerting (21+ alarms)
   - EKS cluster and node monitoring

2. **Prometheus + Grafana (Application Level):**
   - Deployed as containers within EKS cluster
   - Application-specific metrics collection
   - Custom dashboards for business metrics
   - Service-level monitoring and alerting

**CloudWatch Components:**
- **Dashboards:** System metrics visualization
- **Log Groups:** Application and infrastructure logs
- **Metric Alarms:** Proactive alerting
- **Custom Metrics:** AWS service monitoring

**Prometheus/Grafana Stack:**
- **Prometheus:** Metrics collection and storage
- **Grafana:** Visualization and dashboards
- **Container Deployment:** Running within EKS for application monitoring
- **Service Discovery:** Kubernetes-native service discovery

**Key Metrics Monitored:**
- EKS cluster health and resource utilization
- SQS queue depth and processing rates
- S3 storage usage and request metrics
- Application performance metrics (via Prometheus)
- Video processing metrics and duration
- Error rates and response times

**Alerting Strategy:**
- CPU utilization thresholds for auto-scaling
- Queue depth alerts for processing bottlenecks
- Error rate alerts for application issues
- Storage usage alerts for cost management
- Custom application alerts via Grafana

**Consequences:**
- ✅ Comprehensive dual-layer monitoring
- ✅ AWS-native and application-specific visibility
- ✅ Proactive issue detection
- ✅ Performance optimization insights
- ❌ Increased monitoring complexity
- ❌ Additional resource overhead for Prometheus/Grafana
- ❌ Potential monitoring costs

---

## ADR-009: Frontend Hosting Strategy
**Decision:** S3 Static Website Hosting (CloudFront unavailable)  
**Status:** Accepted  
**Context:** Need for scalable, cost-effective frontend hosting within AWS Academy constraints  

**AWS Academy Limitations:**
- **CloudFront:** Unavailable due to insufficient IAM privileges in lab environment
- **AWS Amplify:** Unavailable due to IAM privilege restrictions
- **CDN Alternative:** Direct S3 static hosting without global distribution

**Hosting Configuration:**
```hcl
resource "aws_s3_bucket" "frontend_hosting" {
  bucket = "vclipper-frontend-${environment}"
}

resource "aws_s3_bucket_website_configuration" "frontend_hosting" {
  bucket = aws_s3_bucket.frontend_hosting.id
  
  index_document {
    suffix = "index.html"
  }
  
  error_document {
    key = "error.html"
  }
}

resource "aws_s3_bucket_cors_configuration" "frontend_hosting" {
  bucket = aws_s3_bucket.frontend_hosting.id
  
  cors_rule {
    allowed_headers = ["*"]
    allowed_methods = ["GET", "HEAD"]
    allowed_origins = ["*"]
    max_age_seconds = 3000
  }
}
```

**Deployment Strategy:**
- React build artifacts deployed directly to S3
- Environment-specific S3 buckets
- Versioned deployments for rollback capability
- CORS configuration for backend API communication

**Production vs. Lab Environment:**
- **Lab Environment:** S3-only hosting due to service restrictions
- **Production Recommendation:** CloudFront + S3 for global CDN distribution
- **Security Trade-off:** Direct S3 access instead of CDN security features

**Consequences:**
- ✅ Cost-effective static hosting within constraints
- ✅ High availability and scalability
- ✅ Simple deployment process
- ❌ No global CDN distribution
- ❌ Limited performance optimization
- ❌ Missing CloudFront security features
- ❌ CORS complexity for API calls

---

## ADR-010: Database Strategy
**Decision:** MongoDB Atlas (managed) for application data  
**Status:** Accepted  
**Context:** Need for flexible document storage for video metadata  

**Database Configuration:**
- MongoDB Atlas managed service
- Document-based storage for video metadata
- Flexible schema for evolving requirements
- Connection via connection string with authentication

**Data Models:**
- Video processing requests and status
- User session information
- Processing results and metadata

**Consequences:**
- ✅ Managed database service
- ✅ Flexible schema evolution
- ✅ Good performance for read-heavy workloads
- ❌ Additional service dependency
- ❌ NoSQL learning curve

---

## ADR-011: Security Architecture
**Decision:** Multi-layered security with AWS-native services (constrained by lab environment)  
**Status:** Accepted  
**Context:** Need for comprehensive security within AWS Academy lab limitations  

**AWS Academy Security Constraints:**
- **IAM Roles:** Limited to single LabRole across all services
- **GuardDuty:** Not available in lab environment (production recommendation)
- **CloudTrail:** Not available in lab environment (production recommendation)
- **Custom IAM Policies:** Cannot create custom roles or policies

**Security Layers Implemented:**
1. **Network Security:** VPC with private subnets, security groups
2. **Identity & Access:** Cognito for users, LabRole for all services
3. **Data Security:** S3 encryption, secure database connections
4. **Application Security:** CORS policies, input validation
5. **Container Security:** EKS security groups and network policies

**Security Groups:**
- EKS cluster security group (restricted access)
- Node group security group (internal communication)
- Database security group (application access only)
- All using predefined LabRole permissions

**Encryption:**
- S3 server-side encryption (AES256)
- EBS volume encryption for EKS nodes
- In-transit encryption for all communications
- MongoDB Atlas encryption at rest

**Production Security Recommendations (Not Implemented in Lab):**
- **GuardDuty:** Threat detection and monitoring
- **CloudTrail:** API call logging and auditing
- **Custom IAM Roles:** Principle of least privilege
- **AWS Config:** Configuration compliance monitoring
- **VPC Flow Logs:** Network traffic analysis

**Security Trade-offs in Lab Environment:**
- Single LabRole reduces security isolation
- No advanced threat detection (GuardDuty)
- Limited audit capabilities (no CloudTrail)
- Simplified security model for educational purposes

**Consequences:**
- ✅ Basic security best practices implemented
- ✅ Network and data encryption
- ✅ Authentication and authorization
- ❌ Limited IAM role granularity
- ❌ No advanced threat detection
- ❌ Reduced audit capabilities
- ❌ Security compromises due to lab constraints

---

## ADR-012: Cost Optimization Strategy
**Decision:** Resource lifecycle management and right-sizing  
**Status:** Accepted  
**Context:** Need for cost-effective infrastructure in educational environment  

**Cost Optimization Measures:**
1. **S3 Lifecycle Policies:** Automatic cleanup of old files
2. **EKS Node Sizing:** Right-sized instances (t3.large)
3. **Auto-scaling:** Scale down during low usage
4. **Resource Tagging:** Cost allocation and tracking
5. **Retention Policies:** Limited data retention periods

**Resource Constraints:**
- Video retention: Configurable (default: 30 days)
- Image clips retention: Configurable (default: 7 days)
- Temporary files: 1-day retention
- Log retention: 30 days

**Consequences:**
- ✅ Predictable and controlled costs
- ✅ Automatic resource cleanup
- ✅ Right-sized infrastructure
- ❌ Data loss risk from aggressive cleanup
- ❌ Performance trade-offs for cost savings

---

## ADR-013: Deployment Strategy
**Decision:** Helm-based container deployment on EKS  
**Status:** Accepted  
**Context:** Need for standardized, repeatable application deployments across microservices  

**Helm Chart Architecture:**
```
helm-chart/
├── Chart.yaml                 # Chart metadata
├── values.yaml               # Default configuration values
├── templates/
│   ├── deployment.yaml       # Kubernetes deployments
│   ├── service.yaml          # Service definitions
│   ├── ingress.yaml          # Ingress configuration
│   └── configmap.yaml        # Configuration management
└── charts/                   # Sub-chart dependencies
```

**Application Deployments:**
1. **vclipper_processing:** Backend processing orchestration service
2. **vclipping:** Video processing service (memory-intensive)
3. **vclipper_fe:** Frontend React application

**Resource Allocation Strategy:**
- **Fixed Resource Allocation:** Predefined container and EC2 instance counts
- **Memory Requirements:** VClipping service requires minimum 1.5GB RAM
- **Instance Sizing:** t3.large instances to meet video processing demands
- **Container Limits:** Resource limits defined in Helm values

**Helm Configuration Benefits:**
- **Templating:** Environment-specific configurations
- **Version Management:** Chart versioning for rollbacks
- **Dependency Management:** Service dependencies handled
- **Configuration Management:** Centralized configuration via values.yaml

**Deployment Process:**
1. Docker images built and pushed to container registry
2. Helm charts updated with new image versions
3. Helm deployment to EKS cluster
4. Kubernetes handles container lifecycle and health checks

**Resource Constraints:**
- **Memory:** VClipping service drives minimum memory requirements
- **CPU:** Auto-scaling based on CPU utilization (80% threshold)
- **Storage:** Persistent volumes for temporary processing files
- **Network:** Service mesh communication within cluster

**Consequences:**
- ✅ Standardized deployment process across services
- ✅ Environment-specific configuration management
- ✅ Version control and rollback capabilities
- ✅ Kubernetes-native deployment patterns
- ❌ Helm learning curve for team
- ❌ Fixed resource allocation reduces flexibility
- ❌ Higher infrastructure costs due to memory requirements

## Summary of Key Infrastructure Decisions

1. **Terraform IaC** for reproducible infrastructure management
2. **AWS Cognito** for managed user authentication with JWT tokens
3. **API Gateway bypassed** due to 10MB payload limit for video files
4. **Amazon EKS with Helm** for container orchestration and standardized deployments
5. **S3 with lifecycle policies** for cost-effective video storage
6. **SQS message queuing** for reliable asynchronous processing
7. **SNS notifications** for multi-channel user communications
8. **Dual monitoring stack** - CloudWatch + Prometheus/Grafana for comprehensive observability
9. **S3-only static hosting** due to CloudFront/Amplify unavailability in lab environment
10. **MongoDB Atlas** for flexible document-based data storage
11. **Constrained security model** using single LabRole due to AWS Academy limitations
12. **Cost optimization** through lifecycle management and right-sizing
13. **Helm-based deployments** for standardized container management

## Critical Architectural Trade-offs

1. **API Gateway Limitation:** Chose direct backend access over API Gateway benefits due to payload constraints
2. **Authentication Simplification:** X-User-Id headers instead of full JWT validation for rapid development
3. **AWS Academy Constraints:** Single LabRole and service limitations impacted security and hosting decisions
4. **Instance Sizing:** t3.large required for video processing (1.5GB+ RAM) vs. cost optimization goals
5. **CDN Absence:** S3-only hosting without CloudFront due to IAM privilege limitations
6. **Security Simplification:** Basic security model due to lab environment constraints
7. **Monitoring Complexity:** Dual monitoring stack adds overhead but provides comprehensive coverage
8. **Fixed Resource Allocation:** Helm deployments with predefined resources vs. dynamic scaling flexibility

## AWS Academy Lab Environment Impact

**Service Limitations:**
- CloudFront and Amplify unavailable
- GuardDuty and CloudTrail not accessible
- Single LabRole for all services
- Limited IAM policy creation

**Architectural Adaptations:**
- Direct S3 hosting instead of CDN
- Simplified security model
- Single IAM role across all services
- Basic monitoring without advanced threat detection

**Production Recommendations:**
- Implement CloudFront for global CDN
- Enable GuardDuty and CloudTrail for security
- Create granular IAM roles following least privilege
- Add advanced monitoring and alerting capabilities
