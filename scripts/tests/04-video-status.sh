#!/bin/bash

# VClipper Video Status Test Module
# Tests video status retrieval functionality

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/assertion-helpers.sh"

# Test module configuration
MODULE_NAME="Video Status"
TEST_RESULTS=()

# Main test function
main() {
    print_section "4" "$MODULE_NAME Testing"
    
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
    
    # Test 2: Valid status retrieval
    if test_valid_status_retrieval; then
        TEST_RESULTS+=("PASS: Valid status retrieval")
    else
        TEST_RESULTS+=("FAIL: Valid status retrieval")
        all_passed=false
    fi
    
    # Test 3: Status response validation
    if test_status_response_validation; then
        TEST_RESULTS+=("PASS: Status response validation")
    else
        TEST_RESULTS+=("FAIL: Status response validation")
        all_passed=false
    fi
    
    # Test 4: Invalid video ID handling
    if test_invalid_video_id; then
        TEST_RESULTS+=("PASS: Invalid video ID handling")
    else
        TEST_RESULTS+=("FAIL: Invalid video ID handling")
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

# Test 1: Get video ID from previous test
test_get_video_id() {
    print_test_header "Retrieving video ID from previous test"
    
    if [[ -f "$TEST_STATE_FILE" ]]; then
        VIDEO_ID=$(cat "$TEST_STATE_FILE")
        if assert_not_empty "$VIDEO_ID" "Video ID from state file"; then
            if assert_valid_uuid "$VIDEO_ID" "Video ID format validation"; then
                print_status "SUCCESS" "Using video ID: $VIDEO_ID"
                return 0
            fi
        fi
    fi
    
    print_status "ERROR" "No valid video ID available from previous test"
    return 1
}

# Test 2: Valid status retrieval
test_valid_status_retrieval() {
    print_test_header "Testing video status retrieval for video: $VIDEO_ID"
    
    # Perform status request
    local status_response
    status_response=$(curl -s -X GET \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$VIDEO_ID/status?userId=$TEST_USER_ID")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$status_response")
    local response_body=$(extract_response_body "$status_response")
    
    print_status "INFO" "Status HTTP Code: $http_status"
    print_status "INFO" "Status Response: $response_body"
    
    # Validate HTTP status
    if ! assert_http_status "$http_status" "200" "Status retrieval"; then
        return 1
    fi
    
    # Store response for next test
    echo "$response_body" > .status_response.json
    
    return 0
}

# Test 3: Status response validation
test_status_response_validation() {
    print_test_header "Validating status response structure"
    
    # Read stored response
    if [[ ! -f ".status_response.json" ]]; then
        print_status "ERROR" "Status response not available for validation"
        return 1
    fi
    
    local response_body=$(cat .status_response.json)
    
    # Validate response contains expected status fields
    if ! assert_json_field_exists "$response_body" ".videoId" "Status response - videoId field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".userId" "Status response - userId field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".status" "Status response - status field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".originalFilename" "Status response - originalFilename field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".createdAt" "Status response - createdAt field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".updatedAt" "Status response - updatedAt field"; then
        return 1
    fi
    
    print_status "SUCCESS" "Response contains expected status fields"
    
    # Validate specific field values
    if ! assert_json_field "$response_body" ".videoId" "$VIDEO_ID" "Video ID match"; then
        return 1
    fi
    
    if ! assert_json_field "$response_body" ".userId" "$TEST_USER_ID" "User ID match"; then
        return 1
    fi
    
    # Extract and display status information
    local status_value=$(echo "$response_body" | jq -r '.status.value')
    local filename=$(echo "$response_body" | jq -r '.originalFilename')
    
    print_status "INFO" "Video Status: $status_value"
    print_status "INFO" "Original Filename: $filename"
    
    return 0
}

# Test 4: Invalid video ID handling
test_invalid_video_id() {
    print_test_header "Testing status retrieval with invalid video ID"
    
    local invalid_id="invalid-video-id"
    
    # Perform status request with invalid ID
    local status_response
    status_response=$(curl -s -X GET \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$invalid_id/status?userId=$TEST_USER_ID")
    
    # Extract HTTP status
    local http_status=$(extract_http_status "$status_response")
    
    print_status "INFO" "Invalid video ID status HTTP Code: $http_status"
    
    # Should return 404 for invalid video ID
    if assert_http_status "$http_status" "404" "Invalid video ID handling"; then
        print_status "SUCCESS" "Properly returned 404 for invalid video ID"
        return 0
    else
        return 1
    fi
}

# Print module summary
print_module_summary() {
    local all_passed="$1"
    
    echo ""
    print_status "INFO" "📊 $MODULE_NAME Test Summary:"
    
    # Simple, direct approach - just show the results we know
    if $all_passed; then
        echo -e "  \033[0;32m✅ Video ID retrieval\033[0m"
        echo -e "  \033[0;32m✅ Valid status retrieval\033[0m"
        echo -e "  \033[0;32m✅ Status response validation\033[0m"
        echo -e "  \033[0;32m✅ Invalid video ID handling\033[0m"
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
    rm -f .status_response.json 2>/dev/null || true
}

# Set up cleanup trap
trap cleanup EXIT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
