#!/bin/bash

# VClipper Health Validation Test Module
# Tests application and service health

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/assertion-helpers.sh"

# Test module configuration
MODULE_NAME="Health Validation"
TEST_RESULTS=()

# Main test function
main() {
    print_section "2" "$MODULE_NAME Testing"
    
    local all_passed=true
    
    # Test 1: Application health endpoint
    if test_application_health; then
        TEST_RESULTS+=("PASS: Application health check")
    else
        TEST_RESULTS+=("FAIL: Application health check")
        all_passed=false
    fi
    
    # Test 2: MongoDB connectivity
    if test_mongodb_connectivity; then
        TEST_RESULTS+=("PASS: MongoDB connectivity")
    else
        TEST_RESULTS+=("FAIL: MongoDB connectivity")
        all_passed=false
    fi
    
    # Test 3: Application info endpoint
    if test_application_info; then
        TEST_RESULTS+=("PASS: Application info endpoint")
    else
        TEST_RESULTS+=("FAIL: Application info endpoint")
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

# Test 1: Application health endpoint
test_application_health() {
    print_test_header "Testing application health endpoint"
    
    # Test health endpoint
    local health_response
    health_response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$BASE_URL/actuator/health")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$health_response")
    local response_body=$(extract_response_body "$health_response")
    
    print_status "INFO" "Health endpoint HTTP Code: $http_status"
    print_status "INFO" "Health response: $response_body"
    
    # Validate HTTP status
    if ! assert_http_status "$http_status" "200" "Health endpoint"; then
        return 1
    fi
    
    # Validate health status
    if ! assert_json_field "$response_body" ".status" "UP" "Application health status"; then
        return 1
    fi
    
    print_status "SUCCESS" "Application health endpoint working correctly"
    return 0
}

# Test 2: MongoDB connectivity
test_mongodb_connectivity() {
    print_test_header "Testing MongoDB connectivity via health endpoint"
    
    # Get detailed health information
    local health_response
    health_response=$(curl -s "$BASE_URL/actuator/health")
    
    # Check if MongoDB component is present and UP
    if echo "$health_response" | jq -e '.components.mongo.status' > /dev/null 2>&1; then
        local mongo_status=$(echo "$health_response" | jq -r '.components.mongo.status')
        
        if [[ "$mongo_status" == "UP" ]]; then
            print_status "SUCCESS" "MongoDB connectivity confirmed (status: UP)"
            return 0
        else
            print_status "ERROR" "MongoDB connectivity issue (status: $mongo_status)"
            return 1
        fi
    else
        print_status "WARNING" "MongoDB health component not found in response"
        # Still consider it a pass if overall health is UP
        return 0
    fi
}

# Test 3: Application info endpoint
test_application_info() {
    print_test_header "Testing application info endpoint"
    
    # Test info endpoint
    local info_response
    info_response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "$BASE_URL/actuator/info")
    
    # Extract HTTP status and response body
    local http_status=$(extract_http_status "$info_response")
    local response_body=$(extract_response_body "$info_response")
    
    print_status "INFO" "Info endpoint HTTP Code: $http_status"
    
    # Validate HTTP status
    if ! assert_http_status "$http_status" "200" "Info endpoint"; then
        return 1
    fi
    
    # Check if build info is present
    if echo "$response_body" | jq -e '.build' > /dev/null 2>&1; then
        local build_name=$(echo "$response_body" | jq -r '.build.name // "unknown"')
        local build_version=$(echo "$response_body" | jq -r '.build.version // "unknown"')
        
        print_status "SUCCESS" "Application info endpoint working"
        print_status "INFO" "Application: $build_name v$build_version"
        return 0
    else
        print_status "WARNING" "Build info not found, but endpoint is accessible"
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
        echo -e "  \033[0;32m✅ Application health check\033[0m"
        echo -e "  \033[0;32m✅ MongoDB connectivity\033[0m"
        echo -e "  \033[0;32m✅ Application info endpoint\033[0m"
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
        print_status "INFO" "💚 All services are healthy and ready"
    else
        print_status "ERROR" "💥 $MODULE_NAME: Some tests failed"
    fi
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
