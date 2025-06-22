#!/bin/bash

# Quick test for video upload with proper MIME type
cd /home/saulo/workspace/fiap-alura/fase05/vclipper_processing

# Start services
echo "Starting services..."
docker compose up -d --build

# Wait for services
echo "Waiting for services to start..."
sleep 30

# Create test video file with proper MP4 headers
TEST_VIDEO_FILE="test-video.mp4"
printf '\x00\x00\x00\x20\x66\x74\x79\x70\x69\x73\x6f\x6d\x00\x00\x02\x00\x69\x73\x6f\x6d\x69\x73\x6f\x32\x61\x76\x63\x31\x6d\x70\x34\x31' > "$TEST_VIDEO_FILE"
printf '\x00\x00\x00\x08\x66\x72\x65\x65' >> "$TEST_VIDEO_FILE"
echo "VClipper Test Video Content - $(date)" >> "$TEST_VIDEO_FILE"

echo "Test video file created: $TEST_VIDEO_FILE"
echo "File size: $(wc -c < "$TEST_VIDEO_FILE") bytes"

# Test upload with explicit MIME type
echo "Testing video upload..."
UPLOAD_RESPONSE=$(curl -s -X POST \
    -F "userId=test-user-123" \
    -F "file=@$TEST_VIDEO_FILE;type=video/mp4" \
    -w "%{http_code}" \
    "http://localhost:8080/api/videos/upload")

HTTP_CODE="${UPLOAD_RESPONSE: -3}"
RESPONSE_BODY="${UPLOAD_RESPONSE%???}"

echo "Upload HTTP Status: $HTTP_CODE"
echo "Upload Response: $RESPONSE_BODY"

if [[ "$HTTP_CODE" == "201" ]]; then
    echo "✅ Video upload successful!"
else
    echo "❌ Video upload failed"
    echo "Checking logs..."
    docker compose logs processing-service | tail -10
fi

# Cleanup
docker compose down -v
rm -f "$TEST_VIDEO_FILE"
