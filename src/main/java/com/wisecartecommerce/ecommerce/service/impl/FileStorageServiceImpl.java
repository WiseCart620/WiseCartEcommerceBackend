package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl implements FileStorageService {
    
    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;
    
    @Override
    public String uploadProductImage(MultipartFile file, Long productId) throws IOException {
        validateImageFile(file);
        return saveFile(file, "products", productId != null ? "product_" + productId : "product");
    }
    
    @Override
    public String uploadUserImage(MultipartFile file, Long userId) throws IOException {
        validateImageFile(file);
        return saveFile(file, "users", userId != null ? "user_" + userId : "user");
    }
    
    @Override
    public String uploadDocument(MultipartFile file, String prefix) throws IOException {
        validateDocumentFile(file);
        return saveFile(file, "documents", prefix);
    }
    
    @Override
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        validateImageFile(file);
        return saveFile(file, folder, folder);
    }
    
    @Override
    public void deleteFile(String fileUrl) {
        try {
            String path = extractFilePathFromUrl(fileUrl);
            deleteFileByPath(path);
        } catch (Exception e) {
            log.error("Error deleting file from URL: {}", fileUrl, e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }
    
    @Override
    public void deleteFileByPath(String filePath) {
        try {
            Path path = Paths.get(uploadDir, filePath).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                Files.delete(path);
                log.info("File deleted: {}", path);
            } else {
                log.warn("File not found for deletion: {}", path);
            }
        } catch (IOException e) {
            log.error("Error deleting file: {}", filePath, e);
            throw new RuntimeException("Failed to delete file: " + e.getMessage());
        }
    }
    
    private String saveFile(MultipartFile file, String directory, String prefix) throws IOException {
        Path uploadPath = Paths.get(uploadDir, directory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }
        
        String originalFilename = file.getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String newFilename = generateFilename(prefix, fileExtension);
        Path filePath = uploadPath.resolve(newFilename);
        
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        
        String relativePath = directory + "/" + newFilename;
        log.info("File saved: {}", relativePath);
        
        return "/api/files/serve?path=" + relativePath;
    }
    
    private String generateFilename(String prefix, String extension) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String random = UUID.randomUUID().toString().substring(0, 8);
        
        if (prefix != null && !prefix.trim().isEmpty()) {
            return prefix.trim() + "_" + timestamp + "_" + random + extension;
        }
        return timestamp + "_" + random + extension;
    }
    
    private String extractFilePathFromUrl(String url) {
        if (url.contains("path=")) {
            String path = url.substring(url.indexOf("path=") + 5);
            return java.net.URLDecoder.decode(path, java.nio.charset.StandardCharsets.UTF_8);
        }
        return url;
    }
    
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 5MB");
        }
    }
    
    private void validateDocumentFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("Invalid file type");
        }
        
        boolean isAllowed = contentType.startsWith("application/pdf") ||
                           contentType.contains("msword") ||
                           contentType.contains("excel") ||
                           contentType.contains("spreadsheetml") ||
                           contentType.contains("wordprocessingml") ||
                           contentType.equals("text/plain") ||
                           contentType.startsWith("image/");
        
        if (!isAllowed) {
            throw new IllegalArgumentException("File type not allowed");
        }
        
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new IllegalArgumentException("File size must be less than 10MB");
        }
    }
}