#!/bin/bash

# VClipper Video Listing Test Module
# Tests video listing functionality

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/assertion-helpers.sh"

# Test module configuration
MODULE_NAME="Video Listing"
TEST_RESULTS=()

# Main test function
main() {
    print_section "5" "$MODULE_NAME Testing"
    
    local all_passed=true
    
    # Test 1: Valid video listing
    if test_valid_video_listing; then
        TEST_RESULTS+=("PASS: Valid video listing")
    else
        TEST_RESULTS+=("FAIL: Valid video listing")
        all_passed=false
    fi
    
    # Test 2: Listing response validation
    if test_listing_response_validation; then
        TEST_RESULTS+=("PASS: Listing response validation")
    else
        TEST_RESULTS+=("FAIL: Listing response validation")
        all_passed=false
    fi
    
    # Test 3: Video count validation
    if test_video_count_validation; then
        TEST_RESULTS+=("PASS: Video count validation")
    else
        TEST_RESULTS+=("FAIL: Video count validation")
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

# Test 1: Valid video listing
test_valid_video_listing() {
    print_test_header "Testing video listing for user: $TEST_USER_ID"
    
    # Perform listing request
    local listing_response
    listing_response=$(curl -s -X GET \
        -w "\nHTTP_STATUS:%{http_code}" \
        "$BASE_URL/api/videos?userId=$TEST_USER_ID")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$listing_response")
    local response_body=$(extract_response_body "$listing_response")
    
    print_status "INFO" "List HTTP Code: $http_status"
    print_status "INFO" "List Response: $response_body"
    
    # Validate HTTP status
    if ! assert_http_status "$http_status" "200" "Video listing"; then
        return 1
    fi
    
    # Store response for next tests
    echo "$response_body" > .listing_response.json
    
    return 0
}

# Test 2: Listing response validation
test_listing_response_validation() {
    print_test_header "Validating listing response structure"
    
    # Read stored response
    if [[ ! -f ".listing_response.json" ]]; then
        print_status "ERROR" "Listing response not available for validation"
        return 1
    fi
    
    local response_body=$(cat .listing_response.json)
    
    # Validate response contains expected listing fields
    if ! assert_json_field_exists "$response_body" ".userId" "Listing response - userId field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".videos" "Listing response - videos field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".totalCount" "Listing response - totalCount field"; then
        return 1
    fi
    
    print_status "SUCCESS" "Response contains expected listing fields"
    
    # Validate user ID matches
    if ! assert_json_field "$response_body" ".userId" "$TEST_USER_ID" "User ID match in listing"; then
        return 1
    fi
    
    # Validate videos array structure
    local videos_count=$(echo "$response_body" | jq '.videos | length')
    if ! assert_greater_than "$videos_count" "0" "Videos array not empty"; then
        return 1
    fi
    
    # Validate first video structure (should be our uploaded video)
    if ! assert_json_field_exists "$response_body" ".videos[0].videoId" "First video - videoId field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".videos[0].originalFilename" "First video - originalFilename field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".videos[0].status" "First video - status field"; then
        return 1
    fi
    
    if ! assert_json_field_exists "$response_body" ".videos[0].createdAt" "First video - createdAt field"; then
        return 1
    fi
    
    return 0
}

# Test 3: Video count validation
test_video_count_validation() {
    print_test_header "Validating video count consistency"
    
    # Read stored response
    if [[ ! -f ".listing_response.json" ]]; then
        print_status "ERROR" "Listing response not available for validation"
        return 1
    fi
    
    local response_body=$(cat .listing_response.json)
    
    # Extract counts
    local total_count=$(echo "$response_body" | jq -r '.totalCount')
    local videos_array_length=$(echo "$response_body" | jq '.videos | length')
    
    print_status "INFO" "Total videos for user: $total_count"
    
    # Validate count consistency
    if ! assert_equals "$total_count" "$videos_array_length" "Total count matches videos array length"; then
        return 1
    fi
    
    # Since we uploaded one video, we should have at least 1
    if ! assert_greater_than "$total_count" "0" "At least one video exists"; then
        return 1
    fi
    
    # Validate our uploaded video is in the list
    if [[ -f "$TEST_STATE_FILE" ]]; then
        local expected_video_id=$(cat "$TEST_STATE_FILE")
        local found_video_id=$(echo "$response_body" | jq -r ".videos[] | select(.videoId == \"$expected_video_id\") | .videoId")
        
        if assert_equals "$found_video_id" "$expected_video_id" "Uploaded video found in listing"; then
            print_status "SUCCESS" "Our uploaded video is present in the listing"
            return 0
        else
            print_status "ERROR" "Our uploaded video is missing from the listing"
            return 1
        fi
    else
        print_status "WARNING" "No video ID available to verify in listing"
        return 0
    fi
}

# Print module summary
print_module_summary() {
    local all_passed="$1"
    
    echo ""
    print_status "INFO" "📊 $MODULE_NAME Test Summary:"
    
    # Simple, direct approach - just show the results we know
    if $all_passed; then
        echo -e "  \033[0;32m✅ Valid video listing\033[0m"
        echo -e "  \033[0;32m✅ Listing response validation\033[0m"
        echo -e "  \033[0;32m✅ Video count validation\033[0m"
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
    rm -f .listing_response.json 2>/dev/null || true
}

# Set up cleanup trap
trap cleanup EXIT

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
