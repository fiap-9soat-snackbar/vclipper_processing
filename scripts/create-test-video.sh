#!/bin/bash

# Create a minimal MP4 file with proper headers
TEST_VIDEO_FILE="test-video.mp4"

# Create a minimal MP4 file with proper MP4 signature
printf '\x00\x00\x00\x20\x66\x74\x79\x70\x69\x73\x6f\x6d\x00\x00\x02\x00\x69\x73\x6f\x6d\x69\x73\x6f\x32\x61\x76\x63\x31\x6d\x70\x34\x31' > "$TEST_VIDEO_FILE"
printf '\x00\x00\x00\x08\x66\x72\x65\x65' >> "$TEST_VIDEO_FILE"

# Add some test content
echo "VClipper Test Video Content - $(date)" >> "$TEST_VIDEO_FILE"
echo "This is a test video file for integration testing" >> "$TEST_VIDEO_FILE"

echo "Test video file created: $TEST_VIDEO_FILE"
echo "File size: $(wc -c < "$TEST_VIDEO_FILE") bytes"

# Check MIME type
if command -v file >/dev/null 2>&1; then
    echo "MIME type: $(file --mime-type -b "$TEST_VIDEO_FILE")"
fi
