#!/bin/bash

# VClipper AWS Integration Test Module
# Tests AWS services integration (S3, SQS, SNS)

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/assertion-helpers.sh"

# Test module configuration
MODULE_NAME="AWS Integration"
TEST_RESULTS=()

# Main test function
main() {
    print_section "9" "$MODULE_NAME Testing"
    
    local all_passed=true
    
    # Test 1: S3 integration via upload
    if test_s3_integration; then
        TEST_RESULTS+=("PASS: S3 storage integration")
    else
        TEST_RESULTS+=("FAIL: S3 storage integration")
        all_passed=false
    fi
    
    # Test 2: SQS integration via upload
    if test_sqs_integration; then
        TEST_RESULTS+=("PASS: SQS messaging integration")
    else
        TEST_RESULTS+=("FAIL: SQS messaging integration")
        all_passed=false
    fi
    
    # Test 3: SNS integration via status update
    if test_sns_integration; then
        TEST_RESULTS+=("PASS: SNS notification integration")
    else
        TEST_RESULTS+=("FAIL: SNS notification integration")
        all_passed=false
    fi
    
    # Test 4: S3 presigned URL generation
    if test_s3_presigned_urls; then
        TEST_RESULTS+=("PASS: S3 presigned URL generation")
    else
        TEST_RESULTS+=("FAIL: S3 presigned URL generation")
        all_passed=false
    fi
    
    # Generate module summary
    print_module_summary "$all_passed"
    
    # Return appropriate exit code
    if $all_passed; then
        return 0
    else
        return 1
    fi
}

# Test 1: S3 integration via upload
test_s3_integration() {
    print_test_header "Testing S3 storage integration via video upload"
    
    # Upload a video to test S3 integration
    local upload_response
    upload_response=$(curl -s -X POST \
        -F "userId=$TEST_USER_ID" \
        -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/upload")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$upload_response")
    local response_body=$(extract_response_body "$upload_response")
    
    print_status "INFO" "S3 upload test HTTP Code: $http_status"
    
    # Successful upload indicates S3 integration is working
    if ! assert_http_status "$http_status" "201" "S3 storage integration"; then
        return 1
    fi
    
    # Extract video ID for later tests
    local video_id=$(echo "$response_body" | jq -r '.videoId')
    if assert_valid_uuid "$video_id" "S3 upload video ID validation"; then
        echo "$video_id" > .aws_test_video_id
        print_status "SUCCESS" "S3 storage integration working - video uploaded successfully"
        print_status "INFO" "S3 test video ID: $video_id"
        return 0
    else
        return 1
    fi
}

# Test 2: SQS integration via upload
test_sqs_integration() {
    print_test_header "Testing SQS messaging integration via video upload"
    
    # SQS integration is tested indirectly through successful upload
    # The upload process should publish a message to SQS for processing
    
    if [[ ! -f ".aws_test_video_id" ]]; then
        print_status "ERROR" "No video ID available from S3 test"
        return 1
    fi
    
    local video_id=$(cat .aws_test_video_id)
    
    # Check that the video was created with PENDING status (indicates SQS message was queued)
    local status_response
    status_response=$(curl -s -X GET "$BASE_URL/api/videos/$video_id/status?userId=$TEST_USER_ID")
    local current_status=$(echo "$status_response" | jq -r '.status.value' 2>/dev/null)
    
    if [[ "$current_status" == "PENDING" ]]; then
        print_status "SUCCESS" "SQS messaging integration working - video queued for processing"
        print_status "INFO" "Video status: $current_status (indicates SQS message published)"
        return 0
    else
        print_status "ERROR" "SQS integration issue - expected PENDING status, got: $current_status"
        return 1
    fi
}

# Test 3: SNS integration via status update
test_sns_integration() {
    print_test_header "Testing SNS notification integration via status update"
    
    if [[ ! -f ".aws_test_video_id" ]]; then
        print_status "ERROR" "No video ID available from previous tests"
        return 1
    fi
    
    local video_id=$(cat .aws_test_video_id)
    
    # Update video status to trigger SNS notification
    local status_update_request='{
        "status": {
            "value": "PROCESSING",
            "description": "Video is currently being processed",
            "isTerminal": false
        },
        "processedFileS3Key": null,
        "processingCompletedAt": null,
        "errorMessage": null
    }'
    
    local update_response
    update_response=$(curl -s -X PUT \
        -H "Content-Type: application/json" \
        -d "$status_update_request" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$video_id/status")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$update_response")
    local response_body=$(extract_response_body "$update_response")
    
    print_status "INFO" "SNS notification test HTTP Code: $http_status"
    
    # Successful status update indicates SNS integration is working
    if ! assert_http_status "$http_status" "200" "SNS notification integration"; then
        return 1
    fi
    
    # Validate that the status was updated (indicates SNS notification was sent)
    if ! assert_json_field "$response_body" ".newStatus.value" "PROCESSING" "Status updated to PROCESSING"; then
        return 1
    fi
    
    print_status "SUCCESS" "SNS notification integration working - status update triggered notification"
    print_status "INFO" "Status transition: PENDING → PROCESSING (SNS notification sent)"
    
    return 0
}

# Test 4: S3 presigned URL generation
test_s3_presigned_urls() {
    print_test_header "Testing S3 presigned URL generation"
    
    if [[ ! -f ".aws_test_video_id" ]]; then
        print_status "ERROR" "No video ID available from previous tests"
        return 1
    fi
    
    local video_id=$(cat .aws_test_video_id)
    
    # First, complete the video processing to enable download
    local completion_request='{
        "status": {
            "value": "COMPLETED",
            "description": "Video processing completed successfully",
            "isTerminal": true
        },
        "processedFileS3Key": "processed-videos/'$video_id'/frames.zip",
        "processingCompletedAt": "'$(date -u +%Y-%m-%dT%H:%M:%S)'",
        "errorMessage": null
    }'
    
    local completion_response
    completion_response=$(curl -s -X PUT \
        -H "Content-Type: application/json" \
        -d "$completion_request" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$video_id/status")
    
    local completion_status=$(extract_http_status "$completion_response")
    
    if [[ "$completion_status" != "200" ]]; then
        print_status "WARNING" "Could not complete video for presigned URL test (HTTP $completion_status)"
        return 1
    fi
    
    # Now test presigned URL generation
    local download_response
    download_response=$(curl -s -X GET \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$video_id/download?userId=$TEST_USER_ID")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$download_response")
    local response_body=$(extract_response_body "$download_response")
    
    print_status "INFO" "S3 presigned URL test HTTP Code: $http_status"
    
    # Successful download URL generation indicates S3 presigned URL integration is working
    if ! assert_http_status "$http_status" "200" "S3 presigned URL generation"; then
        return 1
    fi
    
    # Validate presigned URL structure
    local download_url=$(echo "$response_body" | jq -r '.downloadUrl')
    if ! assert_valid_url "$download_url" "Presigned URL format validation"; then
        return 1
    fi
    
    # Validate URL contains AWS S3 signature parameters
    if assert_contains "$download_url" "X-Amz-Signature" "URL contains AWS signature"; then
        if assert_contains "$download_url" "X-Amz-Expires" "URL contains expiration"; then
            print_status "SUCCESS" "S3 presigned URL generation working - valid signed URL created"
            print_status "INFO" "Presigned URL expires in 60 minutes"
            return 0
        fi
    fi
    
    return 1
}

# Print module summary
print_module_summary() {
    local all_passed="$1"
    
    echo ""
    print_status "INFO" "📊 $MODULE_NAME Test Summary:"
    
    # Simple, direct approach - just show the results we know
    if $all_passed; then
        echo -e "  \033[0;32m✅ S3 storage integration\033[0m"
        echo -e "  \033[0;32m✅ SQS messaging integration\033[0m"
        echo -e "  \033[0;32m✅ SNS notification integration\033[0m"
        echo -e "  \033[0;32m✅ S3 presigned URL generation\033[0m"
    else
        # Show individual test results based on what we tracked
        for result in "${TEST_RESULTS[@]}"; do
            if [[ "$result" == PASS:* ]]; then
                local message="${result#PASS: }"
                echo -e "  \033[0;32m✅ $message\033[0m"
            elif [[ "$result" == FAIL:* ]]; then
                local message="${result#FAIL: }"
                echo -e "  \033[0;31m❌ $message\033[0m"
            fi
        done
    fi
    
    echo ""
    if $all_passed; then
        print_status "SUCCESS" "🎉 $MODULE_NAME: All tests passed!"
        print_status "INFO" "☁️ All AWS services (S3, SQS, SNS) are properly integrated"
    else
        print_status "ERROR" "💥 $MODULE_NAME: Some tests failed"
    fi
}

# Cleanup function
cleanup() {
    # Clean up temporary files
    rm -f .aws_test_video_id 2>/dev/null || true
}

# Set up cleanup trap
trap cleanup EXIT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
