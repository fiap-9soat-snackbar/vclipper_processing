#!/bin/bash

# VClipper Test Configuration
# Centralized configuration for all test modules

# Application Configuration
export BASE_URL="http://localhost:8080"
export TEST_USER_ID="test-user-123"

# File Paths
export TEST_VIDEO_FILE="test_videos/better_test_video.mp4"
export TEST_RESULTS_FILE="test-results.log"
export TEST_STATE_FILE=".test_video_id"

# Test Timeouts (in seconds)
export SERVICE_STARTUP_TIMEOUT=30
export HTTP_REQUEST_TIMEOUT=10
export DOCKER_BUILD_TIMEOUT=300
export DOCKER_STARTUP_TIMEOUT=120

# Test Retry Configuration
export MAX_RETRY_ATTEMPTS=3
export RETRY_DELAY=3

# MongoDB Configuration
export MONGODB_USER="vclipperuser"
export MONGODB_PASSWORD="vclipperpass"
export MONGODB_DATABASE="vclipper"

# AWS Configuration (for testing)
export AWS_REGION="us-east-1"
export S3_BUCKET_NAME="vclipper-video-storage-dev"

# Test Data Configuration
export VALID_VIDEO_FORMATS=("mp4" "avi" "mov" "wmv" "flv" "webm")
export INVALID_VIDEO_FORMATS=("txt" "pdf" "jpg" "png")
export MAX_FILE_SIZE_MB=100

# Test Module Configuration
declare -A TEST_MODULES=(
    ["01-environment-setup"]="Environment Setup"
    ["02-health-validation"]="Health Validation"
    ["03-video-upload"]="Video Upload"
    ["04-video-status"]="Video Status"
    ["05-video-listing"]="Video Listing"
    ["06-download-workflow"]="Download Workflow"
    ["07-processing-simulation"]="Processing Simulation"
    ["08-error-handling"]="Error Handling"
    ["09-aws-integration"]="AWS Integration"
)

# Test execution order
export TEST_EXECUTION_ORDER=(
    "01-environment-setup"
    "02-health-validation"
    "03-video-upload"
    "04-video-status"
    "05-video-listing"
    "06-download-workflow"
    "07-processing-simulation"
    "08-error-handling"
    "09-aws-integration"
)

# Test result tracking
declare -A TEST_RESULTS=()
declare -A TEST_DURATIONS=()

# Logging Configuration
export LOG_LEVEL="INFO"  # DEBUG, INFO, WARNING, ERROR
export ENABLE_DETAILED_LOGGING=true
export SAVE_TEST_ARTIFACTS=true

# Cleanup Configuration
export CLEANUP_ON_SUCCESS=true
export CLEANUP_ON_FAILURE=false
export KEEP_CONTAINERS_ON_FAILURE=true

# Performance Thresholds
export MAX_RESPONSE_TIME_MS=5000
export MAX_UPLOAD_TIME_S=30
export MAX_PROCESSING_TIME_S=60

# Validation Configuration
export VALIDATE_JSON_RESPONSES=true
export VALIDATE_HTTP_HEADERS=true
export VALIDATE_RESPONSE_TIMES=true

# Feature Flags for Test Modules
export ENABLE_ENVIRONMENT_SETUP=true
export ENABLE_HEALTH_VALIDATION=true
export ENABLE_VIDEO_UPLOAD=true
export ENABLE_VIDEO_STATUS=true
export ENABLE_VIDEO_LISTING=true
export ENABLE_DOWNLOAD_WORKFLOW=true
export ENABLE_PROCESSING_SIMULATION=true
export ENABLE_ERROR_HANDLING=true
export ENABLE_AWS_INTEGRATION=true

# Debug Configuration
export DEBUG_MODE=false
export VERBOSE_OUTPUT=false
export SAVE_CURL_RESPONSES=false

# Helper functions for configuration
get_test_module_name() {
    local module_key="$1"
    echo "${TEST_MODULES[$module_key]:-Unknown Module}"
}

is_module_enabled() {
    local module_key="$1"
    local var_name="ENABLE_$(echo "$module_key" | tr '[:lower:]' '[:upper:]' | tr '-' '_')"
    local enabled="${!var_name:-true}"
    [[ "$enabled" == "true" ]]
}

get_timeout_for_operation() {
    local operation="$1"
    case "$operation" in
        "service_startup") echo "$SERVICE_STARTUP_TIMEOUT" ;;
        "http_request") echo "$HTTP_REQUEST_TIMEOUT" ;;
        "docker_build") echo "$DOCKER_BUILD_TIMEOUT" ;;
        "docker_startup") echo "$DOCKER_STARTUP_TIMEOUT" ;;
        *) echo "30" ;;  # Default timeout
    esac
}

# Export configuration arrays and functions
export TEST_MODULES TEST_EXECUTION_ORDER TEST_RESULTS TEST_DURATIONS
export -f get_test_module_name is_module_enabled get_timeout_for_operation
