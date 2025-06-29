#!/bin/bash

# VClipper Processing Simulation Test Module
# Tests complete processing workflow simulation

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/assertion-helpers.sh"

# Test module configuration
MODULE_NAME="Processing Simulation"
TEST_RESULTS=()

# Main test function
main() {
    print_section "7" "$MODULE_NAME Testing"
    
    local all_passed=true
    
    # Test 1: Get video ID from previous test
    if test_get_video_id; then
        TEST_RESULTS+=("PASS: Video ID retrieval")
    else
        TEST_RESULTS+=("FAIL: Video ID retrieval")
        all_passed=false
        # Can't continue without video ID
        print_module_summary "$all_passed"
        return 1
    fi
    
    # Test 2: Simulate PENDING → PROCESSING transition
    if test_processing_transition; then
        TEST_RESULTS+=("PASS: PENDING to PROCESSING transition")
    else
        TEST_RESULTS+=("FAIL: PENDING to PROCESSING transition")
        all_passed=false
    fi
    
    # Test 3: Simulate PROCESSING → COMPLETED transition
    if test_completion_transition; then
        TEST_RESULTS+=("PASS: PROCESSING to COMPLETED transition")
    else
        TEST_RESULTS+=("FAIL: PROCESSING to COMPLETED transition")
        all_passed=false
    fi
    
    # Test 4: Verify download URL generation for completed video
    if test_completed_video_download; then
        TEST_RESULTS+=("PASS: Completed video download URL")
    else
        TEST_RESULTS+=("FAIL: Completed video download URL")
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

# Test 1: Get video ID from previous test OR upload a fresh video
test_get_video_id() {
    print_test_header "Getting video ID for processing simulation"
    
    # First, try to get a PENDING video from previous tests
    if [[ -f "$TEST_STATE_FILE" ]]; then
        local existing_video_id=$(cat "$TEST_STATE_FILE")
        
        # Check the current status of this video
        local status_response
        status_response=$(curl -s -X GET "$BASE_URL/api/videos/$existing_video_id/status?userId=$TEST_USER_ID")
        local current_status=$(echo "$status_response" | jq -r '.status.value' 2>/dev/null)
        
        if [[ "$current_status" == "PENDING" ]]; then
            VIDEO_ID="$existing_video_id"
            print_status "SUCCESS" "Using existing PENDING video: $VIDEO_ID"
            return 0
        else
            print_status "INFO" "Existing video is in $current_status status, uploading fresh video for simulation"
        fi
    fi
    
    # Upload a fresh video for processing simulation
    print_status "INFO" "Uploading fresh video for processing workflow simulation..."
    
    local upload_response
    upload_response=$(curl -s -X POST \
        -F "userId=$TEST_USER_ID" \
        -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/upload")
    
    local http_status=$(extract_http_status "$upload_response")
    local response_body=$(extract_response_body "$upload_response")
    
    if [[ "$http_status" == "201" ]]; then
        VIDEO_ID=$(echo "$response_body" | jq -r '.videoId')
        if assert_valid_uuid "$VIDEO_ID" "Fresh video ID validation"; then
            print_status "SUCCESS" "Fresh video uploaded for simulation: $VIDEO_ID"
            # Store the new video ID
            echo "$VIDEO_ID" > "$TEST_STATE_FILE"
            return 0
        fi
    fi
    
    print_status "ERROR" "Failed to get or upload video for processing simulation"
    return 1
}

# Test 2: Simulate PENDING → PROCESSING transition
test_processing_transition() {
    print_test_header "Step 1: Simulating PENDING → PROCESSING transition"
    
    # Prepare status update request with full ProcessingStatus object structure
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
    
    # Perform status update request
    local update_response
    update_response=$(curl -s -X PUT \
        -H "Content-Type: application/json" \
        -d "$status_update_request" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$VIDEO_ID/status")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$update_response")
    local response_body=$(extract_response_body "$update_response")
    
    print_status "INFO" "Processing transition HTTP Code: $http_status"
    print_status "INFO" "Processing transition Response: $response_body"
    
    # Validate HTTP status
    if ! assert_http_status "$http_status" "200" "PENDING to PROCESSING transition"; then
        return 1
    fi
    
    # Validate response structure
    if ! assert_json_field_exists "$response_body" ".videoId" "Processing response - videoId field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".newStatus" "Processing response - newStatus field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".message" "Processing response - message field"; then
        return 1
    fi
    
    # Validate status is now PROCESSING
    if ! assert_json_field "$response_body" ".newStatus.value" "PROCESSING" "New status should be PROCESSING"; then
        return 1
    fi
    
    print_status "SUCCESS" "Video successfully transitioned to PROCESSING status"
    return 0
}

# Test 3: Simulate PROCESSING → COMPLETED transition
test_completion_transition() {
    print_test_header "Step 2: Simulating PROCESSING → COMPLETED transition"
    
    # Generate a mock S3 key for the processed file
    local processed_s3_key="processed-videos/${VIDEO_ID}/frames.zip"
    local completion_timestamp=$(date -u +%Y-%m-%dT%H:%M:%S)
    
    # Prepare completion status update request with full ProcessingStatus object structure
    local completion_request='{
        "status": {
            "value": "COMPLETED",
            "description": "Video processing completed successfully",
            "isTerminal": true
        },
        "processedFileS3Key": "'$processed_s3_key'",
        "processingCompletedAt": "'$completion_timestamp'",
        "errorMessage": null
    }'
    
    # Perform completion status update request
    local completion_response
    completion_response=$(curl -s -X PUT \
        -H "Content-Type: application/json" \
        -d "$completion_request" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$VIDEO_ID/status")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$completion_response")
    local response_body=$(extract_response_body "$completion_response")
    
    print_status "INFO" "Completion transition HTTP Code: $http_status"
    print_status "INFO" "Completion transition Response: $response_body"
    
    # Validate HTTP status
    if ! assert_http_status "$http_status" "200" "PROCESSING to COMPLETED transition"; then
        return 1
    fi
    
    # Validate response structure
    if ! assert_json_field_exists "$response_body" ".videoId" "Completion response - videoId field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".newStatus" "Completion response - newStatus field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".processedFileS3Key" "Completion response - processedFileS3Key field"; then
        return 1
    fi
    
    # Validate status is now COMPLETED
    if ! assert_json_field "$response_body" ".newStatus.value" "COMPLETED" "New status should be COMPLETED"; then
        return 1
    fi
    
    # Validate processed file S3 key
    if ! assert_json_field "$response_body" ".processedFileS3Key" "$processed_s3_key" "Processed file S3 key should match"; then
        return 1
    fi
    
    print_status "SUCCESS" "Video successfully transitioned to COMPLETED status"
    print_status "INFO" "Processed file S3 key: $processed_s3_key"
    
    return 0
}

# Test 4: Verify download URL generation for completed video
test_completed_video_download() {
    print_test_header "Step 3: Testing download URL generation for completed video"
    
    # Perform download request for completed video
    local download_response
    download_response=$(curl -s -X GET \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$VIDEO_ID/download?userId=$TEST_USER_ID")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$download_response")
    local response_body=$(extract_response_body "$download_response")
    
    print_status "INFO" "Download URL HTTP Code: $http_status"
    print_status "INFO" "Download URL Response: $response_body"
    
    # Should return 200 for completed video
    if ! assert_http_status "$http_status" "200" "Download URL generation for completed video"; then
        return 1
    fi
    
    # Validate response structure
    if ! assert_json_field_exists "$response_body" ".videoId" "Download response - videoId field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".downloadUrl" "Download response - downloadUrl field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".expirationMinutes" "Download response - expirationMinutes field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".success" "Download response - success field"; then
        return 1
    fi
    
    # Validate success is true
    if ! assert_json_field "$response_body" ".success" "true" "Success field should be true"; then
        return 1
    fi
    
    # Extract and validate download URL
    local download_url=$(echo "$response_body" | jq -r '.downloadUrl')
    if ! assert_valid_url "$download_url" "Download URL format validation"; then
        return 1
    fi
    
    # Validate expiration time
    local expiration_minutes=$(echo "$response_body" | jq -r '.expirationMinutes')
    if ! assert_equals "$expiration_minutes" "60" "Expiration should be 60 minutes"; then
        return 1
    fi
    
    print_status "SUCCESS" "Download URL generated successfully"
    print_status "INFO" "Download URL: $download_url"
    print_status "INFO" "Expires in: $expiration_minutes minutes"
    
    return 0
}

# Print module summary
print_module_summary() {
    local all_passed="$1"
    
    echo ""
    print_status "INFO" "📊 $MODULE_NAME Test Summary:"
    
    # Simple, direct approach - just show the results we know
    if $all_passed; then
        echo -e "  \033[0;32m✅ Video ID retrieval\033[0m"
        echo -e "  \033[0;32m✅ PENDING to PROCESSING transition\033[0m"
        echo -e "  \033[0;32m✅ PROCESSING to COMPLETED transition\033[0m"
        echo -e "  \033[0;32m✅ Completed video download URL\033[0m"
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
        print_status "INFO" "🔄 Complete workflow validated: PENDING → PROCESSING → COMPLETED → DOWNLOAD"
    else
        print_status "ERROR" "💥 $MODULE_NAME: Some tests failed"
    fi
}

# Cleanup function
cleanup() {
    # Clean up temporary files
    rm -f .processing_response.json .completion_response.json .download_response.json 2>/dev/null || true
}

# Set up cleanup trap
trap cleanup EXIT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
