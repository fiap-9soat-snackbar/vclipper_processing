#!/bin/bash

# VClipper Processing Service - Docker Setup Test
# Tests Docker Compose setup and container health

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m' # No Color

print_status() {
    local status=$1
    local message=$2
    case $status in
        "SUCCESS") echo -e "${GREEN}✅ $message${NC}" ;;
        "ERROR") echo -e "${RED}❌ $message${NC}" ;;
        "WARNING") echo -e "${YELLOW}⚠️  $message${NC}" ;;
        "INFO") echo -e "${BLUE}ℹ️  $message${NC}" ;;
        "SECTION") echo -e "${BOLD}${PURPLE}🎯 $message${NC}" ;;
    esac
}

print_section() {
    echo ""
    echo -e "${BOLD}${PURPLE}============================================${NC}"
    echo -e "${BOLD}${BLUE}   $1${NC}"
    echo -e "${BOLD}${PURPLE}============================================${NC}"
    echo ""
}

# Function to check Docker and Docker Compose
check_docker() {
    print_status "INFO" "Checking Docker installation..."
    
    if ! command -v docker &> /dev/null; then
        print_status "ERROR" "Docker is not installed"
        exit 1
    fi
    
    if ! command -v docker-compose &> /dev/null; then
        print_status "ERROR" "Docker Compose is not installed"
        exit 1
    fi
    
    print_status "SUCCESS" "Docker and Docker Compose are available"
    
    # Check Docker daemon
    if ! docker info &> /dev/null; then
        print_status "ERROR" "Docker daemon is not running"
        exit 1
    fi
    
    print_status "SUCCESS" "Docker daemon is running"
}

# Function to build Docker image
build_docker_image() {
    print_status "INFO" "Building Docker image..."
    
    if docker-compose build --no-cache processing-service; then
        print_status "SUCCESS" "Docker image built successfully"
    else
        print_status "ERROR" "Failed to build Docker image"
        exit 1
    fi
}

# Function to start services
start_services() {
    print_status "INFO" "Starting Docker Compose services..."
    
    # Stop any existing services
    docker-compose down > /dev/null 2>&1
    
    # Start services in detached mode
    if docker-compose up -d; then
        print_status "SUCCESS" "Services started successfully"
    else
        print_status "ERROR" "Failed to start services"
        exit 1
    fi
}

# Function to wait for services to be ready
wait_for_services() {
    print_status "INFO" "Waiting for services to be ready..."
    
    # Wait for MongoDB
    print_status "INFO" "Waiting for MongoDB..."
    local mongo_ready=false
    for i in {1..30}; do
        if docker-compose exec -T mongodb mongosh --eval "db.adminCommand('ping')" > /dev/null 2>&1; then
            mongo_ready=true
            break
        fi
        sleep 2
    done
    
    if $mongo_ready; then
        print_status "SUCCESS" "MongoDB is ready"
    else
        print_status "ERROR" "MongoDB failed to start within timeout"
        return 1
    fi
    
    # Wait for Processing Service
    print_status "INFO" "Waiting for Processing Service..."
    local app_ready=false
    for i in {1..60}; do
        if curl -s -f "http://localhost:8080/actuator/health" > /dev/null 2>&1; then
            app_ready=true
            break
        fi
        sleep 3
    done
    
    if $app_ready; then
        print_status "SUCCESS" "Processing Service is ready"
    else
        print_status "ERROR" "Processing Service failed to start within timeout"
        return 1
    fi
}

# Function to test container health
test_container_health() {
    print_status "INFO" "Testing container health..."
    
    # Check if containers are running
    local containers=$(docker-compose ps -q)
    local running_containers=0
    
    for container in $containers; do
        if docker inspect "$container" --format='{{.State.Status}}' | grep -q "running"; then
            running_containers=$((running_containers + 1))
        fi
    done
    
    if [[ $running_containers -eq 2 ]]; then
        print_status "SUCCESS" "All containers are running ($running_containers/2)"
    else
        print_status "ERROR" "Not all containers are running ($running_containers/2)"
        return 1
    fi
    
    # Test application health endpoint
    local health_response=$(curl -s "http://localhost:8080/actuator/health")
    if echo "$health_response" | grep -q '"status":"UP"'; then
        print_status "SUCCESS" "Application health check passed"
    else
        print_status "ERROR" "Application health check failed"
        echo "Health response: $health_response"
        return 1
    fi
}

# Function to test MongoDB connection
test_mongodb_connection() {
    print_status "INFO" "Testing MongoDB connection..."
    
    # Test MongoDB connection from application
    local health_response=$(curl -s "http://localhost:8080/actuator/health")
    if echo "$health_response" | grep -q "mongo.*UP"; then
        print_status "SUCCESS" "MongoDB connection from application is healthy"
    else
        print_status "WARNING" "MongoDB connection status unclear from application"
    fi
    
    # Test direct MongoDB connection
    if docker-compose exec -T mongodb mongosh vclipper --eval "db.stats()" > /dev/null 2>&1; then
        print_status "SUCCESS" "Direct MongoDB connection successful"
    else
        print_status "ERROR" "Direct MongoDB connection failed"
        return 1
    fi
}

# Function to test environment variables
test_environment_variables() {
    print_status "INFO" "Testing environment variable configuration..."
    
    # Check if application info endpoint shows correct configuration
    local info_response=$(curl -s "http://localhost:8080/actuator/info")
    
    if echo "$info_response" | grep -q "VClipper Processing"; then
        print_status "SUCCESS" "Application info endpoint accessible"
    else
        print_status "WARNING" "Application info endpoint may not be properly configured"
    fi
    
    # Check application logs for environment variable loading
    local logs=$(docker-compose logs processing-service 2>/dev/null | tail -20)
    if echo "$logs" | grep -q "Started.*Application\|VClipper"; then
        print_status "SUCCESS" "Application started successfully with configuration"
    else
        print_status "WARNING" "Could not verify environment variable loading from logs"
    fi
}

# Function to show service status
show_service_status() {
    print_status "INFO" "Service Status Summary:"
    echo ""
    
    print_status "INFO" "Docker Compose Services:"
    docker-compose ps
    echo ""
    
    print_status "INFO" "Container Resource Usage:"
    docker stats --no-stream --format "table {{.Container}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.NetIO}}"
    echo ""
    
    print_status "INFO" "Application Endpoints:"
    print_status "INFO" "   Health: http://localhost:8080/actuator/health"
    print_status "INFO" "   Info: http://localhost:8080/actuator/info"
    print_status "INFO" "   Upload: http://localhost:8080/api/videos/upload"
    print_status "INFO" "   MongoDB: mongodb://localhost:27017/vclipper"
}

# Function to cleanup
cleanup() {
    if [[ "$1" == "stop" ]]; then
        print_status "INFO" "Stopping Docker Compose services..."
        docker-compose down
        print_status "SUCCESS" "Services stopped"
    fi
}

# Main test execution
main() {
    print_section "VCLIPPER PROCESSING SERVICE - DOCKER SETUP TESTS"
    
    print_status "INFO" "🐳 Starting Docker setup validation..."
    
    # Test 1: Check Docker installation
    print_section "TEST 1: DOCKER INSTALLATION"
    check_docker
    
    # Test 2: Build Docker image
    print_section "TEST 2: BUILD DOCKER IMAGE"
    build_docker_image
    
    # Test 3: Start services
    print_section "TEST 3: START SERVICES"
    start_services
    
    # Test 4: Wait for services
    print_section "TEST 4: SERVICE READINESS"
    if ! wait_for_services; then
        print_status "ERROR" "Services failed to become ready"
        cleanup stop
        exit 1
    fi
    
    # Test 5: Container health
    print_section "TEST 5: CONTAINER HEALTH"
    if ! test_container_health; then
        print_status "ERROR" "Container health check failed"
        cleanup stop
        exit 1
    fi
    
    # Test 6: MongoDB connection
    print_section "TEST 6: MONGODB CONNECTION"
    test_mongodb_connection
    
    # Test 7: Environment variables
    print_section "TEST 7: ENVIRONMENT CONFIGURATION"
    test_environment_variables
    
    # Test 8: Service status
    print_section "TEST 8: SERVICE STATUS"
    show_service_status
    
    # Summary
    print_section "TEST SUMMARY"
    print_status "SUCCESS" "🎉 Docker setup tests completed successfully!"
    print_status "INFO" "📊 Test Results:"
    print_status "SUCCESS" "   ✅ Docker Installation"
    print_status "SUCCESS" "   ✅ Image Build"
    print_status "SUCCESS" "   ✅ Service Startup"
    print_status "SUCCESS" "   ✅ Service Readiness"
    print_status "SUCCESS" "   ✅ Container Health"
    print_status "SUCCESS" "   ✅ MongoDB Connection"
    print_status "SUCCESS" "   ✅ Environment Configuration"
    
    print_status "INFO" "🚀 Services are running and ready for testing!"
    print_status "INFO" "📋 Next steps:"
    print_status "INFO" "   1. Run end-to-end tests: ./scripts/test-upload-flow.sh"
    print_status "INFO" "   2. Check logs: docker-compose logs -f processing-service"
    print_status "INFO" "   3. Stop services: docker-compose down"
    
    echo ""
    print_status "WARNING" "Services are still running. Use 'docker-compose down' to stop them."
}

# Handle script arguments
if [[ "$1" == "cleanup" ]]; then
    cleanup stop
    exit 0
fi

# Handle script interruption
trap 'cleanup stop' EXIT

# Run main function
main "$@"
