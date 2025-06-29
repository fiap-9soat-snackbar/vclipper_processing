#!/bin/bash

# Script to simulate vclipping service completing video processing
# Usage: ./simulate-processing-completion.sh <VIDEO_ID> [USER_ID]

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Default values
DEFAULT_USER_ID="test-user-123"
BASE_URL="http://localhost:8080"

# Parse arguments
VIDEO_ID=$1
USER_ID=${2:-$DEFAULT_USER_ID}

if [ -z "$VIDEO_ID" ]; then
    echo -e "${RED}❌ Error: VIDEO_ID is required${NC}"
    echo "Usage: $0 <VIDEO_ID> [USER_ID]"
    echo "Example: $0 abc123-def456-ghi789 test-user-123"
    exit 1
fi

echo -e "${BLUE}🎬 Simulating vclipping service completion for video: ${VIDEO_ID}${NC}"
echo -e "${BLUE}👤 User ID: ${USER_ID}${NC}"
echo ""

# Generate processed file S3 key (simulating what vclipping would create)
PROCESSED_S3_KEY="processed/${USER_ID}/${VIDEO_ID}/clipped-frames.zip"
COMPLETION_TIME=$(date -u +%Y-%m-%dT%H:%M:%SZ)

echo -e "${YELLOW}📁 Simulated processed file S3 key: ${PROCESSED_S3_KEY}${NC}"
echo -e "${YELLOW}⏰ Processing completion time: ${COMPLETION_TIME}${NC}"
echo ""

# Step 1: Mark video as PROCESSING
echo -e "${BLUE}📤 Step 1: Marking video as PROCESSING...${NC}"

PROCESSING_PAYLOAD=$(cat <<EOF
{
  "status": {
    "value": "PROCESSING",
    "description": "Video is currently being processed",
    "isTerminal": false
  },
  "processedFileS3Key": null,
  "processingCompletedAt": null,
  "errorMessage": null
}
EOF
)

echo -e "${YELLOW}Request payload:${NC}"
echo "$PROCESSING_PAYLOAD" | jq .
echo ""

PROCESSING_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X PUT "${BASE_URL}/api/videos/${VIDEO_ID}/status" \
  -H "Content-Type: application/json" \
  -d "$PROCESSING_PAYLOAD")

PROCESSING_HTTP_STATUS=$(echo "$PROCESSING_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
PROCESSING_RESPONSE_BODY=$(echo "$PROCESSING_RESPONSE" | sed '/HTTP_STATUS:/d')

echo -e "${BLUE}📥 Processing Response (HTTP ${PROCESSING_HTTP_STATUS}):${NC}"

if [ "$PROCESSING_HTTP_STATUS" != "200" ]; then
    echo -e "${RED}❌ Failed to mark video as PROCESSING${NC}"
    echo "$PROCESSING_RESPONSE_BODY" | jq . 2>/dev/null || echo "$PROCESSING_RESPONSE_BODY"
    exit 1
fi

echo -e "${GREEN}✅ Video marked as PROCESSING${NC}"
echo "$PROCESSING_RESPONSE_BODY" | jq .
echo ""

# Step 2: Mark video as COMPLETED
echo -e "${BLUE}📤 Step 2: Marking video as COMPLETED...${NC}"

REQUEST_PAYLOAD=$(cat <<EOF
{
  "status": {
    "value": "COMPLETED",
    "description": "Video processing completed successfully",
    "isTerminal": true
  },
  "processedFileS3Key": "${PROCESSED_S3_KEY}",
  "processingCompletedAt": "${COMPLETION_TIME}",
  "errorMessage": null
}
EOF
)

echo -e "${YELLOW}Request payload:${NC}"
echo "$REQUEST_PAYLOAD" | jq .
echo ""

# Send the status update request
echo -e "${BLUE}🔄 Calling PUT /api/videos/${VIDEO_ID}/status...${NC}"

RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X PUT "${BASE_URL}/api/videos/${VIDEO_ID}/status" \
  -H "Content-Type: application/json" \
  -d "$REQUEST_PAYLOAD")

# Extract HTTP status and response body
HTTP_STATUS=$(echo "$RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
RESPONSE_BODY=$(echo "$RESPONSE" | sed '/HTTP_STATUS:/d')

echo -e "${BLUE}📥 Response (HTTP ${HTTP_STATUS}):${NC}"

if [ "$HTTP_STATUS" = "200" ]; then
    echo -e "${GREEN}✅ Status update successful!${NC}"
    echo "$RESPONSE_BODY" | jq .
    
    echo ""
    echo -e "${BLUE}🔍 Now testing download URL generation...${NC}"
    
    # Test download URL generation
    DOWNLOAD_RESPONSE=$(curl -s -w "\nHTTP_STATUS:%{http_code}" \
      "${BASE_URL}/api/videos/${VIDEO_ID}/download?userId=${USER_ID}")
    
    DOWNLOAD_HTTP_STATUS=$(echo "$DOWNLOAD_RESPONSE" | grep "HTTP_STATUS:" | cut -d: -f2)
    DOWNLOAD_RESPONSE_BODY=$(echo "$DOWNLOAD_RESPONSE" | sed '/HTTP_STATUS:/d')
    
    echo -e "${BLUE}📥 Download URL Response (HTTP ${DOWNLOAD_HTTP_STATUS}):${NC}"
    
    if [ "$DOWNLOAD_HTTP_STATUS" = "200" ]; then
        echo -e "${GREEN}✅ Download URL generated successfully!${NC}"
        echo "$DOWNLOAD_RESPONSE_BODY" | jq .
        
        # Extract and display the download URL
        DOWNLOAD_URL=$(echo "$DOWNLOAD_RESPONSE_BODY" | jq -r '.downloadUrl // empty')
        if [ -n "$DOWNLOAD_URL" ] && [ "$DOWNLOAD_URL" != "null" ]; then
            echo ""
            echo -e "${GREEN}🎉 SUCCESS: Complete workflow working!${NC}"
            echo -e "${GREEN}📥 Download URL: ${DOWNLOAD_URL}${NC}"
            echo -e "${YELLOW}⏰ URL expires in: $(echo "$DOWNLOAD_RESPONSE_BODY" | jq -r '.expirationMinutes // "unknown"') minutes${NC}"
        else
            echo -e "${YELLOW}⚠️  Warning: Download URL is null or empty${NC}"
        fi
    else
        echo -e "${RED}❌ Download URL generation failed${NC}"
        echo "$DOWNLOAD_RESPONSE_BODY" | jq .
    fi
    
else
    echo -e "${RED}❌ Status update failed${NC}"
    echo "$RESPONSE_BODY" | jq . 2>/dev/null || echo "$RESPONSE_BODY"
fi

echo ""
echo -e "${BLUE}📊 Summary:${NC}"
echo -e "  Video ID: ${VIDEO_ID}"
echo -e "  User ID: ${USER_ID}"
echo -e "  Status Update: HTTP ${HTTP_STATUS}"
if [ -n "${DOWNLOAD_HTTP_STATUS}" ]; then
    echo -e "  Download URL: HTTP ${DOWNLOAD_HTTP_STATUS}"
fi
echo ""
