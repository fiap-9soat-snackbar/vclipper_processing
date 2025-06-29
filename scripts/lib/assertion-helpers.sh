#!/bin/bash

# VClipper Assertion Helpers Library
# Test assertion functions

# Source test helpers for consistent output
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-helpers.sh"

# HTTP status code assertions
assert_http_status() {
    local actual="$1"
    local expected="$2"
    local context="${3:-HTTP request}"
    
    if [[ "$actual" == "$expected" ]]; then
        print_status "SUCCESS" "$context (HTTP $actual)"
        return 0
    else
        print_status "ERROR" "$context - Expected HTTP $expected, got $actual"
        return 1
    fi
}

# JSON field assertions
assert_json_field() {
    local json="$1"
    local field_path="$2"
    local expected_value="$3"
    local context="${4:-JSON field validation}"
    
    local actual_value
    actual_value=$(echo "$json" | jq -r "$field_path" 2>/dev/null)
    
    if [[ $? -ne 0 ]]; then
        print_status "ERROR" "$context - Failed to extract field: $field_path"
        return 1
    fi
    
    if [[ "$actual_value" == "$expected_value" ]]; then
        print_status "SUCCESS" "$context - $field_path = $expected_value"
        return 0
    else
        print_status "ERROR" "$context - Expected $field_path = $expected_value, got $actual_value"
        return 1
    fi
}

# JSON field exists assertion
assert_json_field_exists() {
    local json="$1"
    local field_path="$2"
    local context="${3:-JSON field existence}"
    
    if echo "$json" | jq -e "$field_path" > /dev/null 2>&1; then
        print_status "SUCCESS" "$context - Field exists: $field_path"
        return 0
    else
        print_status "ERROR" "$context - Field missing: $field_path"
        return 1
    fi
}

# Non-empty assertion
assert_not_empty() {
    local value="$1"
    local context="${2:-Value check}"
    
    if [[ -n "$value" && "$value" != "null" ]]; then
        print_status "SUCCESS" "$context - Value is not empty"
        return 0
    else
        print_status "ERROR" "$context - Value is empty or null"
        return 1
    fi
}

# String contains assertion
assert_contains() {
    local haystack="$1"
    local needle="$2"
    local context="${3:-String contains check}"
    
    if [[ "$haystack" == *"$needle"* ]]; then
        print_status "SUCCESS" "$context - String contains '$needle'"
        return 0
    else
        print_status "ERROR" "$context - String does not contain '$needle'"
        return 1
    fi
}

# String does not contain assertion
assert_not_contains() {
    local haystack="$1"
    local needle="$2"
    local context="${3:-String does not contain check}"
    
    if [[ "$haystack" != *"$needle"* ]]; then
        print_status "SUCCESS" "$context - String does not contain '$needle'"
        return 0
    else
        print_status "ERROR" "$context - String unexpectedly contains '$needle'"
        return 1
    fi
}

# Numeric comparison assertions
assert_greater_than() {
    local actual="$1"
    local expected="$2"
    local context="${3:-Numeric comparison}"
    
    if (( $(echo "$actual > $expected" | bc -l) )); then
        print_status "SUCCESS" "$context - $actual > $expected"
        return 0
    else
        print_status "ERROR" "$context - Expected $actual > $expected"
        return 1
    fi
}

assert_less_than() {
    local actual="$1"
    local expected="$2"
    local context="${3:-Numeric comparison}"
    
    if (( $(echo "$actual < $expected" | bc -l) )); then
        print_status "SUCCESS" "$context - $actual < $expected"
        return 0
    else
        print_status "ERROR" "$context - Expected $actual < $expected"
        return 1
    fi
}

assert_equals() {
    local actual="$1"
    local expected="$2"
    local context="${3:-Equality check}"
    
    if [[ "$actual" == "$expected" ]]; then
        print_status "SUCCESS" "$context - Values are equal"
        return 0
    else
        print_status "ERROR" "$context - Expected '$expected', got '$actual'"
        return 1
    fi
}

# File existence assertions
assert_file_exists() {
    local file_path="$1"
    local context="${2:-File existence check}"
    
    if [[ -f "$file_path" ]]; then
        print_status "SUCCESS" "$context - File exists: $file_path"
        return 0
    else
        print_status "ERROR" "$context - File does not exist: $file_path"
        return 1
    fi
}

# URL validation assertion
assert_valid_url() {
    local url="$1"
    local context="${2:-URL validation}"
    
    # Basic URL pattern check
    if [[ "$url" =~ ^https?://[a-zA-Z0-9.-]+.*$ ]]; then
        print_status "SUCCESS" "$context - Valid URL format"
        return 0
    else
        print_status "ERROR" "$context - Invalid URL format: $url"
        return 1
    fi
}

# UUID validation assertion
assert_valid_uuid() {
    local uuid="$1"
    local context="${2:-UUID validation}"
    
    # UUID pattern check
    if [[ "$uuid" =~ ^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$ ]]; then
        print_status "SUCCESS" "$context - Valid UUID format"
        return 0
    else
        print_status "ERROR" "$context - Invalid UUID format: $uuid"
        return 1
    fi
}

# Response structure validation for common API patterns
assert_success_response() {
    local response="$1"
    local context="${2:-Success response validation}"
    
    # Check for success field
    if assert_json_field "$response" ".success" "true" "$context - success field"; then
        # Check for message field
        if assert_json_field_exists "$response" ".message" "$context - message field"; then
            return 0
        fi
    fi
    return 1
}

assert_error_response() {
    local response="$1"
    local context="${2:-Error response validation}"
    
    # Check for success field being false or error message
    if echo "$response" | jq -e '.success == false or .error' > /dev/null 2>&1; then
        print_status "SUCCESS" "$context - Valid error response structure"
        return 0
    else
        print_status "ERROR" "$context - Invalid error response structure"
        return 1
    fi
}

# Video-specific assertions
assert_video_response() {
    local response="$1"
    local context="${2:-Video response validation}"
    
    local required_fields=".videoId .userId .originalFilename .status"
    
    for field in $required_fields; do
        if ! assert_json_field_exists "$response" "$field" "$context"; then
            return 1
        fi
    done
    
    # Validate UUID format for videoId
    local video_id=$(echo "$response" | jq -r '.videoId')
    assert_valid_uuid "$video_id" "$context - videoId"
}

assert_status_response() {
    local response="$1"
    local context="${2:-Status response validation}"
    
    # Check required status fields
    local required_fields=".status.value .status.description .status.isTerminal .status.finished"
    
    for field in $required_fields; do
        if ! assert_json_field_exists "$response" "$field" "$context"; then
            return 1
        fi
    done
    
    return 0
}

# Export functions
export -f assert_http_status assert_json_field assert_json_field_exists
export -f assert_not_empty assert_contains assert_not_contains
export -f assert_greater_than assert_less_than assert_equals
export -f assert_file_exists assert_valid_url assert_valid_uuid
export -f assert_success_response assert_error_response
export -f assert_video_response assert_status_response
