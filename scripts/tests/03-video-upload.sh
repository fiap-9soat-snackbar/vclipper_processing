#!/bin/bash

# VClipper Video Upload Test Module
# Tests core video upload functionality

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/assertion-helpers.sh"

# Test module configuration
MODULE_NAME="Video Upload"
TEST_RESULTS=()

# Main test function
main() {
    print_section "3" "$MODULE_NAME Testing"
    
    local all_passed=true
    
    # Test 1: Prepare test video file
    if test_prepare_video_file; then
        TEST_RESULTS+=("PASS: Video file preparation")
    else
        TEST_RESULTS+=("FAIL: Video file preparation")
        all_passed=false
    fi
    
    # Test 2: Valid video upload
    if test_valid_video_upload; then
        TEST_RESULTS+=("PASS: Valid video upload")
    else
        TEST_RESULTS+=("FAIL: Valid video upload")
        all_passed=false
    fi
    
    # Test 3: Upload response validation
    if test_upload_response_validation; then
        TEST_RESULTS+=("PASS: Upload response validation")
    else
        TEST_RESULTS+=("FAIL: Upload response validation")
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

# Test 1: Prepare test video file
test_prepare_video_file() {
    print_test_header "Preparing test video file"
    
    # Ensure test video exists
    ensure_test_video
    
    # Validate test video file
    if assert_file_exists "$TEST_VIDEO_FILE" "Test video file check"; then
        print_status "SUCCESS" "Using test video file: $TEST_VIDEO_FILE"
        return 0
    else
        print_status "ERROR" "Test video file not available"
        return 1
    fi
}

# Test 2: Valid video upload
test_valid_video_upload() {
    print_test_header "Testing video upload with valid user"
    
    # Perform upload request
    local upload_response
    upload_response=$(curl -s -X POST \
        -F "userId=$TEST_USER_ID" \
        -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/upload")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$upload_response")
    local response_body=$(extract_response_body "$upload_response")
    
    print_status "INFO" "Upload HTTP Status: $http_status"
    print_status "INFO" "Upload Response: $response_body"
    
    # Validate HTTP status
    if ! assert_http_status "$http_status" "201" "Video upload"; then
        return 1
    fi
    
    # Store response for next test
    echo "$response_body" > .upload_response.json
    
    return 0
}

# Test 3: Upload response validation
test_upload_response_validation() {
    print_test_header "Validating upload response structure"
    
    # Read stored response
    if [[ ! -f ".upload_response.json" ]]; then
        print_status "ERROR" "Upload response not available for validation"
        return 1
    fi
    
    local response_body=$(cat .upload_response.json)
    
    # Validate response structure
    if ! assert_video_response "$response_body" "Upload response validation"; then
        return 1
    fi
    
    # Extract and validate video ID
    local video_id=$(echo "$response_body" | jq -r '.videoId')
    if ! assert_valid_uuid "$video_id" "Video ID validation"; then
        return 1
    fi
    
    # Store video ID for other test modules
    echo "$video_id" > "$TEST_STATE_FILE"
    print_status "SUCCESS" "Video ID extracted: $video_id"
    
    # Validate other key fields
    if ! assert_json_field "$response_body" ".userId" "$TEST_USER_ID" "User ID validation"; then
        return 1
    fi
    
    if ! assert_json_field "$response_body" ".originalFilename" "better_test_video.mp4" "Filename validation"; then
        return 1
    fi
    
    if ! assert_json_field "$response_body" ".status.value" "PENDING" "Initial status validation"; then
        return 1
    fi
    
    if ! assert_json_field "$response_body" ".success" "true" "Success flag validation"; then
        return 1
    fi
    
    print_status "SUCCESS" "Upload response validation completed"
    return 0
}

# Print module summary
print_module_summary() {
    local all_passed="$1"
    
    echo ""
    print_status "INFO" "📊 $MODULE_NAME Test Summary:"
    
    # Simple, direct approach - just show the results we know
    if $all_passed; then
        echo -e "  \033[0;32m✅ Video file preparation\033[0m"
        echo -e "  \033[0;32m✅ Valid video upload\033[0m"
        echo -e "  \033[0;32m✅ Upload response validation\033[0m"
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
    else
        print_status "ERROR" "💥 $MODULE_NAME: Some tests failed"
    fi
}

# Cleanup function
cleanup() {
    # Clean up temporary files
    rm -f .upload_response.json 2>/dev/null || true
}

# Set up cleanup trap
trap cleanup EXIT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
