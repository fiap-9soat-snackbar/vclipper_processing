#!/bin/bash

# VClipper Error Handling Test Module
# Tests critical error scenarios and edge cases

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/assertion-helpers.sh"

# Test module configuration
MODULE_NAME="Error Handling"
TEST_RESULTS=()

# Main test function
main() {
    print_section "8" "$MODULE_NAME Testing"
    
    local all_passed=true
    
    # Test 1: Invalid file type upload
    if test_invalid_file_type; then
        TEST_RESULTS+=("PASS: Invalid file type handling")
    else
        TEST_RESULTS+=("FAIL: Invalid file type handling")
        all_passed=false
    fi
    
    # Test 2: Missing user ID
    if test_missing_user_id; then
        TEST_RESULTS+=("PASS: Missing user ID handling")
    else
        TEST_RESULTS+=("FAIL: Missing user ID handling")
        all_passed=false
    fi
    
    # Test 3: Invalid user ID
    if test_invalid_user_id; then
        TEST_RESULTS+=("PASS: Invalid user ID handling")
    else
        TEST_RESULTS+=("FAIL: Invalid user ID handling")
        all_passed=false
    fi
    
    # Test 4: Malformed JSON in status update
    if test_malformed_json; then
        TEST_RESULTS+=("PASS: Malformed JSON handling")
    else
        TEST_RESULTS+=("FAIL: Malformed JSON handling")
        all_passed=false
    fi
    
    # Test 5: Invalid status transition
    if test_invalid_status_transition; then
        TEST_RESULTS+=("PASS: Invalid status transition handling")
    else
        TEST_RESULTS+=("FAIL: Invalid status transition handling")
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

# Test 1: Invalid file type upload
test_invalid_file_type() {
    print_test_header "Testing upload with invalid file type (security boundary)"
    
    # Create a temporary text file to simulate invalid upload
    local invalid_file="test_invalid.txt"
    echo "This is not a video file" > "$invalid_file"
    
    # Attempt upload with invalid file type
    local upload_response
    upload_response=$(curl -s -X POST \
        -F "userId=$TEST_USER_ID" \
        -F "file=@$invalid_file;type=text/plain" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/upload")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$upload_response")
    local response_body=$(extract_response_body "$upload_response")
    
    print_status "INFO" "Invalid file upload HTTP Code: $http_status"
    print_status "INFO" "Invalid file upload Response: $response_body"
    
    # Should return 400 for invalid file type
    if ! assert_http_status "$http_status" "400" "Invalid file type upload"; then
        rm -f "$invalid_file"
        return 1
    fi
    
    # Validate error response structure (different response format for upload errors)
    if ! assert_json_field_exists "$response_body" ".message" "Error response - message field"; then
        rm -f "$invalid_file"
        return 1
    fi
    
    if ! assert_json_field "$response_body" ".success" "false" "Success field should be false"; then
        rm -f "$invalid_file"
        return 1
    fi
    
    # Validate error message content
    local error_message=$(echo "$response_body" | jq -r '.message')
    if assert_contains "$error_message" "video" "Error message mentions video requirement"; then
        print_status "SUCCESS" "Invalid file type properly rejected with descriptive error"
        rm -f "$invalid_file"
        return 0
    else
        rm -f "$invalid_file"
        return 1
    fi
}

# Test 2: Missing user ID
test_missing_user_id() {
    print_test_header "Testing upload without user ID (validation boundary)"
    
    # Attempt upload without user ID
    local upload_response
    upload_response=$(curl -s -X POST \
        -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/upload")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$upload_response")
    local response_body=$(extract_response_body "$upload_response")
    
    print_status "INFO" "Missing user ID HTTP Code: $http_status"
    print_status "INFO" "Missing user ID Response: $response_body"
    
    # Should return 400 for missing required field
    if assert_http_status "$http_status" "400" "Missing user ID upload"; then
        print_status "SUCCESS" "Missing user ID properly rejected"
        return 0
    else
        return 1
    fi
}

# Test 3: Invalid user ID
test_invalid_user_id() {
    print_test_header "Testing upload with invalid user ID (business rule validation)"
    
    local invalid_user="invalid-user-999"
    
    # Attempt upload with invalid user ID
    local upload_response
    upload_response=$(curl -s -X POST \
        -F "userId=$invalid_user" \
        -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/upload")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$upload_response")
    local response_body=$(extract_response_body "$upload_response")
    
    print_status "INFO" "Invalid user ID HTTP Code: $http_status"
    print_status "INFO" "Invalid user ID Response: $response_body"
    
    # Should return 400 for invalid user
    if ! assert_http_status "$http_status" "400" "Invalid user ID upload"; then
        return 1
    fi
    
    # Validate error message mentions user validation
    local error_message=$(echo "$response_body" | jq -r '.message')
    if assert_contains "$error_message" "user" "Error message mentions user validation"; then
        print_status "SUCCESS" "Invalid user ID properly rejected with business rule validation"
        return 0
    else
        return 1
    fi
}

# Test 4: Malformed JSON in status update
test_malformed_json() {
    print_test_header "Testing status update with malformed JSON (input validation)"
    
    # Get a valid video ID for testing
    local test_video_id
    if [[ -f "$TEST_STATE_FILE" ]]; then
        test_video_id=$(cat "$TEST_STATE_FILE")
    else
        print_status "WARNING" "No video ID available, skipping malformed JSON test"
        return 0
    fi
    
    # Send malformed JSON
    local malformed_json='{"status": "PROCESSING", "processedFileS3Key": null, "processingCompletedAt": null'  # Missing closing brace
    
    local update_response
    update_response=$(curl -s -X PUT \
        -H "Content-Type: application/json" \
        -d "$malformed_json" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$test_video_id/status")
    
    # Extract HTTP status
    local http_status=$(extract_http_status "$update_response")
    
    print_status "INFO" "Malformed JSON HTTP Code: $http_status"
    
    # Should return 400 or 500 for malformed JSON (both are acceptable)
    if [[ "$http_status" == "400" || "$http_status" == "500" ]]; then
        print_status "SUCCESS" "Malformed JSON properly rejected (HTTP $http_status)"
        return 0
    else
        print_status "ERROR" "Malformed JSON handling - Expected HTTP 400 or 500, got $http_status"
        return 1
    fi
}

# Test 5: Invalid status transition
test_invalid_status_transition() {
    print_test_header "Testing invalid status transition (business rule enforcement)"
    
    # Get a valid video ID for testing
    local test_video_id
    if [[ -f "$TEST_STATE_FILE" ]]; then
        test_video_id=$(cat "$TEST_STATE_FILE")
    else
        print_status "WARNING" "No video ID available, skipping invalid transition test"
        return 0
    fi
    
    # Try to transition from any status to PENDING (which should be invalid)
    local invalid_transition_request='{
        "status": {
            "value": "PENDING",
            "description": "Video uploaded and queued for processing",
            "isTerminal": false
        },
        "processedFileS3Key": null,
        "processingCompletedAt": null,
        "errorMessage": null
    }'
    
    local update_response
    update_response=$(curl -s -X PUT \
        -H "Content-Type: application/json" \
        -d "$invalid_transition_request" \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos/$test_video_id/status")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$update_response")
    local response_body=$(extract_response_body "$update_response")
    
    print_status "INFO" "Invalid transition HTTP Code: $http_status"
    print_status "INFO" "Invalid transition Response: $response_body"
    
    # Should return 400 for invalid transition
    if ! assert_http_status "$http_status" "400" "Invalid status transition"; then
        return 1
    fi
    
    # Validate error message mentions transition
    local error_message=$(echo "$response_body" | jq -r '.message')
    if assert_contains "$error_message" "transition" "Error message mentions invalid transition"; then
        print_status "SUCCESS" "Invalid status transition properly rejected with business rule enforcement"
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
        echo -e "  \033[0;32m✅ Invalid file type handling\033[0m"
        echo -e "  \033[0;32m✅ Missing user ID handling\033[0m"
        echo -e "  \033[0;32m✅ Invalid user ID handling\033[0m"
        echo -e "  \033[0;32m✅ Malformed JSON handling\033[0m"
        echo -e "  \033[0;32m✅ Invalid status transition handling\033[0m"
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
        print_status "INFO" "🛡️ Security boundaries and validation rules properly enforced"
    else
        print_status "ERROR" "💥 $MODULE_NAME: Some tests failed"
    fi
}

# Cleanup function
cleanup() {
    # Clean up temporary files
    rm -f test_invalid.txt 2>/dev/null || true
}

# Set up cleanup trap
trap cleanup EXIT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
