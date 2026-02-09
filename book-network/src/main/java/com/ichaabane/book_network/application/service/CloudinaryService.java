package com.ichaabane.book_network.application.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Upload file to Cloudinary
     *
     * @param file The file to upload
     * @param folder The folder name in Cloudinary (e.g., "book-covers")
     * @return The secure URL of the uploaded file
     */
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // Generate unique public ID
            String publicId = generatePublicId(file.getOriginalFilename());

            // Upload to Cloudinary
            Map uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", folder,
                            "public_id", publicId,
                            "resource_type", "auto",
                            "transformation", new com.cloudinary.Transformation()
                                    .width(500)
                                    .height(700)
                                    .crop("limit")
                                    .quality("auto:good")
                                    .fetchFormat("auto")
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            log.info("File uploaded successfully to Cloudinary: {}", secureUrl);
            return secureUrl;

        } catch (IOException e) {
            log.error("Failed to upload file to Cloudinary: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Upload file for a specific user
     *
     * @param file The file to upload
     * @param userId The user ID
     * @return The secure URL of the uploaded file
     */
    public String uploadUserFile(
            MultipartFile file,
            Integer userId) {
        String folder = "book-network/users/" + userId;
        return uploadFile(file, folder);
    }

    /**
     * Delete file from Cloudinary
     *
     * @param imageUrl The full URL of the image to delete
     */
    public void deleteFile(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return;
        }

        try {
            // Extract public ID from URL
            String publicId = extractPublicId(imageUrl);
            if (publicId != null) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                log.info("File deleted from Cloudinary: {}", publicId);
            }
        } catch (IOException e) {
            log.error("Failed to delete file from Cloudinary: {}", e.getMessage());
        }
    }

    /**
     * Generate unique public ID for file
     */
    private String generatePublicId(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + "-" + System.currentTimeMillis() + extension;
    }

    /**
     * Extract public ID from Cloudinary URL
     * Example URL: https://res.cloudinary.com/demo/image/upload/v1234567890/folder/file.jpg
     * Public ID: folder/file
     */
    private String extractPublicId(String imageUrl) {
        try {
            // Split by "upload/"
            String[] parts = imageUrl.split("/upload/");
            if (parts.length < 2) {
                return null;
            }

            // Get the part after "upload/"
            String afterUpload = parts[1];

            // Remove version (v1234567890)
            if (afterUpload.startsWith("v")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            // Remove file extension
            int lastDot = afterUpload.lastIndexOf(".");
            if (lastDot > 0) {
                afterUpload = afterUpload.substring(0, lastDot);
            }

            return afterUpload;
        } catch (Exception e) {
            log.error("Failed to extract public ID from URL: {}", imageUrl);
            return null;
        }
    }
}
