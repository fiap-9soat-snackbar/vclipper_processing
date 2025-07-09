package com.vclipper.processing.infrastructure.adapters.detection;

import com.vclipper.processing.domain.enums.VideoFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MimeTypeDetectionAdapterTest {

    @InjectMocks
    private MimeTypeDetectionAdapter adapter;

    @BeforeEach
    void setUp() {
        // Initialize with real adapter for most tests
        adapter = new MimeTypeDetectionAdapter();
    }

    @Test
    void detectMimeType_MP4_FromMagicBytes() throws IOException {
        // MP4 magic bytes pattern: "....ftyp"
        byte[] mp4Bytes = new byte[32];
        System.arraycopy(new byte[]{0x00, 0x00, 0x00, 0x20, 0x66, 0x74, 0x79, 0x70}, 0, mp4Bytes, 0, 8);

        InputStream inputStream = new ByteArrayInputStream(mp4Bytes);

        String result = adapter.detectMimeType(inputStream, "video.mp4", null);

        assertEquals(VideoFormat.MP4.getMimeType(), result);
    }

    @Test
    void detectMimeType_MP4_FromAlternativeMagicBytes() throws IOException {
        // MP4 alternative pattern: "ftyp" at offset 4
        byte[] mp4Bytes = new byte[32];
        System.arraycopy(new byte[]{0x01, 0x02, 0x03, 0x04, 0x66, 0x74, 0x79, 0x70}, 0, mp4Bytes, 0, 8);

        InputStream inputStream = new ByteArrayInputStream(mp4Bytes);

        String result = adapter.detectMimeType(inputStream, "video.mp4", null);

        assertEquals(VideoFormat.MP4.getMimeType(), result);
    }

    @Test
    void detectMimeType_AVI_FromMagicBytes() throws IOException {
        // AVI magic bytes: "RIFF" + "AVI "
        byte[] aviBytes = new byte[32];
        System.arraycopy(new byte[]{0x52, 0x49, 0x46, 0x46}, 0, aviBytes, 0, 4);
        System.arraycopy(new byte[]{0x41, 0x56, 0x49, 0x20}, 0, aviBytes, 8, 4);

        InputStream inputStream = new ByteArrayInputStream(aviBytes);

        String result = adapter.detectMimeType(inputStream, "video.avi", null);

        assertEquals(VideoFormat.AVI.getMimeType(), result);
    }

    @Test
    void detectMimeType_WMV_FromMagicBytes() throws IOException {
        // WMV magic bytes: ASF header
        byte[] wmvBytes = new byte[32];
        System.arraycopy(new byte[]{0x30, 0x26, (byte)0xB2, 0x75, (byte)0x8E, 0x66, (byte)0xCF, 0x11}, 0, wmvBytes, 0, 8);

        InputStream inputStream = new ByteArrayInputStream(wmvBytes);

        String result = adapter.detectMimeType(inputStream, "video.wmv", null);

        assertEquals(VideoFormat.WMV.getMimeType(), result);
    }

    @Test
    void detectMimeType_FLV_FromMagicBytes() throws IOException {
        // FLV magic bytes: "FLV"
        byte[] flvBytes = new byte[32];
        System.arraycopy(new byte[]{0x46, 0x4C, 0x56}, 0, flvBytes, 0, 3);

        InputStream inputStream = new ByteArrayInputStream(flvBytes);

        String result = adapter.detectMimeType(inputStream, "video.flv", null);

        assertEquals(VideoFormat.FLV.getMimeType(), result);
    }

    @Test
    void detectMimeType_WEBM_FromMagicBytes() throws IOException {
        // WEBM magic bytes: EBML header
        byte[] webmBytes = new byte[32];
        System.arraycopy(new byte[]{0x1A, 0x45, (byte)0xDF, (byte)0xA3}, 0, webmBytes, 0, 4);

        InputStream inputStream = new ByteArrayInputStream(webmBytes);

        String result = adapter.detectMimeType(inputStream, "video.webm", null);

        assertEquals(VideoFormat.WEBM.getMimeType(), result);
    }

    @Test
    void detectMimeType_FromExtension_WhenNoMagicBytes() throws IOException {
        // Empty file - no magic bytes to detect
        byte[] emptyBytes = new byte[32];
        InputStream inputStream = new ByteArrayInputStream(emptyBytes);

        String result = adapter.detectMimeType(inputStream, "video.mp4", null);

        assertEquals(VideoFormat.MP4.getMimeType(), result);
    }

    @Test
    void detectMimeType_FromClientProvided_WhenMatchesExtension() throws IOException {
        // Empty file with client-provided MIME type that matches extension
        byte[] emptyBytes = new byte[32];
        InputStream inputStream = new ByteArrayInputStream(emptyBytes);

        String result = adapter.detectMimeType(inputStream, "video.mp4", "video/mp4");

        assertEquals(VideoFormat.MP4.getMimeType(), result);
    }

    @Test
    void detectMimeType_IgnoresClientProvided_WhenDoesntMatchExtension() throws IOException {
        // Empty file with client-provided MIME type that doesn't match extension
        byte[] emptyBytes = new byte[32];
        InputStream inputStream = new ByteArrayInputStream(emptyBytes);

        String result = adapter.detectMimeType(inputStream, "video.mp4", "video/webm");

        assertEquals(VideoFormat.MP4.getMimeType(), result);
    }

    @Test
    void detectMimeType_HandlesNonMarkableStream() throws IOException {
        // Test with a mock stream that doesn't support mark/reset
        InputStream mockStream = mock(InputStream.class);
        when(mockStream.markSupported()).thenReturn(false);

        String result = adapter.detectMimeType(mockStream, "video.mp4", null);

        assertEquals(VideoFormat.MP4.getMimeType(), result);
        verify(mockStream, never()).read(any(byte[].class));
    }

    @Test
    void detectMimeType_HandlesIOException() throws IOException {
        // Test with a mock stream that throws IOException
        InputStream mockStream = mock(InputStream.class);
        when(mockStream.markSupported()).thenReturn(true);
        doThrow(new IOException("Test exception")).when(mockStream).read(any(byte[].class));

        String result = adapter.detectMimeType(mockStream, "video.mp4", null);

        assertEquals(VideoFormat.MP4.getMimeType(), result);
    }

    @Test
    void detectMimeType_ReturnsNull_ForUnsupportedExtension() {
        byte[] emptyBytes = new byte[32];
        InputStream inputStream = new ByteArrayInputStream(emptyBytes);

        String result = adapter.detectMimeType(inputStream, "document.pdf", null);

        assertNull(result);
    }

    @Test
    void detectMimeType_ReturnsNull_ForNullFilename() {
        byte[] emptyBytes = new byte[32];
        InputStream inputStream = new ByteArrayInputStream(emptyBytes);

        String result = adapter.detectMimeType(inputStream, null, null);

        assertNull(result);
    }

    @Test
    void isSupportedVideoMimeType_ReturnsTrueForSupportedTypes() {
        assertTrue(adapter.isSupportedVideoMimeType("video/mp4"));
        assertTrue(adapter.isSupportedVideoMimeType("video/x-msvideo"));
        assertTrue(adapter.isSupportedVideoMimeType("video/quicktime"));
    }

    @Test
    void isSupportedVideoMimeType_ReturnsFalseForUnsupportedTypes() {
        assertFalse(adapter.isSupportedVideoMimeType("application/pdf"));
        assertFalse(adapter.isSupportedVideoMimeType("text/plain"));
        assertFalse(adapter.isSupportedVideoMimeType(null));
    }

    @ParameterizedTest
    @MethodSource("provideFileExtensionsAndMimeTypes")
    void detectMimeType_CorrectlyMapsExtensionsToMimeTypes(String filename, String expectedMimeType) {
        byte[] emptyBytes = new byte[32];
        InputStream inputStream = new ByteArrayInputStream(emptyBytes);

        String result = adapter.detectMimeType(inputStream, filename, null);

        assertEquals(expectedMimeType, result);
    }

    private static Stream<Arguments> provideFileExtensionsAndMimeTypes() {
        return Stream.of(
            Arguments.of("video.mp4", VideoFormat.MP4.getMimeType()),
            Arguments.of("video.avi", VideoFormat.AVI.getMimeType()),
            Arguments.of("video.mov", VideoFormat.MOV.getMimeType()),
            Arguments.of("video.wmv", VideoFormat.WMV.getMimeType()),
            Arguments.of("video.flv", VideoFormat.FLV.getMimeType()),
            Arguments.of("video.webm", VideoFormat.WEBM.getMimeType()),
            Arguments.of("video.txt", null),
            Arguments.of("video", null)
        );
    }
}
