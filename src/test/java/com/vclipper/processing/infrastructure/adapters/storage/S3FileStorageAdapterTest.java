package com.vclipper.processing.infrastructure.adapters.storage;

import com.vclipper.processing.application.ports.FileStoragePort.FileMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class S3FileStorageAdapterTest {

    private static final String BUCKET_NAME = "test-bucket";
    private static final String TEST_FILENAME = "test-video.mp4";
    private static final String TEST_CONTENT_TYPE = "video/mp4";
    private static final long TEST_FILE_SIZE = 1024L;
    private static final String TEST_STORAGE_KEY = "videos/2023/01/01/12345678-test-video.mp4";
    private static final String TEST_ETAG = "\"test-etag\"";
    private static final String TEST_PRESIGNED_URL = "https://test-bucket.s3.amazonaws.com/test-key?signature=abc";
    private static final String TEST_VIDEO_ID = "video123";

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private S3FileStorageAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new S3FileStorageAdapter(s3Client, s3Presigner, BUCKET_NAME);
    }

    @Test
    void store_shouldUploadFileToS3AndReturnStorageKey() {
        // Arrange
        InputStream inputStream = new ByteArrayInputStream("test content".getBytes());
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().eTag(TEST_ETAG).build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(putObjectResponse);

        // Act
        String result = adapter.store(inputStream, TEST_FILENAME, TEST_CONTENT_TYPE, TEST_FILE_SIZE);

        // Assert
        assertNotNull(result);
        assertTrue(result.contains(TEST_FILENAME));
        assertTrue(result.startsWith("videos/"));

        // Verify
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, capturedRequest.bucket());
        assertEquals(TEST_CONTENT_TYPE, capturedRequest.contentType());
        assertEquals(TEST_FILE_SIZE, capturedRequest.contentLength());
    }

    @Test
    void generateDownloadUrl_shouldCreatePresignedUrl() {
        // Arrange
        when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).thenReturn(presignedGetObjectRequest);
        when(presignedGetObjectRequest.url()).thenReturn(URI.create(TEST_PRESIGNED_URL));
        int expirationMinutes = 30;

        // Act
        String result = adapter.generateDownloadUrl(TEST_STORAGE_KEY, expirationMinutes);

        // Assert
        assertEquals(TEST_PRESIGNED_URL, result);

        // Verify
        ArgumentCaptor<GetObjectPresignRequest> presignRequestCaptor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(presignRequestCaptor.capture());

        GetObjectPresignRequest capturedRequest = presignRequestCaptor.getValue();
        GetObjectRequest getObjectRequest = capturedRequest.getObjectRequest();
        assertEquals(BUCKET_NAME, getObjectRequest.bucket());
        assertEquals(TEST_STORAGE_KEY, getObjectRequest.key());
    }

    @Test
    void exists_shouldReturnTrueWhenFileExists() {
        // Arrange
        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder().build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);

        // Act
        boolean result = adapter.exists(TEST_STORAGE_KEY);

        // Assert
        assertTrue(result);

        // Verify
        ArgumentCaptor<HeadObjectRequest> requestCaptor = ArgumentCaptor.forClass(HeadObjectRequest.class);
        verify(s3Client).headObject(requestCaptor.capture());
        assertEquals(BUCKET_NAME, requestCaptor.getValue().bucket());
        assertEquals(TEST_STORAGE_KEY, requestCaptor.getValue().key());
    }

    @Test
    void exists_shouldReturnFalseWhenFileDoesNotExist() {
        // Arrange
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.class);

        // Act
        boolean result = adapter.exists(TEST_STORAGE_KEY);

        // Assert
        assertFalse(result);
    }

    @Test
    void delete_shouldReturnTrueWhenSuccessful() {
        // Arrange
        DeleteObjectResponse deleteObjectResponse = DeleteObjectResponse.builder().build();
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(deleteObjectResponse);

        // Act
        boolean result = adapter.delete(TEST_STORAGE_KEY);

        // Assert
        assertTrue(result);

        // Verify
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        assertEquals(BUCKET_NAME, requestCaptor.getValue().bucket());
        assertEquals(TEST_STORAGE_KEY, requestCaptor.getValue().key());
    }

    @Test
    void delete_shouldReturnFalseWhenExceptionOccurs() {
        // Arrange
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(S3Exception.builder().build());

        // Act
        boolean result = adapter.delete(TEST_STORAGE_KEY);

        // Assert
        assertFalse(result);
    }

    @Test
    void getMetadata_shouldReturnMetadataWhenFileExists() {
        // Arrange
        Instant lastModified = Instant.now().minus(1, ChronoUnit.DAYS);
        HeadObjectResponse headObjectResponse = HeadObjectResponse.builder()
                .contentLength(TEST_FILE_SIZE)
                .contentType(TEST_CONTENT_TYPE)
                .lastModified(lastModified)
                .build();
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenReturn(headObjectResponse);

        // Act
        FileMetadata result = adapter.getMetadata(TEST_STORAGE_KEY);

        // Assert
        assertNotNull(result);
        assertEquals(TEST_FILE_SIZE, result.sizeBytes());
        assertEquals(TEST_CONTENT_TYPE, result.contentType());
        assertEquals(lastModified.toString(), result.lastModified());
    }

    @Test
    void getMetadata_shouldReturnNullWhenFileDoesNotExist() {
        // Arrange
        when(s3Client.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.class);

        // Act
        FileMetadata result = adapter.getMetadata(TEST_STORAGE_KEY);

        // Assert
        assertNull(result);
    }

    @Test
    void downloadFile_shouldReturnInputStreamWhenSuccessful() {
        // Arrange
        InputStream testStream = new ByteArrayInputStream("test content".getBytes());
        ResponseInputStream<GetObjectResponse> responseStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(testStream)
        );
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseStream);

        // Act
        InputStream result = adapter.downloadFile(TEST_STORAGE_KEY);

        // Assert
        assertNotNull(result);

        // Verify
        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(requestCaptor.capture());
        assertEquals(BUCKET_NAME, requestCaptor.getValue().bucket());
        assertEquals(TEST_STORAGE_KEY, requestCaptor.getValue().key());
    }

    @Test
    void downloadFile_shouldThrowExceptionWhenError() {
        // Arrange
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(S3Exception.builder().build());

        // Act & Assert
        assertThrows(RuntimeException.class, () -> adapter.downloadFile(TEST_STORAGE_KEY));
    }

    @Test
    void storeProcessedFile_shouldStoreFileWithCorrectKey() throws IOException {
        // Arrange
        byte[] testData = "test processed content".getBytes();
        InputStream inputStream = new ByteArrayInputStream(testData);
        PutObjectResponse putObjectResponse = PutObjectResponse.builder().eTag(TEST_ETAG).build();
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(putObjectResponse);

        // Act
        String result = adapter.storeProcessedFile(inputStream, TEST_VIDEO_ID, TEST_FILENAME);

        // Assert
        assertNotNull(result);
        assertEquals("processed-videos/" + TEST_VIDEO_ID + "/test-video_frames.zip", result);

        // Verify
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, capturedRequest.bucket());
        assertEquals("application/zip", capturedRequest.contentType());
        assertEquals(testData.length, capturedRequest.contentLength());
    }

    @Test
    void copyFile_shouldCopyFileSuccessfully() {
        // Arrange
        String sourceKey = "source/key.mp4";
        String targetKey = "target/key.mp4";
        CopyObjectResponse copyObjectResponse = CopyObjectResponse.builder()
                .copyObjectResult(CopyObjectResult.builder().eTag("\"new-etag\"").build())
                .build();
        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenReturn(copyObjectResponse);

        // Act
        adapter.copyFile(sourceKey, targetKey);

        // Verify
        ArgumentCaptor<CopyObjectRequest> requestCaptor = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3Client).copyObject(requestCaptor.capture());
        
        CopyObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(BUCKET_NAME, capturedRequest.sourceBucket());
        assertEquals(sourceKey, capturedRequest.sourceKey());
        assertEquals(BUCKET_NAME, capturedRequest.destinationBucket());
        assertEquals(targetKey, capturedRequest.destinationKey());
    }

    @Test
    void copyFile_shouldThrowExceptionWhenS3Fails() {
        // Arrange
        String sourceKey = "source/key.mp4";
        String targetKey = "target/key.mp4";
        when(s3Client.copyObject(any(CopyObjectRequest.class))).thenThrow(S3Exception.builder().message("Copy failed").build());

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> adapter.copyFile(sourceKey, targetKey));
        assertTrue(exception.getMessage().contains("Failed to copy file in S3"));
    }
    
    // Helper class to create a URI from string (for URL mock)
    private static class URI {
        public static URL create(String uri) {
            try {
                return java.net.URI.create(uri).toURL();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
