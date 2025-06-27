#!/bin/bash

# Create Better Test Video Script for VClipper Processing
# This script creates a more complete MP4 file with proper structure

set -e

echo "🎬 Creating better test MP4 file for VClipper Processing..."

# Create output directory if it doesn't exist
mkdir -p test_videos

OUTPUT_FILE="test_videos/better_test_video.mp4"

echo "📹 Generating more complete MP4 file with proper structure..."

# Create a more complete MP4 file with proper boxes and structure
# This includes more MP4 boxes to ensure proper MIME type detection
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
    
} > "$OUTPUT_FILE"

# Verify the file was created
if [ -f "$OUTPUT_FILE" ]; then
    echo "✅ Better MP4 file created successfully!"
    
    # Display file information
    echo "📊 File Information:"
    echo "   📁 Path: $(pwd)/$OUTPUT_FILE"
    echo "   📏 Size: $(du -h "$OUTPUT_FILE" | cut -f1)"
    
    # Check MIME type using file command
    MIME_TYPE=$(file --mime-type -b "$OUTPUT_FILE")
    echo "   🏷️  MIME Type: $MIME_TYPE"
    
    # Verify it's recognized as MP4
    if [[ "$MIME_TYPE" == "video/mp4" ]]; then
        echo "   ✅ MIME type is correct (video/mp4)"
    else
        echo "   ⚠️  MIME type detected as: $MIME_TYPE"
        echo "   🔍 Let's check the file signature:"
        hexdump -C "$OUTPUT_FILE" | head -3
    fi
    
    # Display hex dump of first few bytes to verify structure
    echo "   🔍 File signature (first 32 bytes):"
    hexdump -C "$OUTPUT_FILE" | head -2
    
    echo ""
    echo "🎯 Better test MP4 file ready for upload testing!"
    echo "   File: $OUTPUT_FILE"
    echo "   This MP4 should pass MIME type validation"
    
else
    echo "❌ Failed to create MP4 file"
    exit 1
fi
