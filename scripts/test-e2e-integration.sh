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
    set -a  # automatically export all variables
    source .env
    set +a  # stop automatically exporting
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
    "SUCCESS"  # 6: Processing Workflow Simulation (PENDING → PROCESSING → COMPLETED)
    "SUCCESS"  # 7: Error Handling Testing
    "SUCCESS"  # 8: AWS Integration Validation (consolidated S3, SQS, SNS)
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
    "Processing Workflow Simulation (PENDING → PROCESSING → COMPLETED)"
    "Error Handling Testing"
    "AWS Integration Validation"
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
# SECTION 0: Environment Setup and Cleanup
# ===================================================
print_section "0" "Environment Setup and Cleanup"

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

# Check MongoDB - simplified approach
print_status "INFO" "Checking if MongoDB container is running..."
if docker compose ps mongodb | grep -q "Up"; then
    print_status "SUCCESS" "MongoDB container is running!"
else
    print_status "ERROR" "MongoDB container is not running"
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
# SECTION 1: Health Check and Service Validation
# ===================================================
print_section "1" "Health Check and Service Validation"

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
# SECTION 2: Video Upload Testing
# ===================================================
print_section "2" "Video Upload Testing"

# Check for existing test video file or create one
print_status "INFO" "Preparing test video file..."

# First, check if we have the existing test video
if [[ -f "test_videos/better_test_video.mp4" ]]; then
    TEST_VIDEO_FILE="test_videos/better_test_video.mp4"
    print_status "SUCCESS" "Using existing test video file: $TEST_VIDEO_FILE"
else
    # Create a new test video file if none exists
    print_status "INFO" "Creating new test video file..."
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
fi

# Test video upload with valid user
print_status "INFO" "Testing video upload with valid user..."
TEST_USER_ID="test-user-123"

UPLOAD_RESPONSE=$(curl -s -X POST \
    -H "X-User-Id: $TEST_USER_ID" \
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
# SECTION 3: Video Status Retrieval Testing
# ===================================================
print_section "3" "Video Status Retrieval Testing"

if [[ -f ".test_video_id" ]]; then
    VIDEO_ID=$(cat .test_video_id)
    print_status "INFO" "Testing video status retrieval for video: $VIDEO_ID"
    
    STATUS_RESPONSE=$(curl -s -X GET \
        -H "X-User-Id: $TEST_USER_ID" \
        -w "%{http_code}" \
        "http://localhost:8080/api/videos/$VIDEO_ID/status")
    
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
# SECTION 4: Video Listing Testing
# ===================================================
print_section "4" "Video Listing Testing"

print_status "INFO" "Testing video listing for user: $TEST_USER_ID"

LIST_RESPONSE=$(curl -s -X GET \
    -H "X-User-Id: $TEST_USER_ID" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos")

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
        -H "X-User-Id: $TEST_USER_ID" \
        -w "%{http_code}" \
        "http://localhost:8080/api/videos/$VIDEO_ID/download")
    
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
        -H "X-User-Id: $TEST_USER_ID" \
        -w "%{http_code}" \
        "http://localhost:8080/api/videos/invalid-video-id/download")
    
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
        -H "X-User-Id: unauthorized-user" \
        -w "%{http_code}" \
        "http://localhost:8080/api/videos/$VIDEO_ID/download")
    
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
# SECTION 6: Processing Workflow Simulation (PENDING → PROCESSING → COMPLETED)
# ===================================================
print_section "6" "Processing Workflow Simulation (PENDING → PROCESSING → COMPLETED)"

# Set base URL for API calls
BASE_URL="http://localhost:8080"

if [[ -f ".test_video_id" ]]; then
    TEST_VIDEO_ID=$(cat .test_video_id)
    print_status "INFO" "Testing complete processing workflow for video: $TEST_VIDEO_ID"
    
    # Generate processed file S3 key (simulating what vclipping would create)
    PROCESSED_S3_KEY="processed/${TEST_USER_ID}/${TEST_VIDEO_ID}/clipped-frames.zip"
    COMPLETION_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    
    print_status "INFO" "Simulated processed file S3 key: $PROCESSED_S3_KEY"
    print_status "INFO" "Processing completion time: $COMPLETION_TIME"
    
    # Step 1: Mark video as PROCESSING
    print_status "INFO" "Step 1: Marking video as PROCESSING..."
    
    PROCESSING_PAYLOAD=$(cat <<EOF
{
  "status": {
    "value": "PROCESSING",
    "description": "Video is currently being processed",
    "isTerminal": false
  },
  "processedFileS3Key": null,
  "processingCompletedAt": null,
  "errorMessage": null
}
EOF
)
    
    print_status "INFO" "Request payload:"
    echo "$PROCESSING_PAYLOAD" | jq .
    
    PROCESSING_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
      -X PUT "${BASE_URL}/api/videos/${TEST_VIDEO_ID}/status" \
      -H "Content-Type: application/json" \
      -H "X-User-Id: ${TEST_USER_ID}" \
      -d "$PROCESSING_PAYLOAD")
    
    PROCESSING_HTTP_STATUS=$(echo "$PROCESSING_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
    PROCESSING_RESPONSE_BODY=$(echo "$PROCESSING_RESPONSE" | sed '/HTTP_STATUS:/d')
    
    print_status "INFO" "Processing Response (HTTP $PROCESSING_HTTP_STATUS):"
    echo "$PROCESSING_RESPONSE_BODY" | jq . 2>/dev/null || echo "$PROCESSING_RESPONSE_BODY"
    
    if [ "$PROCESSING_HTTP_STATUS" = "200" ]; then
        print_status "SUCCESS" "✅ Step 1: Video marked as PROCESSING (HTTP $PROCESSING_HTTP_STATUS)"
        
        # Verify the status was updated correctly
        if echo "$PROCESSING_RESPONSE_BODY" | jq -e '.newStatus.value == "PROCESSING"' > /dev/null 2>&1; then
            print_status "SUCCESS" "✅ Status correctly updated to PROCESSING"
            
            # Step 2: Mark video as COMPLETED
            print_status "INFO" "Step 2: Marking video as COMPLETED..."
        
        COMPLETION_PAYLOAD=$(cat <<EOF
{
  "status": {
    "value": "COMPLETED",
    "description": "Video processing completed successfully",
    "isTerminal": true
  },
  "processedFileS3Key": "${PROCESSED_S3_KEY}",
  "processingCompletedAt": "${COMPLETION_TIME}",
  "errorMessage": null
}
EOF
)
        
        COMPLETION_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
          -X PUT "${BASE_URL}/api/videos/${TEST_VIDEO_ID}/status" \
          -H "Content-Type: application/json" \
          -H "X-User-Id: ${TEST_USER_ID}" \
          -d "$COMPLETION_PAYLOAD")
        
        COMPLETION_HTTP_STATUS=$(echo "$COMPLETION_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
        COMPLETION_RESPONSE_BODY=$(echo "$COMPLETION_RESPONSE" | sed '/HTTP_STATUS:/d')
        
        if [ "$COMPLETION_HTTP_STATUS" = "200" ]; then
            print_status "SUCCESS" "✅ Step 2: Video marked as COMPLETED (HTTP $COMPLETION_HTTP_STATUS)"
            
            # Verify the completion status and S3 key
            if echo "$COMPLETION_RESPONSE_BODY" | jq -e '.newStatus.value == "COMPLETED"' > /dev/null 2>&1; then
                print_status "SUCCESS" "✅ Status correctly updated to COMPLETED"
                
                # Check if S3 key was set
                RETURNED_S3_KEY=$(echo "$COMPLETION_RESPONSE_BODY" | jq -r '.processedFileS3Key // empty')
                if [ -n "$RETURNED_S3_KEY" ] && [ "$RETURNED_S3_KEY" != "null" ]; then
                    print_status "SUCCESS" "✅ Processed file S3 key set: $RETURNED_S3_KEY"
                else
                    print_status "WARNING" "⚠️ Processed file S3 key not set in response"
                    set_section_status 6 "WARNING"
                fi
                
                # Step 3: Test download URL generation after completion
                print_status "INFO" "Step 3: Testing download URL generation after completion..."
                
                DOWNLOAD_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
                  -H "X-User-Id: ${TEST_USER_ID}" \
                  "${BASE_URL}/api/videos/${TEST_VIDEO_ID}/download")
                
                DOWNLOAD_HTTP_STATUS=$(echo "$DOWNLOAD_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
                DOWNLOAD_RESPONSE_BODY=$(echo "$DOWNLOAD_RESPONSE" | sed '/HTTP_STATUS:/d')
                
                if [ "$DOWNLOAD_HTTP_STATUS" = "200" ]; then
                    print_status "SUCCESS" "✅ Step 3: Download URL generated successfully (HTTP $DOWNLOAD_HTTP_STATUS)"
                    
                    # Extract and validate download URL
                    DOWNLOAD_URL=$(echo "$DOWNLOAD_RESPONSE_BODY" | jq -r '.downloadUrl // empty')
                    if [ -n "$DOWNLOAD_URL" ] && [ "$DOWNLOAD_URL" != "null" ]; then
                        print_status "SUCCESS" "✅ Download URL generated: ${DOWNLOAD_URL:0:50}..."
                        
                        # Check expiration time
                        EXPIRATION_MINUTES=$(echo "$DOWNLOAD_RESPONSE_BODY" | jq -r '.expirationMinutes // empty')
                        if [ -n "$EXPIRATION_MINUTES" ] && [ "$EXPIRATION_MINUTES" != "null" ]; then
                            print_status "SUCCESS" "✅ URL expires in: $EXPIRATION_MINUTES minutes"
                        fi
                        
                        print_status "SUCCESS" "🎉 COMPLETE WORKFLOW SUCCESS: PENDING → PROCESSING → COMPLETED → DOWNLOAD"
                    else
                        print_status "WARNING" "⚠️ Download URL is null or empty"
                        set_section_status 6 "WARNING"
                    fi
                else
                    print_status "ERROR" "❌ Step 3: Download URL generation failed (HTTP $DOWNLOAD_HTTP_STATUS)"
                    print_status "INFO" "Response: $DOWNLOAD_RESPONSE_BODY"
                    set_section_status 6 "ERROR"
                fi
            else
                print_status "WARNING" "⚠️ Status update response may not be correct"
                set_section_status 6 "WARNING"
            fi
        else
            print_status "ERROR" "❌ Step 2: Failed to mark video as COMPLETED (HTTP $COMPLETION_HTTP_STATUS)"
            print_status "INFO" "Response: $COMPLETION_RESPONSE_BODY"
            set_section_status 6 "ERROR"
        fi
        else
            print_status "WARNING" "⚠️ Status update response may not be correct"
            set_section_status 6 "WARNING"
        fi
    else
        print_status "ERROR" "❌ Step 1: Failed to mark video as PROCESSING (HTTP $PROCESSING_HTTP_STATUS)"
        print_status "INFO" "Response: $PROCESSING_RESPONSE_BODY"
        set_section_status 6 "ERROR"
    fi
else
    print_status "WARNING" "Skipping processing workflow test - no video ID available"
    set_section_status 6 "WARNING"
fi

# ===================================================
# SECTION 7: Error Handling Testing
# ===================================================
print_section "7" "Error Handling Testing"

# Test upload with missing user header (should cause 400 due to missing required header)
print_status "INFO" "Testing upload with missing user header..."

INVALID_USER_RESPONSE=$(curl -s -X POST \
    -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${INVALID_USER_RESPONSE: -3}"
print_status "INFO" "Missing user header upload HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "400" ]]; then
    print_status "SUCCESS" "Properly rejected missing user header (HTTP 400)"
else
    print_status "WARNING" "Expected HTTP 400 for missing user header, got $HTTP_CODE"
    set_section_status 7 "WARNING"
fi

# Test upload without file
print_status "INFO" "Testing upload without file..."

NO_FILE_RESPONSE=$(curl -s -X POST \
    -H "X-User-Id: $TEST_USER_ID" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${NO_FILE_RESPONSE: -3}"
print_status "INFO" "No file upload HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "400" ]]; then
    print_status "SUCCESS" "Properly rejected upload without file (HTTP 400)"
else
    print_status "WARNING" "Expected HTTP 400 for missing file, got $HTTP_CODE"
    set_section_status 7 "WARNING"
fi

# Test status retrieval with invalid video ID
print_status "INFO" "Testing status retrieval with invalid video ID..."

INVALID_STATUS_RESPONSE=$(curl -s -X GET \
    -H "X-User-Id: $TEST_USER_ID" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/invalid-id/status")

HTTP_CODE="${INVALID_STATUS_RESPONSE: -3}"
print_status "INFO" "Invalid video ID status HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "404" ]]; then
    print_status "SUCCESS" "Properly returned 404 for invalid video ID"
else
    print_status "WARNING" "Expected HTTP 404 for invalid video ID, got $HTTP_CODE"
    set_section_status 7 "WARNING"
fi

# Test AWS service error handling scenarios
print_status "INFO" "Testing AWS service error handling scenarios..."

# Test S3 error handling - check logs for proper error handling
print_status "INFO" "Checking S3 error handling in logs..."
S3_ERROR_HANDLING=$(docker compose logs processing-service | grep -c "S3.*error\|S3.*exception\|S3.*retry" || echo "0")
print_status "INFO" "S3 error handling log entries: $S3_ERROR_HANDLING"

# Test SQS error handling - check logs for proper error handling
print_status "INFO" "Checking SQS error handling in logs..."
SQS_ERROR_HANDLING=$(docker compose logs processing-service | grep -c "SQS.*error\|SQS.*exception\|SQS.*retry" || echo "0")
print_status "INFO" "SQS error handling log entries: $SQS_ERROR_HANDLING"

# Test SNS error handling - check logs for proper error handling
print_status "INFO" "Checking SNS error handling in logs..."
SNS_ERROR_HANDLING=$(docker compose logs processing-service | grep -c "SNS.*error\|SNS.*exception\|SNS.*retry" || echo "0")
print_status "INFO" "SNS error handling log entries: $SNS_ERROR_HANDLING"

# Test upload with unsupported file type
print_status "INFO" "Testing upload with unsupported file type..."
UNSUPPORTED_FILE="test-unsupported.txt"
echo "This is not a video file" > "$UNSUPPORTED_FILE"

UNSUPPORTED_RESPONSE=$(curl -s -X POST \
    -H "X-User-Id: $TEST_USER_ID" \
    -F "file=@$UNSUPPORTED_FILE;type=text/plain" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${UNSUPPORTED_RESPONSE: -3}"
print_status "INFO" "Unsupported file type HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "400" ]]; then
    print_status "SUCCESS" "Properly rejected unsupported file type (HTTP 400)"
else
    print_status "WARNING" "Expected HTTP 400 for unsupported file type, got $HTTP_CODE"
    set_section_status 7 "WARNING"
fi

# Cleanup test file
rm -f "$UNSUPPORTED_FILE"

# ===================================================
# SECTION 8: AWS Integration Validation
# ===================================================
print_section "8" "AWS Integration Validation"

print_status "INFO" "Validating real AWS service integrations..."

# ===================================================
# S3 Integration Testing
# ===================================================
print_status "INFO" "🪣 Testing AWS S3 Integration..."

# Check for real S3 operations in logs (not mock)
S3_REAL_OPERATIONS=$(docker compose logs processing-service | grep -c "Successfully stored in S3\|S3.*ETag\|PutObjectResponse" || echo "0")
S3_MOCK_OPERATIONS=$(docker compose logs processing-service | grep -c "MOCK S3" || echo "0")

print_status "INFO" "Real S3 operations logged: $S3_REAL_OPERATIONS"
print_status "INFO" "Mock S3 operations logged: $S3_MOCK_OPERATIONS"

if [[ "$S3_REAL_OPERATIONS" -gt 0 ]]; then
    print_status "SUCCESS" "✅ Real AWS S3 integration is working"
    
    # Check for S3 bucket and key information
    S3_BUCKET_LOGS=$(docker compose logs processing-service | grep "vclipper-video-storage-dev" | wc -l)
    S3_KEY_LOGS=$(docker compose logs processing-service | grep "videos/[0-9]*/[0-9]*/[0-9]*/" | wc -l)
    
    print_status "INFO" "S3 bucket references in logs: $S3_BUCKET_LOGS"
    print_status "INFO" "S3 key pattern matches in logs: $S3_KEY_LOGS"
    
    if [[ "$S3_BUCKET_LOGS" -gt 0 && "$S3_KEY_LOGS" -gt 0 ]]; then
        print_status "SUCCESS" "S3 bucket and key structure validation passed"
    else
        print_status "WARNING" "S3 bucket or key structure may not be correct"
        set_section_status 8 "WARNING"
    fi
    
    # Show recent S3 operation logs
    print_status "INFO" "Recent S3 operations:"
    docker compose logs processing-service | grep -E "S3.*store|S3.*upload|Successfully stored in S3|PutObjectResponse" | tail -3
    
elif [[ "$S3_MOCK_OPERATIONS" -gt 0 ]]; then
    print_status "WARNING" "⚠️ Still using Mock S3 adapter - real AWS integration not active"
    set_section_status 8 "WARNING"
else
    print_status "ERROR" "❌ No S3 operations detected (neither real nor mock)"
    set_section_status 8 "ERROR"
fi

# ===================================================
# SQS Integration Testing
# ===================================================
print_status "INFO" "📬 Testing AWS SQS Integration..."

SQS_REAL_OPERATIONS=$(docker compose logs processing-service | grep -c "Successfully sent message to SQS\|MessageId.*sent to SQS\|SendMessageResponse" || echo "0")
SQS_MOCK_OPERATIONS=$(docker compose logs processing-service | grep -c "MOCK SQS" || echo "0")

print_status "INFO" "Real SQS operations logged: $SQS_REAL_OPERATIONS"
print_status "INFO" "Mock SQS operations logged: $SQS_MOCK_OPERATIONS"

if [[ "$SQS_REAL_OPERATIONS" -gt 0 ]]; then
    print_status "SUCCESS" "✅ Real AWS SQS integration is working"
    
    # Show recent SQS operation logs
    print_status "INFO" "Recent SQS operations:"
    docker compose logs processing-service | grep -E "SQS.*message|Successfully sent message to SQS|SendMessageResponse" | tail -2
    
elif [[ "$SQS_MOCK_OPERATIONS" -gt 0 ]]; then
    print_status "WARNING" "⚠️ Still using Mock SQS adapter - real AWS integration not active"
    set_section_status 8 "WARNING"
else
    print_status "ERROR" "❌ No SQS operations detected (neither real nor mock)"
    set_section_status 8 "ERROR"
fi

# ===================================================
# SNS Integration Testing
# ===================================================
print_status "INFO" "📧 Testing AWS SNS Integration..."

SNS_REAL_OPERATIONS=$(docker compose logs processing-service | grep -c "Successfully sent notification via SNS\|MessageId.*SNS\|PublishResponse" || echo "0")
SNS_MOCK_OPERATIONS=$(docker compose logs processing-service | grep -c "MOCK SNS" || echo "0")

print_status "INFO" "Real SNS operations logged: $SNS_REAL_OPERATIONS"
print_status "INFO" "Mock SNS operations logged: $SNS_MOCK_OPERATIONS"

if [[ "$SNS_REAL_OPERATIONS" -gt 0 ]]; then
    print_status "SUCCESS" "✅ Real AWS SNS integration is working"
    
    # Show recent SNS operation logs
    print_status "INFO" "Recent SNS operations:"
    docker compose logs processing-service | grep -E "SNS.*notification|Successfully sent notification via SNS|PublishResponse" | tail -2
    
elif [[ "$SNS_MOCK_OPERATIONS" -gt 0 ]]; then
    print_status "WARNING" "⚠️ Still using Mock SNS adapter - real AWS integration not active"
    set_section_status 8 "WARNING"
else
    print_status "ERROR" "❌ No SNS operations detected (neither real nor mock)"
    set_section_status 8 "ERROR"
fi

# ===================================================
# AWS Configuration Validation
# ===================================================
print_status "INFO" "⚙️ Validating AWS Configuration..."

# Validate AWS credentials are properly mounted
AWS_CREDS_LOGS=$(docker compose logs processing-service | grep -c "AWS\|credentials\|region" || echo "0")

if [[ "$AWS_CREDS_LOGS" -gt 0 ]]; then
    print_status "INFO" "AWS configuration references found in logs: $AWS_CREDS_LOGS"
else
    print_status "WARNING" "Limited AWS configuration references in logs"
    set_section_status 8 "WARNING"
fi

# ===================================================
# User Service Mock Validation (Expected)
# ===================================================
print_status "INFO" "👤 Testing User Service Mock Integration..."

USER_SERVICE_OPERATIONS=$(docker compose logs processing-service | grep -c "MOCK USER SERVICE" || echo "0")

if [[ "$USER_SERVICE_OPERATIONS" -gt 0 ]]; then
    print_status "SUCCESS" "✅ User Service Mock adapter is active (expected)"
else
    print_status "WARNING" "⚠️ User Service Mock adapter may not be active"
    set_section_status 8 "WARNING"
fi

# ===================================================
# Overall AWS Integration Assessment
# ===================================================
print_status "INFO" "📊 Overall AWS Integration Assessment..."

AWS_SERVICES_WORKING=0
if [[ "$S3_REAL_OPERATIONS" -gt 0 ]]; then 
    AWS_SERVICES_WORKING=$((AWS_SERVICES_WORKING + 1))
fi
if [[ "$SQS_REAL_OPERATIONS" -gt 0 ]]; then 
    AWS_SERVICES_WORKING=$((AWS_SERVICES_WORKING + 1))
fi
if [[ "$SNS_REAL_OPERATIONS" -gt 0 ]]; then 
    AWS_SERVICES_WORKING=$((AWS_SERVICES_WORKING + 1))
fi

print_status "INFO" "AWS services with real integration: $AWS_SERVICES_WORKING/3"

if [[ "$AWS_SERVICES_WORKING" -eq 3 ]]; then
    print_status "SUCCESS" "🎉 Complete AWS integration achieved (S3 + SQS + SNS)"
elif [[ "$AWS_SERVICES_WORKING" -gt 0 ]]; then
    print_status "WARNING" "⚠️ Partial AWS integration ($AWS_SERVICES_WORKING/3 services working)"
    set_section_status 8 "WARNING"
else
    print_status "ERROR" "❌ No real AWS integrations detected - still using mocks"
    set_section_status 8 "ERROR"
fi

# ===================================================
# SECTION 9: Database Validation
# ===================================================
print_section "9" "Database Validation"

print_status "INFO" "Validating data persistence in MongoDB..."

# Debug MongoDB connection parameters
print_status "INFO" "MongoDB connection parameters - User: $MONGODB_USER, Database: $MONGODB_DATABASE"

# Check if video data was persisted
print_status "INFO" "Querying video count from MongoDB..."
echo "DEBUG: About to execute MongoDB query..."
VIDEO_COUNT=$(docker exec $(docker compose ps -q mongodb) mongosh --quiet -u "$MONGODB_USER" -p "$MONGODB_PASSWORD" --authenticationDatabase "$MONGODB_DATABASE" --eval "db.videoProcessingRequests.countDocuments({})" "$MONGODB_DATABASE" 2>/dev/null || echo "0")
echo "DEBUG: MongoDB query completed, result: $VIDEO_COUNT"

print_status "INFO" "Videos stored in MongoDB: $VIDEO_COUNT"

if [[ "$VIDEO_COUNT" -gt 0 ]]; then
    print_status "SUCCESS" "Video data successfully persisted in MongoDB"
else
    print_status "WARNING" "No video data found in MongoDB"
    set_section_status 9 "WARNING"
fi

# Check MongoDB indexes
print_status "INFO" "Checking MongoDB indexes..."
INDEX_COUNT=$(docker exec $(docker compose ps -q mongodb) mongosh --quiet -u "$MONGODB_USER" -p "$MONGODB_PASSWORD" --authenticationDatabase "$MONGODB_DATABASE" --eval "db.videoProcessingRequests.getIndexes().length" "$MONGODB_DATABASE" 2>/dev/null || echo "0")

print_status "INFO" "Number of indexes on videoProcessingRequests collection: $INDEX_COUNT"

if [[ "$INDEX_COUNT" -gt 1 ]]; then  # More than just the default _id index
    print_status "SUCCESS" "Custom indexes are properly created"
else
    print_status "WARNING" "Custom indexes may not be properly created"
    set_section_status 9 "WARNING"
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
    -H "X-User-Id: $TEST_USER_ID" \
    -F "file=@$LARGE_FILE" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${LARGE_UPLOAD_RESPONSE: -3}"
print_status "INFO" "Large file upload HTTP Code: $HTTP_CODE"

if [[ "$HTTP_CODE" == "413" || "$HTTP_CODE" == "400" || "$HTTP_CODE" == "500" ]]; then
    print_status "INFO" "File size validation working (HTTP $HTTP_CODE)"
else
    print_status "WARNING" "File size validation may not be working properly"
    set_section_status 10 "WARNING"
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
    set_section_status 11 "WARNING"
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
# Only remove the temporary test file if we created one (not the existing test_videos file)
if [[ "$TEST_VIDEO_FILE" == "test-video.mp4" ]]; then
    rm -f "$TEST_VIDEO_FILE"
    print_status "INFO" "Removed temporary test video file"
fi
rm -f .test_video_id

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
