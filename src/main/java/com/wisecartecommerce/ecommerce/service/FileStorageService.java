package com.wisecartecommerce.ecommerce.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface FileStorageService {
    String uploadProductImage(MultipartFile file, Long productId) throws IOException;
    String uploadUserImage(MultipartFile file, Long userId) throws IOException;
    String uploadDocument(MultipartFile file, String prefix) throws IOException;
    String uploadFile(MultipartFile file, String folder) throws IOException;
    void deleteFile(String fileUrl);
    void deleteFileByPath(String filePath);
}