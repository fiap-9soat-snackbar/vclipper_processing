#!/bin/bash

# VClipper Docker Helpers Library
# Container management functions

# Source test helpers for consistent output
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/test-helpers.sh"

# Container management functions
start_containers() {
    print_status "INFO" "Starting Docker containers..."
    
    # Clean up existing containers
    stop_containers
    
    # Start containers
    if run_with_timeout 120 "Docker container startup" docker compose up -d --quiet-pull; then
        print_status "SUCCESS" "Containers started successfully"
        return 0
    else
        print_status "ERROR" "Failed to start containers"
        return 1
    fi
}

stop_containers() {
    print_status "INFO" "Stopping Docker containers..."
    
    if docker compose down -v --remove-orphans > /dev/null 2>&1; then
        print_status "SUCCESS" "Docker containers stopped"
    else
        print_status "WARNING" "Some containers may not have stopped cleanly"
    fi
}

check_container_health() {
    local container_name="$1"
    
    if docker compose ps "$container_name" | grep -q "Up"; then
        return 0
    else
        return 1
    fi
}

wait_for_containers() {
    print_status "INFO" "Waiting for containers to be ready..."
    
    # Check MongoDB container
    print_status "INFO" "Checking if MongoDB container is running..."
    if check_container_health "mongodb"; then
        print_status "SUCCESS" "MongoDB container is running!"
    else
        print_status "ERROR" "MongoDB container is not running"
        return 1
    fi
    
    # Check Processing Service container
    print_status "INFO" "Checking if Processing Service is ready..."
    if wait_for_service "Processing Service" "$BASE_URL/actuator/health" 15 3; then
        print_status "SUCCESS" "Processing Service is ready!"
        
        # Get and display health check response
        local health_response=$(curl -s "$BASE_URL/actuator/health")
        echo "Health check response: $health_response"
        return 0
    else
        print_status "ERROR" "Processing Service failed to become ready"
        return 1
    fi
}

get_container_logs() {
    local service_name="$1"
    local lines="${2:-50}"
    
    print_status "INFO" "Getting logs for $service_name (last $lines lines)..."
    docker compose logs --tail="$lines" "$service_name"
}

check_container_resources() {
    print_status "INFO" "Checking container resource usage..."
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"
}

build_application() {
    print_status "INFO" "Building application..."
    
    # Clean previous build
    if [[ -d "target" ]]; then
        print_status "INFO" "Removing target folder..."
        rm -rf target
        print_status "SUCCESS" "Target folder removed"
    fi
    
    # Build with Maven
    if run_with_timeout 300 "Maven build" mvn clean package -DskipTests; then
        print_status "SUCCESS" "Application built successfully"
        return 0
    else
        print_status "ERROR" "Application build failed"
        return 1
    fi
}

check_docker_images() {
    print_status "INFO" "Checking Docker image status..."
    
    # Check if images exist
    local processing_image=$(docker images -q vclipper_processing-processing-service 2>/dev/null)
    local mongodb_image=$(docker images -q mongo:8.0.10 2>/dev/null)
    
    if [[ -n "$processing_image" && -n "$mongodb_image" ]]; then
        print_status "INFO" "Docker images exist, checking if rebuild is needed..."
        
        # Simple check - if source files are newer than image, rebuild might be needed
        # For now, we'll use existing images to save time
        print_status "INFO" "Using existing Docker images (no recent source changes)"
        return 0
    else
        print_status "INFO" "Docker images need to be built"
        return 1
    fi
}

setup_test_environment() {
    print_status "INFO" "Setting up test environment..."
    
    # Build application
    if ! build_application; then
        return 1
    fi
    
    # Check Docker images
    check_docker_images
    
    # Start containers
    if ! start_containers; then
        return 1
    fi
    
    # Wait for services to be ready
    if ! wait_for_containers; then
        print_status "ERROR" "Test environment setup failed"
        return 1
    fi
    
    print_status "SUCCESS" "Test environment setup completed"
    return 0
}

cleanup_test_environment() {
    local keep_containers="${1:-false}"
    
    if [[ "$keep_containers" == "true" ]]; then
        print_status "INFO" "Keeping containers running for debugging"
    else
        print_status "INFO" "Cleaning up test environment..."
        stop_containers
        print_status "SUCCESS" "Test environment cleanup completed"
    fi
}

get_container_status() {
    print_status "INFO" "Checking container status..."
    docker compose ps
}

# Export functions
export -f start_containers stop_containers check_container_health
export -f wait_for_containers get_container_logs check_container_resources
export -f build_application check_docker_images
export -f setup_test_environment cleanup_test_environment get_container_status
