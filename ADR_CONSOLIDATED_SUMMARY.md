# VClipper System - Consolidated Architecture Decision Records Summary

## System Overview
**VClipper** is a cloud-native video processing platform built as a microservices architecture on AWS, designed to handle video upload, frame extraction, and user management with scalable, resilient components.

---

## Cross-Cutting Architectural Decisions

### Technology Stack & Language Decisions
1. **Java 21 + Spring Boot 3.4.1** chosen for backend services (vclipper_processing, vclipping) for enterprise-grade performance, AWS SDK integration, and team expertise
2. **React 18.3.1 + TypeScript** selected for frontend (vclipper_fe) providing modern UI capabilities, type safety, and excellent developer experience
3. **Clean Architecture with Hexagonal Pattern** implemented across all services for maintainability, testability, and technology independence
4. **Domain-Driven Design (DDD)** principles applied consistently across frontend and backend for clear business logic separation

### AWS Cloud Architecture
5. **AWS-native services strategy** with S3, SQS, SNS, Cognito, and EKS for scalable, managed cloud infrastructure
6. **Terraform Infrastructure as Code** for reproducible, version-controlled infrastructure management across all environments
7. **Amazon EKS with Helm deployments** for container orchestration, providing scalability and standardized deployment processes
8. **AWS Academy lab constraints** requiring single LabRole usage across all services due to IAM privilege limitations

### Authentication & Security
9. **AWS Cognito User Pool** for managed authentication with JWT tokens, advanced security features, and MFA support
10. **X-User-Id header authentication** pattern between frontend and backend services, simplifying integration while maintaining user context
11. **API Gateway bypassed** due to 10MB payload limit conflicting with 500MB video upload requirements, choosing direct backend access
12. **Multi-layered security model** with VPC isolation, security groups, S3 encryption, and container-level security (constrained by lab environment)

### Data & Storage Architecture
13. **MongoDB Atlas** for flexible document storage of video metadata and processing status with schema evolution capabilities
14. **Amazon S3 with lifecycle policies** for scalable video storage with automatic cleanup (videos: 30 days, clips: 7 days, temp: 1 day)
15. **Hybrid storage strategy** combining S3 for persistence and local processing for FFmpeg performance optimization

### Messaging & Communication
16. **Amazon SQS message queuing** for reliable asynchronous communication between services with dead letter queues and retry mechanisms
17. **Amazon SNS notifications** for multi-channel user notifications (email, future SMS/push) and system alerts
18. **Event-driven architecture** with domain events for status changes and processing workflows across services

### Monitoring & Observability
19. **Dual monitoring stack** - AWS CloudWatch for infrastructure metrics + Prometheus/Grafana containers for application-level monitoring
20. **Spring Boot Actuator** endpoints for health checks, metrics, and operational visibility across all Java services
21. **Comprehensive logging strategy** with structured logging, configurable levels, and centralized log aggregation

---

## Service-Specific Architectural Decisions

### VClipper Infrastructure (vclipper_infra)
22. **Modular Terraform structure** with service-specific modules (cognito, eks, s3, sqs, monitoring) for maintainable infrastructure code
23. **S3-only frontend hosting** due to CloudFront/Amplify unavailability in AWS Academy environment (production would use CDN)
24. **Fixed resource allocation** with t3.large instances driven by vclipping service memory requirements (1.5GB minimum)
25. **Cost optimization through lifecycle management** with aggressive cleanup policies suitable for educational environment

### VClipping Service (vclipping)
26. **FFmpeg as video processing engine** for industry-standard video manipulation with system-level integration in containers
27. **500MB maximum file size limit** with 1-hour duration limit, supporting MP4, AVI, MOV, WMV formats for practical video processing
28. **Local processing + S3 storage hybrid** downloading videos locally for FFmpeg processing, then uploading results to S3
29. **Multi-stage Docker build** with FFmpeg system dependencies, optimizing container size while maintaining processing capabilities
30. **Memory-efficient processing** with 1.5GB minimum RAM requirement driving entire infrastructure sizing decisions

### VClipper Processing Service (vclipper_processing)
31. **Processing orchestration role** managing video upload workflows, status tracking, and inter-service communication
32. **Result pattern for error handling** providing comprehensive error management with structured success/failure responses
33. **Mock service adapters** for development and testing (S3, SQS, User services) with production integration interfaces
34. **Configurable retry mechanisms** with exponential backoff for resilient processing in distributed environment

### VClipper Frontend (vclipper_fe)
35. **React Context + Custom Hooks** state management avoiding external libraries while maintaining type safety and reusability
36. **Comprehensive testing strategy** achieving 25.48% coverage with 255 tests across domain, application, and presentation layers
37. **Feature-based component organization** with reusable common components and clear separation between authentication and video features
38. **WCAG 2.1 AA accessibility compliance** with comprehensive inclusive design, keyboard navigation, and screen reader support
39. **Direct backend API communication** with chunked file uploads supporting 500MB videos and real-time progress tracking
40. **Create React App with TypeScript** for rapid development setup while maintaining production-ready build optimization

---

## Critical System Trade-offs

### Performance vs. Cost
- **Instance Sizing:** t3.large instances required for video processing vs. cost optimization goals
- **Storage Lifecycle:** Aggressive cleanup policies (30/7/1 day retention) for cost control vs. data persistence

### Security vs. Simplicity
- **Authentication Model:** X-User-Id headers vs. full JWT validation for rapid development
- **AWS Academy Constraints:** Single LabRole vs. granular IAM permissions for security isolation

### Scalability vs. Complexity
- **Microservices Architecture:** EKS + multiple services vs. monolithic simplicity
- **Clean Architecture:** Higher initial complexity vs. long-term maintainability benefits

### Functionality vs. Constraints
- **API Gateway Bypass:** Large file upload capability vs. API Gateway benefits (throttling, monitoring)
- **Video Processing Limits:** 500MB/1-hour limits vs. unlimited processing capability

---

## System Constraints & Assumptions

### Technical Constraints
- **AWS Academy Environment:** Limited IAM privileges, no CloudFront/Amplify, single LabRole usage
- **Memory Requirements:** 1.5GB minimum for video processing driving infrastructure costs
- **File Size Limits:** 500MB maximum video files, 1-hour maximum duration
- **Processing Timeouts:** 5-minute FFmpeg timeout, 2-minute file transfers

### Business Assumptions
- **Educational Use Case:** Aggressive cost optimization suitable for learning environment
- **Video Formats:** Support for common formats (MP4, AVI, MOV, WMV) covering majority use cases
- **User Scale:** Designed for moderate concurrent users in educational setting
- **Processing Workflow:** Frame extraction at 1 FPS with JPEG quality balance (85%)

### Deployment Assumptions
- **Container Environment:** Docker + Kubernetes deployment with Helm standardization
- **AWS Region:** us-east-1 for all services and resources
- **Network Connectivity:** Reliable internet for AWS service communication
- **Development Team:** Java/React expertise with AWS cloud knowledge

This consolidated summary represents the complete architectural decision landscape for the VClipper video processing platform, balancing educational constraints with production-ready architectural patterns.
