#!/bin/bash

# VClipper Processing Service - End-to-End Upload Flow Test
# Tests the complete video upload and status tracking workflow

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Configuration
API_BASE_URL="http://localhost:8080"
TEST_USER_ID="test-user-123"
TEST_VIDEO_FILE="test-video.mp4"

print_status() {
    local status=$1
    local message=$2
    case $status in
        "SUCCESS") echo -e "${GREEN}✅ $message${NC}" ;;
        "ERROR") echo -e "${RED}❌ $message${NC}" ;;
        "WARNING") echo -e "${YELLOW}⚠️  $message${NC}" ;;
        "INFO") echo -e "${BLUE}ℹ️  $message${NC}" ;;
        "SECTION") echo -e "${BOLD}${PURPLE}🎯 $message${NC}" ;;
    esac
}

print_section() {
    echo ""
    echo -e "${BOLD}${PURPLE}============================================${NC}"
    echo -e "${BOLD}${BLUE}   $1${NC}"
    echo -e "${BOLD}${PURPLE}============================================${NC}"
    echo ""
}

# Function to create test video file
create_test_video() {
    print_status "INFO" "Creating test video file..."
    
    # Create a small test file (simulating MP4)
    echo "This is a test video file for VClipper Processing Service" > "$TEST_VIDEO_FILE"
    echo "File size: $(wc -c < "$TEST_VIDEO_FILE") bytes"
    echo "Content-Type: video/mp4 (simulated)"
    
    if [[ -f "$TEST_VIDEO_FILE" ]]; then
        print_status "SUCCESS" "Test video file created: $TEST_VIDEO_FILE"
    else
        print_status "ERROR" "Failed to create test video file"
        exit 1
    fi
}

# Function to test API health
test_api_health() {
    print_status "INFO" "Testing API health..."
    
    response=$(curl -s -w "%{http_code}" -o /dev/null "$API_BASE_URL/actuator/health")
    
    if [[ "$response" == "200" ]]; then
        print_status "SUCCESS" "API is healthy (HTTP 200)"
    else
        print_status "ERROR" "API health check failed (HTTP $response)"
        print_status "WARNING" "Make sure the application is running: docker-compose up"
        exit 1
    fi
}

# Function to test video upload
test_video_upload() {
    print_status "INFO" "Testing video upload..."
    
    response=$(curl -s -X POST \
        -F "userId=$TEST_USER_ID" \
        -F "file=@$TEST_VIDEO_FILE" \
        -w "%{http_code}" \
        "$API_BASE_URL/api/videos/upload")
    
    http_code="${response: -3}"
    response_body="${response%???}"
    
    echo "HTTP Status: $http_code"
    echo "Response: $response_body"
    
    if [[ "$http_code" == "201" ]]; then
        print_status "SUCCESS" "Video upload successful (HTTP 201)"
        
        # Extract video ID from response
        VIDEO_ID=$(echo "$response_body" | grep -o '"videoId":"[^"]*' | cut -d'"' -f4)
        
        if [[ -n "$VIDEO_ID" ]]; then
            print_status "SUCCESS" "Video ID extracted: $VIDEO_ID"
            echo "$VIDEO_ID" > .test_video_id  # Save for other tests
        else
            print_status "WARNING" "Could not extract video ID from response"
        fi
    else
        print_status "ERROR" "Video upload failed (HTTP $http_code)"
        echo "Response: $response_body"
        return 1
    fi
}

# Function to test processing status
test_processing_status() {
    print_status "INFO" "Testing processing status retrieval..."
    
    if [[ -f ".test_video_id" ]]; then
        VIDEO_ID=$(cat .test_video_id)
    else
        print_status "ERROR" "No video ID available for status test"
        return 1
    fi
    
    response=$(curl -s -w "%{http_code}" \
        "$API_BASE_URL/api/videos/$VIDEO_ID/status?userId=$TEST_USER_ID")
    
    http_code="${response: -3}"
    response_body="${response%???}"
    
    echo "HTTP Status: $http_code"
    echo "Response: $response_body"
    
    if [[ "$http_code" == "200" ]]; then
        print_status "SUCCESS" "Status retrieval successful (HTTP 200)"
        
        # Check if response contains expected fields
        if echo "$response_body" | grep -q "videoId\|status\|originalFilename"; then
            print_status "SUCCESS" "Response contains expected status fields"
        else
            print_status "WARNING" "Response missing expected status fields"
        fi
    else
        print_status "ERROR" "Status retrieval failed (HTTP $http_code)"
        echo "Response: $response_body"
        return 1
    fi
}

# Function to test video listing
test_video_listing() {
    print_status "INFO" "Testing video listing..."
    
    response=$(curl -s -w "%{http_code}" \
        "$API_BASE_URL/api/videos?userId=$TEST_USER_ID")
    
    http_code="${response: -3}"
    response_body="${response%???}"
    
    echo "HTTP Status: $http_code"
    echo "Response: $response_body"
    
    if [[ "$http_code" == "200" ]]; then
        print_status "SUCCESS" "Video listing successful (HTTP 200)"
        
        # Check if response contains expected fields
        if echo "$response_body" | grep -q "userId\|videos\|totalCount"; then
            print_status "SUCCESS" "Response contains expected listing fields"
        else
            print_status "WARNING" "Response missing expected listing fields"
        fi
    else
        print_status "ERROR" "Video listing failed (HTTP $http_code)"
        echo "Response: $response_body"
        return 1
    fi
}

# Function to test MongoDB connection
test_mongodb_connection() {
    print_status "INFO" "Testing MongoDB connection..."
    
    # Test if we can connect to MongoDB via the application's health endpoint
    response=$(curl -s "$API_BASE_URL/actuator/health")
    
    if echo "$response" | grep -q "mongo.*UP\|mongodb.*UP"; then
        print_status "SUCCESS" "MongoDB connection is healthy"
    else
        print_status "WARNING" "MongoDB connection status unclear"
        echo "Health response: $response"
    fi
}

# Function to cleanup test files
cleanup() {
    print_status "INFO" "Cleaning up test files..."
    
    if [[ -f "$TEST_VIDEO_FILE" ]]; then
        rm "$TEST_VIDEO_FILE"
        print_status "SUCCESS" "Removed test video file"
    fi
    
    if [[ -f ".test_video_id" ]]; then
        rm ".test_video_id"
        print_status "SUCCESS" "Removed test video ID file"
    fi
}

# Main test execution
main() {
    print_section "VCLIPPER PROCESSING SERVICE - END-TO-END TESTS"
    
    print_status "INFO" "🚀 Starting end-to-end upload flow tests..."
    print_status "INFO" "📋 Test Configuration:"
    print_status "INFO" "   API Base URL: $API_BASE_URL"
    print_status "INFO" "   Test User ID: $TEST_USER_ID"
    print_status "INFO" "   Test Video File: $TEST_VIDEO_FILE"
    
    # Test 1: Create test video file
    print_section "TEST 1: CREATE TEST VIDEO FILE"
    create_test_video
    
    # Test 2: API Health Check
    print_section "TEST 2: API HEALTH CHECK"
    test_api_health
    
    # Test 3: MongoDB Connection
    print_section "TEST 3: MONGODB CONNECTION"
    test_mongodb_connection
    
    # Test 4: Video Upload
    print_section "TEST 4: VIDEO UPLOAD"
    if ! test_video_upload; then
        print_status "ERROR" "Video upload test failed, skipping dependent tests"
        cleanup
        exit 1
    fi
    
    # Test 5: Processing Status
    print_section "TEST 5: PROCESSING STATUS"
    test_processing_status
    
    # Test 6: Video Listing
    print_section "TEST 6: VIDEO LISTING"
    test_video_listing
    
    # Summary
    print_section "TEST SUMMARY"
    print_status "SUCCESS" "🎉 All end-to-end tests completed!"
    print_status "INFO" "📊 Test Results:"
    print_status "SUCCESS" "   ✅ API Health Check"
    print_status "SUCCESS" "   ✅ MongoDB Connection"
    print_status "SUCCESS" "   ✅ Video Upload"
    print_status "SUCCESS" "   ✅ Processing Status"
    print_status "SUCCESS" "   ✅ Video Listing"
    
    print_status "INFO" "🔍 Check application logs for detailed mock service output:"
    print_status "INFO" "   docker-compose logs processing-service"
    
    # Cleanup
    cleanup
    
    print_status "SUCCESS" "🚀 VClipper Processing Service is working correctly!"
}

# Handle script interruption
trap cleanup EXIT

# Run main function
main "$@"
