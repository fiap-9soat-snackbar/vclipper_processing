#!/bin/bash

# VClipper Test Helpers Library
# Common functions for all test modules

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Test configuration
TEST_USER_ID="test-user-123"
BASE_URL="http://localhost:8080"
TEST_VIDEO_FILE="test_videos/better_test_video.mp4"

# Print functions with consistent formatting
print_status() {
    local level="$1"
    local message="$2"
    
    case "$level" in
        "SUCCESS") echo -e "${GREEN}✅ $message${NC}" ;;
        "ERROR") echo -e "${RED}❌ $message${NC}" ;;
        "WARNING") echo -e "${YELLOW}⚠️  $message${NC}" ;;
        "INFO") echo -e "${BLUE}ℹ️  $message${NC}" ;;
        *) echo -e "$message" ;;
    esac
}

print_section() {
    local section_num="$1"
    local section_name="$2"
    
    echo ""
    echo -e "${BOLD}${PURPLE}==============================================${NC}"
    echo -e "${BOLD}${BLUE}   SECTION $section_num: $section_name${NC}"
    echo -e "${BOLD}${PURPLE}==============================================${NC}"
    echo ""
}

print_test_header() {
    local test_name="$1"
    echo -e "\n${YELLOW}--- $test_name ---${NC}"
}

# Service readiness checking
wait_for_service() {
    local service_name="$1"
    local url="$2"
    local max_attempts="$3"
    local delay="${4:-3}"
    
    print_status "INFO" "Waiting for $service_name to be ready..."
    
    for ((i=1; i<=max_attempts; i++)); do
        print_status "INFO" "Attempt $i/$max_attempts: Testing $service_name endpoint..."
        
        if curl -s --connect-timeout 5 --max-time 10 "$url" > /dev/null 2>&1; then
            print_status "SUCCESS" "$service_name is ready!"
            return 0
        fi
        
        if [[ $i -lt $max_attempts ]]; then
            sleep "$delay"
        fi
    done
    
    print_status "ERROR" "$service_name failed to become ready after $max_attempts attempts"
    return 1
}

# JSON response validation
validate_json_response() {
    local response="$1"
    local expected_field="$2"
    
    if echo "$response" | jq -e ".$expected_field" > /dev/null 2>&1; then
        return 0
    else
        return 1
    fi
}

# HTTP status validation
validate_http_status() {
    local actual_status="$1"
    local expected_status="$2"
    local context="$3"
    
    if [[ "$actual_status" == "$expected_status" ]]; then
        print_status "SUCCESS" "$context (HTTP $actual_status)"
        return 0
    else
        print_status "ERROR" "$context - Expected HTTP $expected_status, got $actual_status"
        return 1
    fi
}

# Extract HTTP status from curl response
extract_http_status() {
    local response="$1"
    echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2
}

# Extract response body from curl response
extract_response_body() {
    local response="$1"
    echo "$response" | sed '/HTTP_STATUS:/d'
}

# Generate unique test identifier
generate_test_id() {
    echo "test-$(date +%s)-$$"
}

# Cleanup test data files
cleanup_test_data() {
    local pattern="$1"
    if [[ -n "$pattern" ]]; then
        rm -f "$pattern" 2>/dev/null || true
    fi
}

# Check if file exists and is not empty
file_exists_and_not_empty() {
    local file="$1"
    [[ -f "$file" && -s "$file" ]]
}

# Create test video file if it doesn't exist
ensure_test_video() {
    if [[ -f "$TEST_VIDEO_FILE" && -s "$TEST_VIDEO_FILE" ]]; then
        print_status "INFO" "Test video already exists: $TEST_VIDEO_FILE"
        return 0
    fi
    
    create_test_video "$TEST_VIDEO_FILE"
}

# Create a proper MP4 test video file
create_test_video() {
    local output_file="$1"
    local video_dir="$(dirname "$output_file")"
    
    print_status "INFO" "Creating test video file..."
    
    # Create output directory if it doesn't exist
    mkdir -p "$video_dir"
    
    # Create a complete MP4 file with proper structure
    # This includes MP4 boxes to ensure proper MIME type detection
    {
        # ftyp box (file type box) - 32 bytes
        printf '\x00\x00\x00\x20'    # Box size: 32 bytes
        printf 'ftyp'                # Box type: file type
        printf 'mp41'                # Major brand: MP4 v1
        printf '\x00\x00\x00\x00'    # Minor version: 0
        printf 'mp41'                # Compatible brand 1: MP4 v1
        printf 'isom'                # Compatible brand 2: ISO base media
        printf '\x00\x00\x00\x00'    # Padding
        printf '\x00\x00\x00\x00'    # Padding
        
        # moov box (movie box) - container for metadata
        printf '\x00\x00\x00\x6C'    # Box size: 108 bytes
        printf 'moov'                # Box type: movie
        
        # mvhd box (movie header box) inside moov
        printf '\x00\x00\x00\x68'    # Box size: 104 bytes
        printf 'mvhd'                # Box type: movie header
        printf '\x00'                # Version
        printf '\x00\x00\x00'        # Flags
        printf '\x00\x00\x00\x00'    # Creation time
        printf '\x00\x00\x00\x00'    # Modification time
        printf '\x00\x00\x03\xE8'    # Time scale (1000)
        printf '\x00\x00\x03\xE8'    # Duration (1000 = 1 second)
        printf '\x00\x01\x00\x00'    # Rate (1.0)
        printf '\x01\x00'            # Volume (1.0)
        printf '\x00\x00'            # Reserved
        printf '\x00\x00\x00\x00'    # Reserved
        printf '\x00\x00\x00\x00'    # Reserved
        # Matrix (identity matrix)
        printf '\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x00\x40\x00\x00\x00'
        printf '\x00\x00\x00\x00'    # Preview time
        printf '\x00\x00\x00\x00'    # Preview duration
        printf '\x00\x00\x00\x00'    # Poster time
        printf '\x00\x00\x00\x00'    # Selection time
        printf '\x00\x00\x00\x00'    # Selection duration
        printf '\x00\x00\x00\x00'    # Current time
        printf '\x00\x00\x00\x02'    # Next track ID
        
        # mdat box (media data box) - minimal
        printf '\x00\x00\x00\x10'    # Box size: 16 bytes
        printf 'mdat'                # Box type: media data
        printf '\x00\x00\x00\x00'    # Dummy data
        printf '\x00\x00\x00\x00'    # Dummy data
        
    } > "$output_file"
    
    # Verify the file was created successfully
    if [[ -f "$output_file" && -s "$output_file" ]]; then
        local file_size=$(stat -c%s "$output_file" 2>/dev/null || stat -f%z "$output_file" 2>/dev/null)
        local mime_type=$(file --mime-type -b "$output_file" 2>/dev/null || echo "unknown")
        
        print_status "SUCCESS" "Test video created: $output_file ($file_size bytes)"
        
        if [[ "$mime_type" == "video/mp4" ]]; then
            print_status "SUCCESS" "MIME type validation: $mime_type ✅"
        else
            print_status "WARNING" "MIME type detected as: $mime_type"
        fi
        
        return 0
    else
        print_status "ERROR" "Failed to create test video file"
        return 1
    fi
}

# Run command with timeout and error handling
run_with_timeout() {
    local timeout_duration="$1"
    local description="$2"
    shift 2
    
    print_status "INFO" "Running: $description"
    
    if timeout "$timeout_duration" "$@"; then
        print_status "SUCCESS" "$description completed"
        return 0
    else
        local exit_code=$?
        if [[ $exit_code -eq 124 ]]; then
            print_status "ERROR" "$description timed out after ${timeout_duration}s"
        else
            print_status "ERROR" "$description failed with exit code $exit_code"
        fi
        return $exit_code
    fi
}

# Validate JSON structure
validate_json_structure() {
    local json="$1"
    local required_fields="$2"
    
    for field in $required_fields; do
        if ! echo "$json" | jq -e ".$field" > /dev/null 2>&1; then
            print_status "ERROR" "Missing required field: $field"
            return 1
        fi
    done
    
    return 0
}

# Check if string contains substring
string_contains() {
    local string="$1"
    local substring="$2"
    [[ "$string" == *"$substring"* ]]
}

# Get current timestamp
get_timestamp() {
    date -u +%Y-%m-%dT%H:%M:%SZ
}

# Log test result
log_test_result() {
    local test_name="$1"
    local result="$2"
    local details="$3"
    
    local timestamp=$(get_timestamp)
    echo "[$timestamp] $test_name: $result - $details" >> test-results.log
}

# Initialize test environment
init_test_environment() {
    print_status "INFO" "Initializing test environment..."
    
    # Ensure test video exists
    ensure_test_video
    
    # Clean up any previous test artifacts
    cleanup_test_data "*.test.*"
    cleanup_test_data ".test_*"
    
    # Create test results log
    echo "# Test Results - $(date)" > test-results.log
    
    print_status "SUCCESS" "Test environment initialized"
}

# Export functions for use in other scripts
export -f print_status print_section print_test_header
export -f wait_for_service validate_json_response validate_http_status
export -f extract_http_status extract_response_body
export -f generate_test_id cleanup_test_data file_exists_and_not_empty
export -f ensure_test_video run_with_timeout validate_json_structure
export -f string_contains get_timestamp log_test_result init_test_environment

# Export variables
export RED GREEN YELLOW BLUE PURPLE BOLD NC
export TEST_USER_ID BASE_URL TEST_VIDEO_FILE
