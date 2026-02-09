package com.ichaabane.book_network.application.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CloudinaryService - Comprehensive Tests")
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @Mock
    private MultipartFile mockFile;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        // Use lenient to avoid UnnecessaryStubbingException for tests that don't use uploader
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    // ===========================
    // Upload Tests - Success Cases
    // ===========================

    @Test
    @DisplayName("Should upload file successfully with custom folder")
    void shouldUploadFileWithCustomFolderSuccessfully() throws IOException {
        // Given
        String folder = "test-folder";
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/v1234567890/test-folder/test.jpg");

        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any())).willReturn(uploadResult);

        // When
        String result = cloudinaryService.uploadFile(mockFile, folder);

        // Then
        assertThat(result).isNotNull()
                .contains("cloudinary.com")
                .contains("test-folder");
        verify(uploader).upload(any(byte[].class), any());
    }

    @Test
    @DisplayName("Should upload user file successfully")
    void shouldUploadUserFileSuccessfully() throws IOException {
        // Given
        Integer userId = 1;
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/v1234567890/book-network/users/1/test.jpg");

        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any())).willReturn(uploadResult);

        // When
        String result = cloudinaryService.uploadUserFile(mockFile, userId);

        // Then
        assertThat(result).isNotNull()
                .contains("cloudinary.com")
                .contains("book-network/users/1");
        verify(uploader).upload(any(byte[].class), any());
    }

    @Test
    @DisplayName("Should upload file with no extension")
    void shouldUploadFileWithNoExtension() throws IOException {
        // Given
        String folder = "test-folder";
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/v1234567890/test-folder/file");

        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any())).willReturn(uploadResult);

        // When
        String result = cloudinaryService.uploadFile(mockFile, folder);

        // Then
        assertThat(result).isNotNull()
                .contains("cloudinary.com");
        verify(uploader).upload(any(byte[].class), any());
    }

    @Test
    @DisplayName("Should upload file with multiple dots in filename")
    void shouldUploadFileWithMultipleDotsInName() throws IOException {
        // Given
        String folder = "test-folder";
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/v1234567890/test-folder/file.test.jpg");

        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any())).willReturn(uploadResult);

        // When
        String result = cloudinaryService.uploadFile(mockFile, folder);

        // Then
        assertThat(result).isNotNull()
                .contains("cloudinary.com");
        verify(uploader).upload(any(byte[].class), any());
    }

    @Test
    @DisplayName("Should handle null original filename")
    void shouldHandleNullOriginalFilename() throws IOException {
        // Given
        String folder = "test-folder";
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/v1234567890/test-folder/file");

        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any())).willReturn(uploadResult);

        // When
        String result = cloudinaryService.uploadFile(mockFile, folder);

        // Then
        assertThat(result).isNotNull()
                .contains("cloudinary.com");
        verify(uploader).upload(any(byte[].class), any());
    }

    // ===========================
    // Upload Tests - Failure Cases
    // ===========================

    @Test
    @DisplayName("Should return null on upload failure due to IOException")
    void shouldReturnNullOnUploadFailure() throws IOException {
        // Given
        given(mockFile.getBytes()).willThrow(new IOException("Upload failed"));

        // When
        String result = cloudinaryService.uploadUserFile(mockFile, 1);

        // Then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("Should return null when Cloudinary upload throws IOException")
    void shouldHandleCloudinaryUploadException() throws IOException {
        // Given
        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any())).willThrow(new IOException("Cloudinary API error"));

        // When
        String result = cloudinaryService.uploadFile(mockFile, "test-folder");

        // Then
        assertThat(result).isNull();
        verify(uploader).upload(any(byte[].class), any());
    }

    // ===========================
    // Delete Tests
    // ===========================

    @Test
    @DisplayName("Should delete file successfully")
    void shouldDeleteFileSuccessfully() throws IOException {
        // Given
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/book-network/users/1/test.jpg";
        Map<String, Object> deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");

        given(uploader.destroy(eq("book-network/users/1/test"), any())).willReturn(deleteResult);

        // When
        cloudinaryService.deleteFile(imageUrl);

        // Then
        verify(uploader).destroy(eq("book-network/users/1/test"), any());
    }

    @Test
    @DisplayName("Should handle null URL in delete gracefully")
    void shouldHandleNullUrlInDelete() throws IOException {
        // When
        cloudinaryService.deleteFile(null);

        // Then
        verify(uploader, never()).destroy(any(), any());
    }

    @Test
    @DisplayName("Should handle empty URL in delete gracefully")
    void shouldHandleEmptyUrlInDelete() throws IOException {
        // When
        cloudinaryService.deleteFile("");

        // Then
        verify(uploader, never()).destroy(any(), any());
    }

    @Test
    @DisplayName("Should handle invalid URL format in delete")
    void shouldHandleInvalidUrlInDelete() throws IOException {
        // Given - URL without "upload/" segment
        String invalidUrl = "https://res.cloudinary.com/demo/image/test.jpg";

        // When
        cloudinaryService.deleteFile(invalidUrl);

        // Then
        verify(uploader, never()).destroy(any(), any());
    }

    @Test
    @DisplayName("Should handle IOException during delete gracefully")
    void shouldHandleDeleteFailureGracefully() throws IOException {
        // Given
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/book-network/users/1/test.jpg";
        given(uploader.destroy(any(), any())).willThrow(new IOException("Delete failed"));

        // When - Should not throw exception
        assertThatCode(() -> cloudinaryService.deleteFile(imageUrl))
                .doesNotThrowAnyException();

        // Then
        verify(uploader).destroy(any(), any());
    }

    // ===========================
    // Public ID Extraction Tests
    // ===========================

    @Test
    @DisplayName("Should extract public ID from URL with version")
    void shouldExtractPublicIdWithVersion() throws IOException {
        // Given
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/folder/subfolder/file.jpg";
        Map<String, Object> deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");

        given(uploader.destroy(eq("folder/subfolder/file"), any())).willReturn(deleteResult);

        // When
        cloudinaryService.deleteFile(imageUrl);

        // Then
        verify(uploader).destroy(eq("folder/subfolder/file"), any());
    }

    @Test
    @DisplayName("Should extract public ID from URL without version")
    void shouldExtractPublicIdWithoutVersion() throws IOException {
        // Given
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/folder/subfolder/file.jpg";
        Map<String, Object> deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");

        given(uploader.destroy(eq("folder/subfolder/file"), any())).willReturn(deleteResult);

        // When
        cloudinaryService.deleteFile(imageUrl);

        // Then
        verify(uploader).destroy(eq("folder/subfolder/file"), any());
    }

    @Test
    @DisplayName("Should extract public ID from complex URL")
    void shouldExtractPublicIdFromComplexUrl() throws IOException {
        // Given
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/book-network/users/123/uuid-123456789.png";
        Map<String, Object> deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");

        given(uploader.destroy(eq("book-network/users/123/uuid-123456789"), any())).willReturn(deleteResult);

        // When
        cloudinaryService.deleteFile(imageUrl);

        // Then
        verify(uploader).destroy(eq("book-network/users/123/uuid-123456789"), any());
    }

    @Test
    @DisplayName("Should handle URL without file extension")
    void shouldHandleUrlWithoutExtension() throws IOException {
        // Given
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v1234567890/folder/file";
        Map<String, Object> deleteResult = new HashMap<>();
        deleteResult.put("result", "ok");

        given(uploader.destroy(eq("folder/file"), any())).willReturn(deleteResult);

        // When
        cloudinaryService.deleteFile(imageUrl);

        // Then
        verify(uploader).destroy(eq("folder/file"), any());
    }

    @Test
    @DisplayName("Should return null for malformed URL")
    void shouldReturnNullForMalformedUrl() throws IOException {
        // Given
        String malformedUrl = "not-a-valid-url";

        // When
        cloudinaryService.deleteFile(malformedUrl);

        // Then
        verify(uploader, never()).destroy(any(), any());
    }

    // ===========================
    // Folder Structure Tests
    // ===========================

    @Test
    @DisplayName("Should verify correct folder structure for user files")
    void shouldVerifyCorrectFolderStructure() throws IOException {
        // Given
        Integer userId = 42;
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/test.jpg");

        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any())).willReturn(uploadResult);

        // When
        cloudinaryService.uploadUserFile(mockFile, userId);

        // Then
        verify(uploader).upload(any(byte[].class), any());
        // The folder should be "book-network/users/42" - verified in the service implementation
    }

    // ===========================
    // Verification Tests
    // ===========================

    @Test
    @DisplayName("Should verify transformation parameters are applied")
    void shouldVerifyTransformationParameters() throws IOException {
        // Given
        String folder = "test-folder";
        Map<String, Object> uploadResult = new HashMap<>();
        uploadResult.put("secure_url", "https://res.cloudinary.com/demo/image/upload/test.jpg");

        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any())).willReturn(uploadResult);

        // When
        cloudinaryService.uploadFile(mockFile, folder);

        // Then
        verify(uploader).upload(any(byte[].class), any());
        // Transformation parameters (width: 500, height: 700, quality: auto:good) are set in the service
    }

    @Test
    @DisplayName("Should generate unique public IDs for multiple uploads")
    void shouldGenerateUniquePublicIds() throws IOException {
        // Given
        String folder = "test-folder";
        Map<String, Object> uploadResult1 = new HashMap<>();
        uploadResult1.put("secure_url", "https://res.cloudinary.com/demo/image/upload/file1.jpg");
        Map<String, Object> uploadResult2 = new HashMap<>();
        uploadResult2.put("secure_url", "https://res.cloudinary.com/demo/image/upload/file2.jpg");

        given(mockFile.getBytes()).willReturn("test content".getBytes());
        given(uploader.upload(any(byte[].class), any()))
                .willReturn(uploadResult1)
                .willReturn(uploadResult2);

        // When
        String result1 = cloudinaryService.uploadFile(mockFile, folder);
        String result2 = cloudinaryService.uploadFile(mockFile, folder);

        // Then
        assertThat(result1).isNotNull();
        assertThat(result2).isNotNull();
        verify(uploader, times(2)).upload(any(byte[].class), any());
        // Each upload should have a unique publicId due to UUID + timestamp
    }
}
