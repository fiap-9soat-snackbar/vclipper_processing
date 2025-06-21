#!/bin/bash

# VClipper Processing Orchestration Service - Quick Test Script
# For rapid feedback during development

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

# Start testing
print_status "INFO" "⚡ Running VClipper Processing Service Quick Tests..."
echo ""

# Test 1: Maven compilation
print_section "PHASE 1: BUILD VERIFICATION"
print_status "INFO" "🔍 Checking Maven compilation..."
if mvn clean compile -q; then
    print_status "SUCCESS" "Maven compilation successful"
else
    print_status "ERROR" "Maven compilation failed"
    exit 1
fi

# Test 2: Domain layer validation
print_section "PHASE 2: DOMAIN LAYER VALIDATION"
print_status "INFO" "🏗️ Validating domain layer structure..."

# Check if all domain files exist
domain_files=(
    "src/main/java/com/vclipper/processing/domain/entity/ProcessingStatus.java"
    "src/main/java/com/vclipper/processing/domain/entity/VideoMetadata.java"
    "src/main/java/com/vclipper/processing/domain/entity/VideoProcessingRequest.java"
    "src/main/java/com/vclipper/processing/domain/entity/User.java"
    "src/main/java/com/vclipper/processing/domain/entity/NotificationType.java"
    "src/main/java/com/vclipper/processing/domain/enums/VideoFormat.java"
    "src/main/java/com/vclipper/processing/domain/exceptions/VideoProcessingException.java"
    "src/main/java/com/vclipper/processing/domain/event/DomainEvent.java"
)

missing_files=0
for file in "${domain_files[@]}"; do
    if [[ -f "$file" ]]; then
        print_status "SUCCESS" "Found: $(basename "$file")"
    else
        print_status "ERROR" "Missing: $file"
        missing_files=$((missing_files + 1))
    fi
done

if [[ $missing_files -eq 0 ]]; then
    print_status "SUCCESS" "All domain files present"
else
    print_status "ERROR" "$missing_files domain files missing"
    exit 1
fi

# Test 3: Clean architecture compliance
print_section "PHASE 3: CLEAN ARCHITECTURE COMPLIANCE"
print_status "INFO" "🧹 Checking clean architecture compliance..."

# Check for infrastructure dependencies in domain layer
if grep -r "import.*aws" src/main/java/com/vclipper/processing/domain/ 2>/dev/null; then
    print_status "ERROR" "Domain layer contains AWS dependencies (clean architecture violation)"
    exit 1
else
    print_status "SUCCESS" "Domain layer is clean (no infrastructure dependencies)"
fi

if grep -r "import.*springframework" src/main/java/com/vclipper/processing/domain/ 2>/dev/null; then
    print_status "ERROR" "Domain layer contains Spring dependencies (clean architecture violation)"
    exit 1
else
    print_status "SUCCESS" "Domain layer is framework-agnostic"
fi

# Check for @Service annotations in application layer (should be clean)
if grep -r "@Service" src/main/java/com/vclipper/processing/application/ 2>/dev/null; then
    print_status "ERROR" "Application layer contains @Service annotations (clean architecture violation)"
    exit 1
else
    print_status "SUCCESS" "Application layer is framework-agnostic (no @Service annotations)"
fi

# Test 4: Application layer validation (Phase 3)
print_section "PHASE 4: APPLICATION LAYER VALIDATION"
print_status "INFO" "🎯 Validating application layer structure..."

# Check if all port interfaces exist
port_files=(
    "src/main/java/com/vclipper/processing/application/ports/VideoRepositoryPort.java"
    "src/main/java/com/vclipper/processing/application/ports/FileStoragePort.java"
    "src/main/java/com/vclipper/processing/application/ports/MessageQueuePort.java"
    "src/main/java/com/vclipper/processing/application/ports/NotificationPort.java"
    "src/main/java/com/vclipper/processing/application/ports/UserServicePort.java"
)

missing_ports=0
for file in "${port_files[@]}"; do
    if [[ -f "$file" ]]; then
        print_status "SUCCESS" "Found port: $(basename "$file")"
    else
        print_status "ERROR" "Missing port: $file"
        missing_ports=$((missing_ports + 1))
    fi
done

if [[ $missing_ports -eq 0 ]]; then
    print_status "SUCCESS" "All port interfaces present"
else
    print_status "ERROR" "$missing_ports port interfaces missing"
    exit 1
fi

# Check if all use cases exist
usecase_files=(
    "src/main/java/com/vclipper/processing/application/usecases/SubmitVideoProcessingUseCase.java"
    "src/main/java/com/vclipper/processing/application/usecases/GetProcessingStatusUseCase.java"
    "src/main/java/com/vclipper/processing/application/usecases/ListUserVideosUseCase.java"
    "src/main/java/com/vclipper/processing/application/usecases/GetVideoDownloadUrlUseCase.java"
    "src/main/java/com/vclipper/processing/application/usecases/UpdateProcessingStatusUseCase.java"
)

missing_usecases=0
for file in "${usecase_files[@]}"; do
    if [[ -f "$file" ]]; then
        print_status "SUCCESS" "Found use case: $(basename "$file")"
    else
        print_status "ERROR" "Missing use case: $file"
        missing_usecases=$((missing_usecases + 1))
    fi
done

if [[ $missing_usecases -eq 0 ]]; then
    print_status "SUCCESS" "All use cases present"
else
    print_status "ERROR" "$missing_usecases use cases missing"
    exit 1
fi

# Test 5: Configuration validation
print_section "PHASE 5: CONFIGURATION VALIDATION"
print_status "INFO" "⚙️ Validating configuration setup..."

# Check configuration files
config_files=(
    "src/main/java/com/vclipper/processing/infrastructure/config/ProcessingProperties.java"
    "src/main/java/com/vclipper/processing/infrastructure/config/UseCaseConfiguration.java"
)

missing_config=0
for file in "${config_files[@]}"; do
    if [[ -f "$file" ]]; then
        print_status "SUCCESS" "Found config: $(basename "$file")"
    else
        print_status "ERROR" "Missing config: $file"
        missing_config=$((missing_config + 1))
    fi
done

if [[ $missing_config -eq 0 ]]; then
    print_status "SUCCESS" "All configuration files present"
else
    print_status "ERROR" "$missing_config configuration files missing"
    exit 1
fi

# Check for configurable properties in application.yml
if grep -q "vclipper.processing" src/main/resources/application.yml; then
    print_status "SUCCESS" "Configuration properties found in application.yml"
else
    print_status "ERROR" "Configuration properties missing in application.yml"
    exit 1
fi

# Test 6: Business logic validation
print_section "PHASE 6: BUSINESS LOGIC VALIDATION"
print_status "INFO" "💼 Validating business logic implementation..."

# Check for key business methods
if grep -q "canTransitionTo" src/main/java/com/vclipper/processing/domain/entity/ProcessingStatus.java; then
    print_status "SUCCESS" "ProcessingStatus contains transition logic"
else
    print_status "ERROR" "ProcessingStatus missing transition logic"
    exit 1
fi

if grep -q "updateStatus" src/main/java/com/vclipper/processing/domain/entity/VideoProcessingRequest.java; then
    print_status "SUCCESS" "VideoProcessingRequest contains status update logic"
else
    print_status "ERROR" "VideoProcessingRequest missing status update logic"
    exit 1
fi

if grep -q "isSupported" src/main/java/com/vclipper/processing/domain/enums/VideoFormat.java; then
    print_status "SUCCESS" "VideoFormat contains validation logic"
else
    print_status "ERROR" "VideoFormat missing validation logic"
    exit 1
fi

# Check for use case business logic
if grep -q "execute" src/main/java/com/vclipper/processing/application/usecases/SubmitVideoProcessingUseCase.java; then
    print_status "SUCCESS" "SubmitVideoProcessingUseCase contains execute method"
else
    print_status "ERROR" "SubmitVideoProcessingUseCase missing execute method"
    exit 1
fi

# Test 7: Package structure validation
print_section "PHASE 7: PACKAGE STRUCTURE VALIDATION"
print_status "INFO" "📦 Validating package structure..."

expected_packages=(
    "src/main/java/com/vclipper/processing/domain/entity"
    "src/main/java/com/vclipper/processing/domain/enums"
    "src/main/java/com/vclipper/processing/domain/exceptions"
    "src/main/java/com/vclipper/processing/domain/event"
    "src/main/java/com/vclipper/processing/application/ports"
    "src/main/java/com/vclipper/processing/application/usecases"
    "src/main/java/com/vclipper/processing/infrastructure/config"
    "src/main/java/com/vclipper/processing/infrastructure/adapters"
    "src/main/java/com/vclipper/processing/infrastructure/controllers"
)

for package in "${expected_packages[@]}"; do
    if [[ -d "$package" ]]; then
        print_status "SUCCESS" "Package exists: $(basename "$package")"
    else
        print_status "WARNING" "Package not yet created: $(basename "$package")"
    fi
done

# Test 8: Dependency injection validation
print_section "PHASE 8: DEPENDENCY INJECTION VALIDATION"
print_status "INFO" "🔗 Validating dependency injection setup..."

# Check for @Bean annotations in configuration
if grep -q "@Bean" src/main/java/com/vclipper/processing/infrastructure/config/UseCaseConfiguration.java; then
    print_status "SUCCESS" "Spring @Bean configuration found"
else
    print_status "ERROR" "Spring @Bean configuration missing"
    exit 1
fi

# Check for @EnableConfigurationProperties
if grep -q "@EnableConfigurationProperties" src/main/java/com/vclipper/processing/infrastructure/config/UseCaseConfiguration.java; then
    print_status "SUCCESS" "Configuration properties enabled"
else
    print_status "ERROR" "Configuration properties not enabled"
    exit 1
fi

# Final summary
print_section "SUMMARY"
print_status "SUCCESS" "🎉 Quick tests completed successfully!"
print_status "INFO" "📊 Phase 1 (Setup): ✅ Complete"
print_status "INFO" "📊 Phase 2 (Domain): ✅ Complete"
print_status "INFO" "📊 Phase 3 (Application): ✅ Complete"
print_status "INFO" "📊 Clean architecture: ✅ Compliant"
print_status "INFO" "📊 Business logic: ✅ Implemented"
print_status "INFO" "📊 Configuration: ✅ Externalized"
print_status "INFO" "📊 Build status: ✅ Passing"

echo ""
print_status "INFO" "🚀 Ready for Phase 4: Infrastructure Layer development!"
echo ""
