package com.wisecartecommerce.ecommerce.controller.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Slf4j
public class FileUploadController {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/upload/product")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadProductImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "productId", required = false) Long productId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Please select a file to upload"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Only image files are allowed"));
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File size must be less than 5MB"));
            }

            Map<String, String> fileInfo = saveFile(file, "products",
                    productId != null ? "product_" + productId : null);

            return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", fileInfo));

        } catch (IOException e) {
            log.error("Error uploading product image", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload image: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/category")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadCategoryImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "categoryId", required = false) Long categoryId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Please select a file to upload"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Only image files are allowed"));
            }

            if (file.getSize() > 5 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File size must be less than 5MB"));
            }

            Map<String, String> fileInfo = saveFile(file, "categories",
                    categoryId != null ? "category_" + categoryId : "category");

            return ResponseEntity.ok(ApiResponse.success("Category image uploaded successfully", fileInfo));

        } catch (IOException e) {
            log.error("Error uploading category image", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload image: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/user")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadUserImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Long userId) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Please select a file to upload"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Only image files are allowed"));
            }

            if (file.getSize() > 2 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File size must be less than 2MB"));
            }

            Map<String, String> fileInfo = saveFile(file, "users",
                    userId != null ? "user_" + userId : null);

            return ResponseEntity.ok(ApiResponse.success("Profile image uploaded successfully", fileInfo));

        } catch (IOException e) {
            log.error("Error uploading user image", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload image: " + e.getMessage()));
        }
    }

    @PostMapping("/upload/document")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, String>>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prefix", required = false) String prefix) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Please select a file to upload"));
            }

            String contentType = file.getContentType();
            List<String> allowedTypes = Arrays.asList(
                    "application/pdf",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-excel",
                    "text/plain",
                    "image/jpeg",
                    "image/png");

            if (contentType == null || !allowedTypes.contains(contentType)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Only PDF, Word, Excel, Text, and Image files are allowed"));
            }

            if (file.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File size must be less than 10MB"));
            }

            Map<String, String> fileInfo = saveFile(file, "documents", prefix);

            return ResponseEntity.ok(ApiResponse.success("Document uploaded successfully", fileInfo));

        } catch (IOException e) {
            log.error("Error uploading document", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to upload document: " + e.getMessage()));
        }
    }

    @DeleteMapping("/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFile(@RequestParam String fileUrl) {
        try {
            String[] parts = fileUrl.split("/");
            if (parts.length < 3) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Invalid file URL"));
            }

            String directory = parts[parts.length - 2];
            String filename = parts[parts.length - 1];

            Path filePath = Paths.get(uploadDir, directory, filename);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted: {}", filePath);
                return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("File not found"));
            }

        } catch (IOException e) {
            log.error("Error deleting file", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Failed to delete file: " + e.getMessage()));
        }
    }

    private Map<String, String> saveFile(MultipartFile file, String directory, String prefix) throws IOException {
        Path uploadPath = Paths.get(uploadDir, directory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Created directory: {}", uploadPath);
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        String newFilename;
        if (prefix != null && !prefix.trim().isEmpty()) {
            newFilename = prefix.trim() + "_" + System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        } else {
            newFilename = System.currentTimeMillis() + "_" +
                    UUID.randomUUID().toString().substring(0, 8) + fileExtension;
        }

        Path filePath = uploadPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        String fileUrl = "/api/files/serve?path=" + directory + "/" + newFilename;

        Map<String, String> fileInfo = new HashMap<>();
        fileInfo.put("url", fileUrl);
        fileInfo.put("filename", newFilename);
        fileInfo.put("originalName", originalFilename);
        fileInfo.put("type", file.getContentType());
        fileInfo.put("size", String.valueOf(file.getSize()));
        fileInfo.put("path", directory + "/" + newFilename);

        log.info("File saved: {}", filePath);
        return fileInfo;
    }
}