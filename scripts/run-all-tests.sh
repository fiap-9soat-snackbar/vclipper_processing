#!/bin/bash

# VClipper Complete Modular Test Suite
# Final working orchestrator

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
PURPLE='\033[0;35m'
BOLD='\033[1m'
NC='\033[0m'

PASSED=0
FAILED=0
START_TIME=$(date +%s)

echo -e "${BOLD}${PURPLE}================================================${NC}"
echo -e "${BOLD}${BLUE}   VClipper Modular Test Suite${NC}"
echo -e "${BOLD}${PURPLE}================================================${NC}"
echo ""

# Function to run a test
run_test() {
    local test_name="$1"
    local test_script="$2"
    
    echo -e "${YELLOW}$test_name${NC}"
    
    if bash "$test_script" > /dev/null 2>&1; then
        echo -e "${GREEN}✅ PASSED${NC}"
        ((PASSED++))
    else
        echo -e "${RED}❌ FAILED${NC}"
        ((FAILED++))
    fi
    echo ""
}

# Run all tests
run_test "[1/9] Environment Setup" "tests/01-environment-setup.sh"
run_test "[2/9] Health Validation" "tests/02-health-validation.sh"
run_test "[3/9] Video Upload" "tests/03-video-upload.sh"
run_test "[4/9] Video Status" "tests/04-video-status.sh"
run_test "[5/9] Video Listing" "tests/05-video-listing.sh"
run_test "[6/9] Download Workflow" "tests/06-download-workflow.sh"
run_test "[7/9] Processing Simulation" "tests/07-processing-simulation.sh"
run_test "[8/9] Error Handling" "tests/08-error-handling.sh"
run_test "[9/9] AWS Integration" "tests/09-aws-integration.sh"

# Summary
END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo -e "${BOLD}${PURPLE}================================================${NC}"
echo -e "${BOLD}${BLUE}   Results${NC}"
echo -e "${BOLD}${PURPLE}================================================${NC}"
echo -e "Duration: ${DURATION}s"
echo -e "Passed: ${GREEN}$PASSED${NC}/9"
echo -e "Failed: ${RED}$FAILED${NC}/9"
echo ""

if [[ $FAILED -eq 0 ]]; then
    echo -e "${GREEN}${BOLD}🎉 ALL TESTS PASSED!${NC}"
    echo -e "${GREEN}VClipper system is ready!${NC}"
    exit 0
else
    echo -e "${RED}${BOLD}💥 $FAILED TESTS FAILED${NC}"
    exit 1
fi
