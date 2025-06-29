#!/bin/bash

# VClipper Environment Setup Test Module
# Tests environment setup and container readiness

# Get script directory and source libraries
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LIB_DIR="$(dirname "$SCRIPT_DIR")/lib"
CONFIG_DIR="$(dirname "$SCRIPT_DIR")/config"

source "$CONFIG_DIR/test-config.sh"
source "$LIB_DIR/test-helpers.sh"
source "$LIB_DIR/docker-helpers.sh"

# Test module configuration
MODULE_NAME="Environment Setup"
TEST_RESULTS=()

# Main test function
main() {
    print_section "1" "$MODULE_NAME Testing"
    
    local all_passed=true
    
    # Test 1: Docker containers status
    if test_container_status; then
        TEST_RESULTS+=("PASS: Container status check")
    else
        TEST_RESULTS+=("FAIL: Container status check")
        all_passed=false
    fi
    
    # Test 2: Test video file availability
    if test_video_file_availability; then
        TEST_RESULTS+=("PASS: Test video file availability")
    else
        TEST_RESULTS+=("FAIL: Test video file availability")
        all_passed=false
    fi
    
    # Test 3: Environment initialization
    if test_environment_initialization; then
        TEST_RESULTS+=("PASS: Environment initialization")
    else
        TEST_RESULTS+=("FAIL: Environment initialization")
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

# Test 1: Docker containers status
test_container_status() {
    print_test_header "Checking Docker container status"
    
    # Check if containers are running
    if docker compose ps | grep -q "Up"; then
        print_status "SUCCESS" "Docker containers are running"
        
        # Show container status
        print_status "INFO" "Container status:"
        docker compose ps --format "table {{.Service}}\t{{.Status}}\t{{.Ports}}"
        
        return 0
    else
        print_status "ERROR" "Docker containers are not running properly"
        return 1
    fi
}

# Test 2: Test video file availability
test_video_file_availability() {
    print_test_header "Checking test video file availability"
    
    # Check if test video exists (should be created by orchestrator)
    if [[ -f "$TEST_VIDEO_FILE" && -s "$TEST_VIDEO_FILE" ]]; then
        local file_size=$(stat -c%s "$TEST_VIDEO_FILE" 2>/dev/null || stat -f%z "$TEST_VIDEO_FILE" 2>/dev/null)
        print_status "SUCCESS" "Test video file available: $TEST_VIDEO_FILE ($file_size bytes)"
        return 0
    else
        print_status "WARNING" "Test video file not found, creating it now..."
        ensure_test_video
        
        if [[ -f "$TEST_VIDEO_FILE" && -s "$TEST_VIDEO_FILE" ]]; then
            local file_size=$(stat -c%s "$TEST_VIDEO_FILE" 2>/dev/null || stat -f%z "$TEST_VIDEO_FILE" 2>/dev/null)
            print_status "SUCCESS" "Test video file created: $TEST_VIDEO_FILE ($file_size bytes)"
            return 0
        else
            print_status "ERROR" "Failed to create test video file"
            return 1
        fi
    fi
}

# Test 3: Environment initialization
test_environment_initialization() {
    print_test_header "Initializing test environment"
    
    # Initialize test environment
    init_test_environment
    
    # Check if initialization was successful
    if [[ -f "test-results.log" ]]; then
        print_status "SUCCESS" "Test environment initialized successfully"
        print_status "INFO" "Test results log created"
        return 0
    else
        print_status "ERROR" "Test environment initialization failed"
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
        echo -e "  \033[0;32m✅ Container status check\033[0m"
        echo -e "  \033[0;32m✅ Test video file availability\033[0m"
        echo -e "  \033[0;32m✅ Environment initialization\033[0m"
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
        print_status "INFO" "🚀 Environment ready for testing"
    else
        print_status "ERROR" "💥 $MODULE_NAME: Some tests failed"
    fi
}

# Run main function if script is executed directly
if [[ "${BASH_SOURCE[0]}" == "${0}" ]]; then
    main "$@"
fi
