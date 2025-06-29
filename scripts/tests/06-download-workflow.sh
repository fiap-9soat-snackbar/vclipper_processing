#!/bin/bash

# VClipper Download Workflow Test Module
# Tests download URL generation functionality

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/assertion-helpers.sh"

# Test module configuration
MODULE_NAME="Download Workflow"
TEST_RESULTS=()

# Main test function
main() {
    print_section "6" "$MODULE_NAME Testing"
    
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
    
    # Test 2: Download URL for PENDING video (should fail)
    if test_pending_video_download; then
        TEST_RESULTS+=("PASS: PENDING video download handling")
    else
        TEST_RESULTS+=("FAIL: PENDING video download handling")
        all_passed=false
    fi
    
    # Test 3: Invalid video ID download
    if test_invalid_video_download; then
        TEST_RESULTS+=("PASS: Invalid video ID handling")
    else
        TEST_RESULTS+=("FAIL: Invalid video ID handling")
        all_passed=false
    fi
    
    # Test 4: Unauthorized user download
    if test_unauthorized_download; then
        TEST_RESULTS+=("PASS: Unauthorized access handling")
    else
        TEST_RESULTS+=("FAIL: Unauthorized access handling")
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

# Test 2: Download URL for PENDING video (should return business error)
test_pending_video_download() {
    print_test_header "Testing download URL for PENDING video (Result pattern validation)"
    
    # Perform download request for PENDING video
    local download_response
    download_response=$(curl -s -X GET \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$VIDEO_ID/download?userId=$TEST_USER_ID")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$download_response")
    local response_body=$(extract_response_body "$download_response")
    
    print_status "INFO" "Download HTTP Code: $http_status"
    print_status "INFO" "Download Response: $response_body"
    
    # Should return 409 Conflict for video not ready
    if ! assert_http_status "$http_status" "409" "Video not ready for download"; then
        return 1
    fi
    
    # Validate response structure (Result pattern)
    if ! assert_json_field_exists "$response_body" ".videoId" "Download response - videoId field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".message" "Download response - message field"; then
        return 1
    fi
    
    # Check if success field exists (it should be present)
    if echo "$response_body" | jq -e 'has("success")' > /dev/null 2>&1; then
        print_status "SUCCESS" "Download response - success field exists"
        
        # Validate success is false
        if ! assert_json_field "$response_body" ".success" "false" "Success field should be false"; then
            return 1
        fi
    else
        print_status "WARNING" "Download response - success field missing (but response is still valid)"
    fi
    
    # Extract and display business error message
    local error_message=$(echo "$response_body" | jq -r '.message')
    print_status "INFO" "Business error message: $error_message"
    
    # Validate message contains expected content
    if assert_contains "$error_message" "not ready for download" "Error message content"; then
        print_status "SUCCESS" "Result pattern response structure is correct"
        return 0
    else
        return 1
    fi
}

# Test 3: Invalid video ID download
test_invalid_video_download() {
    print_test_header "Testing download URL with invalid video ID (security boundary)"
    
    local invalid_id="invalid-video-id"
    
    # Perform download request with invalid ID
    local download_response
    download_response=$(curl -s -X GET \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$invalid_id/download?userId=$TEST_USER_ID")
    
    # Extract HTTP status
    local http_status=$(extract_http_status "$download_response")
    
    print_status "INFO" "Invalid video download HTTP Code: $http_status"
    
    # Should return 404 for invalid video ID
    if assert_http_status "$http_status" "404" "Invalid video ID download"; then
        print_status "SUCCESS" "Security boundary maintained (404 for invalid video)"
        return 0
    else
        return 1
    fi
}

# Test 4: Unauthorized user download
test_unauthorized_download() {
    print_test_header "Testing download URL with unauthorized user (security boundary)"
    
    local unauthorized_user="unauthorized-user"
    
    # Perform download request with unauthorized user
    local download_response
    download_response=$(curl -s -X GET \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$VIDEO_ID/download?userId=$unauthorized_user")
    
    # Extract HTTP status
    local http_status=$(extract_http_status "$download_response")
    
    print_status "INFO" "Unauthorized download HTTP Code: $http_status"
    
    # Should return 404 for unauthorized access (security through obscurity)
    if assert_http_status "$http_status" "404" "Unauthorized user download"; then
        print_status "SUCCESS" "Authorization security maintained (404 for unauthorized user)"
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
        echo -e "  \033[0;32m✅ PENDING video download handling\033[0m"
        echo -e "  \033[0;32m✅ Invalid video ID handling\033[0m"
        echo -e "  \033[0;32m✅ Unauthorized access handling\033[0m"
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
    rm -f .download_response.json 2>/dev/null || true
}

# Set up cleanup trap
trap cleanup EXIT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
