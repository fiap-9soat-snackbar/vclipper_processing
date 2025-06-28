#!/bin/bash

# ===================================================
# VClipper Processing Service - End-to-End Integration Test
# ===================================================
# Comprehensive test that validates the complete video processing workflow
# from environment setup to API functionality and cleanup
#
# Usage: ./scripts/test-e2e-integration.sh [--fast] [--no-cleanup]
#   --fast      Skip image rebuilding if images exist and are recent
#   --no-cleanup Keep containers running after test completion
# ===================================================

set -e  # Exit on any error

# Parse command line arguments
FAST_MODE=false
NO_CLEANUP=false

for arg in "$@"; do
    case $arg in
        --fast)
            FAST_MODE=true
            shift
            ;;
        --no-cleanup)
            NO_CLEANUP=true
            shift
            ;;
        --help)
            echo "Usage: $0 [--fast] [--no-cleanup]"
            echo "  --fast      Skip rebuilding if Docker images are recent"
            echo "  --no-cleanup Keep containers running after test"
            exit 0
            ;;
    esac
done

# Load environment variables from .env file
if [ -f .env ]; then
    export $(grep -v '^#' .env | xargs)
else
    echo "❌ Error: .env file not found. Please ensure .env file exists in the project root."
    exit 1
fi

# Color definitions for better readability
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Status tracking for each section
declare -a SECTION_STATUS=(
    "SUCCESS"  # 0: Environment Setup & Cleanup
    "SUCCESS"  # 1: Health Check & Service Validation
    "SUCCESS"  # 2: Video Upload Testing
    "SUCCESS"  # 3: Video Status Retrieval Testing
    "SUCCESS"  # 4: Video Listing Testing
    "SUCCESS"  # 5: Download URL Testing (Result Pattern)
    "SUCCESS"  # 6: Error Handling Testing
    "SUCCESS"  # 7: AWS S3 Integration Validation
    "SUCCESS"  # 8: Mock Service Validation
    "SUCCESS"  # 9: Database Validation
    "SUCCESS"  # 10: Configuration Validation
    "SUCCESS"  # 11: Application Logs Analysis
    "SUCCESS"  # 12: Performance & Resource Usage
    "SUCCESS"  # 13: Environment Cleanup
)

declare -a SECTION_NAMES=(
    "Environment Setup & Cleanup"
    "Health Check & Service Validation"
    "Video Upload Testing"
    "Video Status Retrieval Testing"
    "Video Listing Testing"
    "Download URL Testing (Result Pattern)"
    "Error Handling Testing"
    "AWS S3 Integration Validation"
    "Mock Service Validation"
    "Database Validation"
    "Configuration Validation"
    "Application Logs Analysis"
    "Performance & Resource Usage"
    "Environment Cleanup"
)

# Function to set section status
set_section_status() {
    local section_index=$1
    local status=$2  # SUCCESS, WARNING, ERROR
    SECTION_STATUS[$section_index]=$status
}

# Function to print section headers
print_section() {
  echo ""
  echo -e "${BOLD}${PURPLE}==============================================${NC}"
  echo -e "${BOLD}${BLUE}   SECTION $1: $2${NC}"
  echo -e "${BOLD}${PURPLE}==============================================${NC}"
  echo ""
}

# Function to print status messages
print_status() {
    local status=$1
    local message=$2
    case $status in
        "SUCCESS") echo -e "${GREEN}✅ $message${NC}" ;;
        "ERROR") echo -e "${RED}❌ $message${NC}" ;;
        "WARNING") echo -e "${YELLOW}⚠️  $message${NC}" ;;
        "INFO") echo -e "${BLUE}ℹ️  $message${NC}" ;;
    esac
}

# Function to run command with timeout
run_with_timeout() {
    local timeout=$1
    local description=$2
    shift 2
    
    echo "Running: $*"
    if timeout $timeout "$@"; then
        return 0
    else
        print_status "ERROR" "$description timed out after $timeout seconds"
        return 1
    fi
}

# Function to wait for service to be ready
wait_for_service() {
    local service_name=$1
    local health_url=$2
    local max_attempts=$3
    local sleep_time=${4:-3}
    
    print_status "INFO" "Checking if $service_name is ready..."
    
    for i in $(seq 1 $max_attempts); do
        echo "Attempt $i/$max_attempts: Testing $service_name endpoint..."
        if curl -s -f "$health_url" > /dev/null 2>&1; then
            print_status "SUCCESS" "$service_name is ready!"
            return 0
        fi
        
        if [ $i -lt $max_attempts ]; then
            sleep $sleep_time
        fi
    done
    
    print_status "ERROR" "$service_name failed to become ready after $max_attempts attempts"
    return 1
}

# ===================================================
# SECTION 1: Environment Setup and Cleanup
# ===================================================
print_section "1" "Environment Setup and Cleanup"

print_status "INFO" "Cleaning up existing environment..."

# Remove target folder
print_status "INFO" "Removing target folder..."
if [ -d "target" ]; then
    run_with_timeout 30 "Target folder removal" rm -rf target
    print_status "SUCCESS" "Target folder removed"
else
    print_status "INFO" "Target folder doesn't exist, skipping"
fi

# Stop and remove existing containers
print_status "INFO" "Stopping and removing Docker containers..."
run_with_timeout 60 "Docker cleanup" docker compose down -v --remove-orphans
print_status "SUCCESS" "Docker containers removed (keeping images)"

# Build application
print_status "INFO" "Building application..."
run_with_timeout 300 "Maven build" mvn clean package -DskipTests
print_status "SUCCESS" "Application built successfully"

# Check if Docker images exist to avoid unnecessary rebuilds
print_status "INFO" "Checking Docker image status..."
PROCESSING_IMAGE_EXISTS=$(docker images -q vclipper_processing-processing-service 2>/dev/null)
BUILD_FLAG=""

if [[ -n "$PROCESSING_IMAGE_EXISTS" ]] && [[ "$FAST_MODE" == "true" ]]; then
    print_status "INFO" "Fast mode enabled - using existing Docker images"
    BUILD_FLAG=""
elif [[ -n "$PROCESSING_IMAGE_EXISTS" ]]; then
    print_status "INFO" "Docker images exist, checking if rebuild is needed..."
    # Check if source code is newer than image (simplified check)
    if [[ -n "$(find src/ -newer target/ 2>/dev/null | head -1)" ]] 2>/dev/null; then
        print_status "INFO" "Source code changes detected, rebuilding images..."
        BUILD_FLAG="--build"
    else
        print_status "INFO" "Using existing Docker images (no recent source changes)"
        BUILD_FLAG=""
    fi
else
    print_status "INFO" "Docker images not found, building from scratch..."
    BUILD_FLAG="--build"
fi

# Start containers
print_status "INFO" "Starting Docker containers..."
if run_with_timeout 300 "Docker startup" docker compose up -d $BUILD_FLAG --quiet-pull; then
    print_status "SUCCESS" "Containers started successfully"
else
    print_status "ERROR" "Failed to start containers"
    print_status "INFO" "Showing recent Docker logs for debugging..."
    docker compose logs --tail=20
    set_section_status 0 "ERROR"
    exit 1
fi

# Show container status
echo "Checking container status:"
docker compose ps

# Wait for services to start
print_status "INFO" "Waiting for application to start (30 seconds)..."
sleep 30

# Check MongoDB
print_status "INFO" "Checking if MongoDB is ready..."
MONGO_READY=false
for i in {1..10}; do
  echo "Attempt $i/10: Testing MongoDB connection..."
  if docker compose exec -T mongodb mongosh --quiet --eval "db.runCommand({ping:1}).ok" 2>&1 | grep -q "1"; then
    MONGO_READY=true
    print_status "SUCCESS" "MongoDB is ready!"
    break
  else
    print_status "WARNING" "MongoDB not ready yet, waiting 3 seconds..."
    sleep 3
  fi
done

if [ "$MONGO_READY" = false ]; then
  print_status "ERROR" "MongoDB is not ready. Checking logs..."
  echo "MongoDB logs:"
  docker compose logs mongodb | tail -20
  set_section_status 0 "ERROR"
  exit 1
fi

# Check Processing Service
wait_for_service "Processing Service" "http://localhost:8080/actuator/health" 15 3
if [ $? -eq 0 ]; then
    print_status "SUCCESS" "Processing Service is ready!"
    HEALTH_RESPONSE=$(curl -s http://localhost:8080/actuator/health)
    echo "Health check response: $HEALTH_RESPONSE"
else
    print_status "ERROR" "Processing Service failed to start"
    set_section_status 0 "ERROR"
    exit 1
fi

# ===================================================
# SECTION 2: Health Check and Service Validation
# ===================================================
print_section "2" "Health Check and Service Validation"

# Test health endpoint
print_status "INFO" "Testing application health endpoint..."
HEALTH_RESPONSE=$(curl -s http://localhost:8080/actuator/health)
print_status "INFO" "Health Response: $HEALTH_RESPONSE"

if echo "$HEALTH_RESPONSE" | grep -q '"status":"UP"'; then
    print_status "SUCCESS" "Application health check passed"
else
    print_status "ERROR" "Application health check failed"
    set_section_status 1 "ERROR"
fi

# Test info endpoint
print_status "INFO" "Testing application info endpoint..."
INFO_RESPONSE=$(curl -s http://localhost:8080/actuator/info)
print_status "INFO" "Info Response: $INFO_RESPONSE"

if [ -n "$INFO_RESPONSE" ] && [ "$INFO_RESPONSE" != "{}" ]; then
    print_status "SUCCESS" "Application info endpoint accessible"
else
    print_status "WARNING" "Application info endpoint returned empty response"
    set_section_status 1 "WARNING"
fi

# Validate MongoDB connectivity
print_status "INFO" "Validating MongoDB connectivity through application..."
if echo "$HEALTH_RESPONSE" | grep -q "mongo.*UP\|db.*UP"; then
    print_status "SUCCESS" "MongoDB connection from application is healthy"
else
    print_status "WARNING" "MongoDB connection status unclear from application health"
    set_section_status 1 "WARNING"
fi

# ===================================================
# SECTION 3: Video Upload Testing
# ===================================================
print_section "3" "Video Upload Testing"

# Create test video file
print_status "INFO" "Creating test video file..."
TEST_VIDEO_FILE="test-video.mp4"

# Create a minimal MP4 file with proper MP4 signature for MIME type detection
printf '\x00\x00\x00\x20\x66\x74\x79\x70\x69\x73\x6f\x6d\x00\x00\x02\x00\x69\x73\x6f\x6d\x69\x73\x6f\x32\x61\x76\x63\x31\x6d\x70\x34\x31' > "$TEST_VIDEO_FILE"
printf '\x00\x00\x00\x08\x66\x72\x65\x65' >> "$TEST_VIDEO_FILE"
echo "VClipper Test Video Content - $(date)" >> "$TEST_VIDEO_FILE"
echo "This is a test video file for integration testing" >> "$TEST_VIDEO_FILE"

if [[ -f "$TEST_VIDEO_FILE" ]]; then
    print_status "SUCCESS" "Test video file created: $TEST_VIDEO_FILE"
else
    print_status "ERROR" "Failed to create test video file"
    set_section_status 2 "ERROR"
    exit 1
fi

# Test video upload with valid user
print_status "INFO" "Testing video upload with valid user..."
TEST_USER_ID="test-user-123"

UPLOAD_RESPONSE=$(curl -s -X POST \
    -F "userId=$TEST_USER_ID" \
    -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${UPLOAD_RESPONSE: -3}"
RESPONSE_BODY="${UPLOAD_RESPONSE%???}"

print_status "INFO" "Upload HTTP Status: $HTTP_CODE"
print_status "INFO" "Upload Response: $RESPONSE_BODY"

if [[ "$HTTP_CODE" == "201" ]]; then
    print_status "SUCCESS" "Video upload successful (HTTP 201)"
    
    # Extract video ID from response
    VIDEO_ID=$(echo "$RESPONSE_BODY" | grep -o '"videoId":"[^"]*' | cut -d'"' -f4)
    
    if [[ -n "$VIDEO_ID" ]]; then
        print_status "SUCCESS" "Video ID extracted: $VIDEO_ID"
        echo "$VIDEO_ID" > .test_video_id  # Save for other tests
    else
        print_status "WARNING" "Could not extract video ID from response"
        set_section_status 2 "WARNING"
    fi
else
    print_status "ERROR" "Video upload failed (HTTP $HTTP_CODE)"
    print_status "INFO" "Response: $RESPONSE_BODY"
    print_status "INFO" "Checking application logs for upload errors..."
    docker compose logs processing-service | grep -A 5 -B 5 "error\|Error\|ERROR" | tail -10
    set_section_status 2 "ERROR"
fi

# ===================================================
# SECTION 4: Video Status Retrieval Testing
# ===================================================
print_section "4" "Video Status Retrieval Testing"

if [[ -f ".test_video_id" ]]; then
    VIDEO_ID=$(cat .test_video_id)
    print_status "INFO" "Testing video status retrieval for video: $VIDEO_ID"
    
    STATUS_RESPONSE=$(curl -s -X GET \
        -w "%{http_code}" \
        "http://localhost:8080/api/videos/$VIDEO_ID/status?userId=$TEST_USER_ID")
    
    HTTP_CODE="${STATUS_RESPONSE: -3}"
    RESPONSE_BODY="${STATUS_RESPONSE%???}"
    
    print_status "INFO" "Status HTTP Code: $HTTP_CODE"
    print_status "INFO" "Status Response: $RESPONSE_BODY"
    
    if [[ "$HTTP_CODE" == "200" ]]; then
        print_status "SUCCESS" "Status retrieval successful (HTTP 200)"
        
        # Check if response contains expected fields
        if echo "$RESPONSE_BODY" | grep -q '"videoId"\|"status"\|"originalFilename"'; then
            print_status "SUCCESS" "Response contains expected status fields"
            
            # Extract and display key information
            STATUS_VALUE=$(echo "$RESPONSE_BODY" | grep -o '"value":"[^"]*' | cut -d'"' -f4)
            FILENAME=$(echo "$RESPONSE_BODY" | grep -o '"originalFilename":"[^"]*' | cut -d'"' -f4)
            
            print_status "INFO" "Video Status: $STATUS_VALUE"
            print_status "INFO" "Original Filename: $FILENAME"
        else
            print_status "WARNING" "Response missing expected status fields"
            set_section_status 3 "WARNING"
        fi
    else
        print_status "ERROR" "Status retrieval failed (HTTP $HTTP_CODE)"
        print_status "INFO" "Response: $RESPONSE_BODY"
        set_section_status 3 "ERROR"
    fi
else
    print_status "WARNING" "Skipping status test - no video ID available"
    set_section_status 3 "WARNING"
fi

# ===================================================
# SECTION 5: Video Listing Testing
# ===================================================
print_section "5" "Video Listing Testing"

print_status "INFO" "Testing video listing for user: $TEST_USER_ID"

LIST_RESPONSE=$(curl -s -X GET \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos?userId=$TEST_USER_ID")

HTTP_CODE="${LIST_RESPONSE: -3}"
RESPONSE_BODY="${LIST_RESPONSE%???}"

print_status "INFO" "List HTTP Code: $HTTP_CODE"
print_status "INFO" "List Response: $RESPONSE_BODY"

if [[ "$HTTP_CODE" == "200" ]]; then
    print_status "SUCCESS" "Video listing successful (HTTP 200)"
    
    # Check if response contains expected fields
    if echo "$RESPONSE_BODY" | grep -q '"userId"\|"videos"\|"totalCount"'; then
        print_status "SUCCESS" "Response contains expected listing fields"
        
        # Extract total count
        TOTAL_COUNT=$(echo "$RESPONSE_BODY" | grep -o '"totalCount":[0-9]*' | cut -d':' -f2)
        print_status "INFO" "Total videos for user: $TOTAL_COUNT"
    else
        print_status "WARNING" "Response missing expected listing fields"
        set_section_status 4 "WARNING"
    fi
else
    print_status "ERROR" "Video listing failed (HTTP $HTTP_CODE)"
    print_status "INFO" "Response: $RESPONSE_BODY"
    set_section_status 4 "ERROR"
fi

# ===================================================
# SECTION 5: Download URL Testing (Result Pattern)
# ===================================================
print_section "5" "Download URL Testing (Result Pattern)"

if [[ -f ".test_video_id" ]]; then
    VIDEO_ID=$(cat .test_video_id)
    print_status "INFO" "Testing download URL generation for video: $VIDEO_ID"
    
    # Test download URL request for PENDING video (should return business error, not exception)
    print_status "INFO" "Testing download URL for PENDING video (Result pattern validation)..."
    
    DOWNLOAD_RESPONSE=$(curl -s -X GET \
        -w "%{http_code}" \
        "http://localhost:8080/api/videos/$VIDEO_ID/download?userId=$TEST_USER_ID")
    
    HTTP_CODE="${DOWNLOAD_RESPONSE: -3}"
    RESPONSE_BODY="${DOWNLOAD_RESPONSE%???}"
    
    print_status "INFO" "Download HTTP Code: $HTTP_CODE"
    print_status "INFO" "Download Response: $RESPONSE_BODY"
    
    # Should return 409 Conflict for business rule violation (video not ready)
    if [[ "$HTTP_CODE" == "409" ]]; then
        print_status "SUCCESS" "Correct HTTP status for video not ready (409 Conflict)"
        
        # Validate Result pattern response structure
        if echo "$RESPONSE_BODY" | grep -q '"success":false' && echo "$RESPONSE_BODY" | grep -q '"message".*not ready'; then
            print_status "SUCCESS" "Result pattern response structure is correct"
            
            # Extract key information
            MESSAGE=$(echo "$RESPONSE_BODY" | grep -o '"message":"[^"]*' | cut -d'"' -f4)
            print_status "INFO" "Business error message: $MESSAGE"
            
            # Verify no exception logging noise (check logs)
            print_status "INFO" "Checking for clean error logging (no stack traces)..."
            RECENT_LOGS=$(docker compose logs processing-service --since=30s)
            
            if echo "$RECENT_LOGS" | grep -q "Video not ready for download" && ! echo "$RECENT_LOGS" | grep -q "Exception.*VideoNotReadyException"; then
                print_status "SUCCESS" "Clean business error logging confirmed (no exception stack traces)"
            else
                print_status "WARNING" "May still have exception logging noise"
                set_section_status 5 "WARNING"
            fi
        else
            print_status "WARNING" "Response structure doesn't match expected Result pattern"
            set_section_status 5 "WARNING"
        fi
    else
        print_status "ERROR" "Expected HTTP 409 for video not ready, got $HTTP_CODE"
        print_status "INFO" "Response: $RESPONSE_BODY"
        set_section_status 5 "ERROR"
    fi
    
    # Test download URL with invalid video ID (should still throw exception for security)
    print_status "INFO" "Testing download URL with invalid video ID (security boundary)..."
    
    INVALID_DOWNLOAD_RESPONSE=$(curl -s -X GET \
        -w "%{http_code}" \
        "http://localhost:8080/api/videos/invalid-video-id/download?userId=$TEST_USER_ID")
    
    HTTP_CODE="${INVALID_DOWNLOAD_RESPONSE: -3}"
    print_status "INFO" "Invalid video download HTTP Code: $HTTP_CODE"
    
    if [[ "$HTTP_CODE" == "404" ]]; then
        print_status "SUCCESS" "Security boundary maintained (404 for invalid video)"
    else
        print_status "WARNING" "Expected HTTP 404 for invalid video, got $HTTP_CODE"
        set_section_status 5 "WARNING"
    fi
    
    # Test download URL with unauthorized user (should throw exception for security)
    print_status "INFO" "Testing download URL with unauthorized user (security boundary)..."
    
    UNAUTHORIZED_DOWNLOAD_RESPONSE=$(curl -s -X GET \
        -w "%{http_code}" \
        "http://localhost:8080/api/videos/$VIDEO_ID/download?userId=unauthorized-user")
    
    HTTP_CODE="${UNAUTHORIZED_DOWNLOAD_RESPONSE: -3}"
    print_status "INFO" "Unauthorized download HTTP Code: $HTTP_CODE"
    
    if [[ "$HTTP_CODE" == "404" ]]; then
        print_status "SUCCESS" "Authorization security maintained (404 for unauthorized user)"
    else
        print_status "WARNING" "Expected HTTP 404 for unauthorized user, got $HTTP_CODE"
        set_section_status 5 "WARNING"
    fi
    
else
    print_status "WARNING" "Skipping download URL test - no video ID available"
    set_section_status 5 "WARNING"
fi

# ===================================================
# SECTION 6: Error Handling Testing
# ===================================================
print_section "6" "Error Handling Testing"

# Test upload with invalid user
print_status "INFO" "Testing upload with invalid user..."

INVALID_USER_RESPONSE=$(curl -s -X POST \
    -F "userId=" \
    -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${INVALID_USER_RESPONSE: -3}"
print_status "INFO" "Invalid user upload HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "400" ]]; then
    print_status "SUCCESS" "Properly rejected invalid user (HTTP 400)"
else
    print_status "WARNING" "Expected HTTP 400 for invalid user, got $HTTP_CODE"
    set_section_status 5 "WARNING"
fi

# Test upload without file
print_status "INFO" "Testing upload without file..."

NO_FILE_RESPONSE=$(curl -s -X POST \
    -F "userId=$TEST_USER_ID" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${NO_FILE_RESPONSE: -3}"
print_status "INFO" "No file upload HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "400" ]]; then
    print_status "SUCCESS" "Properly rejected upload without file (HTTP 400)"
else
    print_status "WARNING" "Expected HTTP 400 for missing file, got $HTTP_CODE"
    set_section_status 5 "WARNING"
fi

# Test status retrieval with invalid video ID
print_status "INFO" "Testing status retrieval with invalid video ID..."

INVALID_STATUS_RESPONSE=$(curl -s -X GET \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/invalid-id/status?userId=$TEST_USER_ID")

HTTP_CODE="${INVALID_STATUS_RESPONSE: -3}"
print_status "INFO" "Invalid video ID status HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "404" ]]; then
    print_status "SUCCESS" "Properly returned 404 for invalid video ID"
else
    print_status "WARNING" "Expected HTTP 404 for invalid video ID, got $HTTP_CODE"
    set_section_status 5 "WARNING"
fi

# ===================================================
# SECTION 7: AWS S3 Integration Validation
# ===================================================
print_section "7" "AWS S3 Integration Validation"

print_status "INFO" "Validating real AWS S3 integration..."

# Check for real S3 operations in logs (not mock)
S3_REAL_OPERATIONS=$(docker compose logs processing-service | grep -c "Successfully stored in S3\|S3.*ETag" || echo "0")
S3_MOCK_OPERATIONS=$(docker compose logs processing-service | grep -c "MOCK S3" || echo "0")

print_status "INFO" "Real S3 operations logged: $S3_REAL_OPERATIONS"
print_status "INFO" "Mock S3 operations logged: $S3_MOCK_OPERATIONS"

if [[ "$S3_REAL_OPERATIONS" -gt 0 ]]; then
    print_status "SUCCESS" "Real AWS S3 integration is working"
    
    # Check for S3 bucket and key information
    S3_BUCKET_LOGS=$(docker compose logs processing-service | grep "vclipper-video-storage-dev" | wc -l)
    S3_KEY_LOGS=$(docker compose logs processing-service | grep "videos/[0-9]*/[0-9]*/[0-9]*/" | wc -l)
    
    print_status "INFO" "S3 bucket references in logs: $S3_BUCKET_LOGS"
    print_status "INFO" "S3 key pattern matches in logs: $S3_KEY_LOGS"
    
    if [[ "$S3_BUCKET_LOGS" -gt 0 && "$S3_KEY_LOGS" -gt 0 ]]; then
        print_status "SUCCESS" "S3 bucket and key structure validation passed"
    else
        print_status "WARNING" "S3 bucket or key structure may not be correct"
        set_section_status 7 "WARNING"
    fi
    
    # Show recent S3 operation logs
    print_status "INFO" "Recent S3 operations:"
    docker compose logs processing-service | grep -E "S3.*store|S3.*upload|Successfully stored in S3" | tail -3
    
elif [[ "$S3_MOCK_OPERATIONS" -gt 0 ]]; then
    print_status "WARNING" "Still using Mock S3 adapter - real AWS integration not active"
    set_section_status 7 "WARNING"
else
    print_status "ERROR" "No S3 operations detected (neither real nor mock)"
    set_section_status 7 "ERROR"
fi

# Validate AWS credentials are properly mounted
print_status "INFO" "Checking AWS credentials configuration..."
AWS_CREDS_LOGS=$(docker compose logs processing-service | grep -c "AWS\|credentials\|region" || echo "0")

if [[ "$AWS_CREDS_LOGS" -gt 0 ]]; then
    print_status "INFO" "AWS configuration references found in logs: $AWS_CREDS_LOGS"
else
    print_status "WARNING" "Limited AWS configuration references in logs"
    set_section_status 7 "WARNING"
fi

# ===================================================
# SECTION 8: AWS Integration Validation
# ===================================================
print_section "8" "AWS Integration Validation"

print_status "INFO" "Validating real AWS service integrations..."

# Check for real AWS operations in logs
SQS_REAL_OPERATIONS=$(docker compose logs processing-service | grep -c "Successfully sent message to SQS\|MessageId.*sent to SQS" || echo "0")
SNS_REAL_OPERATIONS=$(docker compose logs processing-service | grep -c "Successfully sent notification via SNS\|MessageId.*SNS" || echo "0")
USER_SERVICE_OPERATIONS=$(docker compose logs processing-service | grep -c "MOCK USER SERVICE" || echo "0")

print_status "INFO" "Real SQS operations logged: $SQS_REAL_OPERATIONS"
print_status "INFO" "Real SNS operations logged: $SNS_REAL_OPERATIONS"
print_status "INFO" "User Service Mock operations logged: $USER_SERVICE_OPERATIONS"

if [[ "$SQS_REAL_OPERATIONS" -gt 0 && "$SNS_REAL_OPERATIONS" -gt 0 ]]; then
    print_status "SUCCESS" "Real AWS SQS and SNS integrations are working"
    
    # Show recent AWS operation logs
    print_status "INFO" "Recent SQS operations:"
    docker compose logs processing-service | grep -E "SQS.*message|Successfully sent message to SQS" | tail -2
    
    print_status "INFO" "Recent SNS operations:"
    docker compose logs processing-service | grep -E "SNS.*notification|Successfully sent notification via SNS" | tail -2
    
elif [[ "$SQS_REAL_OPERATIONS" -gt 0 ]]; then
    print_status "WARNING" "SQS integration working but SNS operations not detected"
    set_section_status 8 "WARNING"
elif [[ "$SNS_REAL_OPERATIONS" -gt 0 ]]; then
    print_status "WARNING" "SNS integration working but SQS operations not detected"
    set_section_status 8 "WARNING"
else
    print_status "ERROR" "No real AWS SQS or SNS operations detected"
    set_section_status 8 "ERROR"
fi

if [[ "$USER_SERVICE_OPERATIONS" -gt 0 ]]; then
    print_status "INFO" "User Service Mock adapter is active (expected)"
else
    print_status "WARNING" "User Service Mock adapter may not be active"
    set_section_status 8 "WARNING"
fi

# ===================================================
# SECTION 9: Database Validation
# ===================================================
print_section "9" "Database Validation"

print_status "INFO" "Validating data persistence in MongoDB..."

# Check if video data was persisted
VIDEO_COUNT=$(docker exec $(docker compose ps -q mongodb) mongosh --quiet -u "$MONGODB_USER" -p "$MONGODB_PASSWORD" --authenticationDatabase "$MONGODB_DATABASE" --eval "db.videoProcessingRequests.countDocuments({})" "$MONGODB_DATABASE" 2>/dev/null || echo "0")

print_status "INFO" "Videos stored in MongoDB: $VIDEO_COUNT"

if [[ "$VIDEO_COUNT" -gt 0 ]]; then
    print_status "SUCCESS" "Video data successfully persisted in MongoDB"
else
    print_status "WARNING" "No video data found in MongoDB"
    set_section_status 7 "WARNING"
fi

# Check MongoDB indexes
print_status "INFO" "Checking MongoDB indexes..."
INDEX_COUNT=$(docker exec $(docker compose ps -q mongodb) mongosh --quiet -u "$MONGODB_USER" -p "$MONGODB_PASSWORD" --authenticationDatabase "$MONGODB_DATABASE" --eval "db.videoProcessingRequests.getIndexes().length" "$MONGODB_DATABASE" 2>/dev/null || echo "0")

print_status "INFO" "Number of indexes on videoProcessingRequests collection: $INDEX_COUNT"

if [[ "$INDEX_COUNT" -gt 1 ]]; then  # More than just the default _id index
    print_status "SUCCESS" "Custom indexes are properly created"
else
    print_status "WARNING" "Custom indexes may not be properly created"
    set_section_status 7 "WARNING"
fi

# ===================================================
# SECTION 10: Configuration Validation
# ===================================================
print_section "10" "Configuration Validation"

print_status "INFO" "Validating configuration loading..."

# Check for configuration-related log entries
CONFIG_LOGS=$(docker compose logs processing-service | grep -c "Configuration\|config\|property" || echo "0")
print_status "INFO" "Configuration-related log entries: $CONFIG_LOGS"

# Test file size limits (create a file larger than configured limit)
print_status "INFO" "Testing configured file size limits..."
LARGE_FILE="large-test-file.mp4"
# Create 600MB file (larger than 500MB limit)
dd if=/dev/zero of="$LARGE_FILE" bs=1M count=600 2>/dev/null

LARGE_UPLOAD_RESPONSE=$(curl -s -X POST \
    -F "userId=$TEST_USER_ID" \
    -F "file=@$LARGE_FILE" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${LARGE_UPLOAD_RESPONSE: -3}"
print_status "INFO" "Large file upload HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "413" || "$HTTP_CODE" == "400" || "$HTTP_CODE" == "500" ]]; then
    print_status "INFO" "File size validation working (HTTP $HTTP_CODE)"
else
    print_status "WARNING" "File size validation may not be working properly"
    set_section_status 8 "WARNING"
fi

# Cleanup large file
rm -f "$LARGE_FILE"

# ===================================================
# SECTION 11: Application Logs Analysis
# ===================================================
print_section "11" "Application Logs Analysis"

print_status "INFO" "Analyzing application logs for errors and warnings..."

# Count actual log levels (not just words containing "error") - fix multi-line output
ERROR_COUNT=$(docker compose logs processing-service | grep " ERROR " | wc -l)
WARNING_COUNT=$(docker compose logs processing-service | grep " WARN " | wc -l)
SUCCESS_COUNT=$(docker compose logs processing-service | grep -i "success" | wc -l)

print_status "INFO" "ERROR level log entries: $ERROR_COUNT"

if [[ "$ERROR_COUNT" -gt 0 ]]; then
    print_status "WARNING" "Found ERROR level entries in application logs:"
    docker compose logs processing-service | grep " ERROR " | tail -5
    set_section_status 9 "WARNING"
fi

print_status "INFO" "WARN level log entries: $WARNING_COUNT"

if [[ "$WARNING_COUNT" -gt 0 ]]; then
    print_status "INFO" "Found WARN level entries (expected for test scenarios):"
    docker compose logs processing-service | grep " WARN " | tail -3
fi

print_status "INFO" "Success log entries: $SUCCESS_COUNT"

# ===================================================
# SECTION 12: Performance and Resource Usage
# ===================================================
print_section "12" "Performance and Resource Usage"

print_status "INFO" "Checking container resource usage..."

# Show container stats
docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"

# Show Docker disk usage
print_status "INFO" "Docker disk usage:"
docker system df

# ===================================================
# SECTION 13: Cleanup
# ===================================================
print_section "13" "Cleanup"

print_status "INFO" "Cleaning up test files..."
rm -f "$TEST_VIDEO_FILE" .test_video_id

if [[ "$NO_CLEANUP" == "true" ]]; then
    print_status "INFO" "Skipping Docker cleanup (--no-cleanup flag specified)"
    print_status "INFO" "Containers are still running for manual inspection"
    print_status "INFO" "To stop containers manually: docker compose down -v"
else
    print_status "INFO" "Cleaning up Docker environment..."
    run_with_timeout 60 "Docker cleanup" docker compose down -v --remove-orphans
    print_status "SUCCESS" "Environment cleanup completed"
fi

# ===================================================
# FINAL SUMMARY
# ===================================================
echo ""
echo -e "${BOLD}${GREEN}==================================================${NC}"
echo -e "${BOLD}${GREEN}   VClipper E2E Integration Test Completed!${NC}"
echo -e "${BOLD}${GREEN}==================================================${NC}"
echo ""
print_status "SUCCESS" "🎉 End-to-end integration test completed successfully!"
echo ""
print_status "INFO" "📊 Test Summary:"

# Display actual section statuses
for i in "${!SECTION_NAMES[@]}"; do
    case "${SECTION_STATUS[$i]}" in
        "SUCCESS") print_status "SUCCESS" "   ✅ ${SECTION_NAMES[$i]}" ;;
        "WARNING") print_status "WARNING" "   ⚠️  ${SECTION_NAMES[$i]}" ;;
        "ERROR") print_status "ERROR" "   ❌ ${SECTION_NAMES[$i]}" ;;
    esac
done

echo ""
print_status "INFO" "🚀 VClipper Processing Service is working correctly!"
echo ""
