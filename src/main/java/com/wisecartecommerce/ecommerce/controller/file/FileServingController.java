package com.wisecartecommerce.ecommerce.controller.file;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/files")
@CrossOrigin(origins = "*")
@Slf4j
public class FileServingController {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    @GetMapping("/serve")
    public ResponseEntity<Resource> serveFile(@RequestParam String path) {
        log.info("Serving file request for path: {}", path);
        return handleFileRequest(path, false);
    }

    @GetMapping("/{directory}/{filename:.+}")
    public ResponseEntity<Resource> serveFileByPath(
            @PathVariable String directory,
            @PathVariable String filename) {
        try {
            log.info("Serving file: {}/{}", directory, filename);
            String relativePath = directory + "/" + filename;
            return handleFileRequest(relativePath, false);
        } catch (Exception e) {
            log.error("Error serving file: {}/{}", directory, filename, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> downloadFile(@RequestParam String path) {
        log.info("Download request for path: {}", path);
        return handleFileRequest(path, true);
    }

    private ResponseEntity<Resource> handleFileRequest(String path, boolean download) {
        try {
            String decodedPath = java.net.URLDecoder.decode(path, "UTF-8");
            log.info("Decoded path: {}", decodedPath);

            String cleanPath = decodedPath.trim();

            if (cleanPath.startsWith("/")) {
                cleanPath = cleanPath.substring(1);
            }

            if (cleanPath.startsWith("uploads/")) {
                cleanPath = cleanPath.substring("uploads/".length());
            }

            cleanPath = cleanPath.replace("\\", "/");
            cleanPath = cleanPath.replaceAll("/+", "/");
            cleanPath = cleanPath.replaceAll("\\.\\./", "");

            log.info("Cleaned path: {}", cleanPath);

            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(cleanPath).normalize();

            log.info("Upload directory: {}", uploadPath);
            log.info("Resolved file path: {}", filePath);

            if (!filePath.toAbsolutePath().normalize().startsWith(uploadPath.toAbsolutePath().normalize())) {
                log.error("Security violation: File path outside upload directory");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!Files.exists(filePath)) {
                log.error("File not found at: {}", filePath.toAbsolutePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            if (!Files.isReadable(filePath)) {
                log.error("File not readable: {}", filePath);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            String contentType = Files.probeContentType(filePath);
            if (contentType == null || "application/octet-stream".equals(contentType)) {
                contentType = getContentTypeForExtension(cleanPath);
            }

            String filename = filePath.getFileName().toString();
            long fileSize = Files.size(filePath);

            log.info("Serving file: {} (size: {} bytes, type: {}, download: {})",
                    filename, fileSize, contentType, download);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(fileSize);

            String disposition = download ? "attachment; filename=\"" + filename + "\""
                    : "inline; filename=\"" + filename + "\"";
            headers.set(HttpHeaders.CONTENT_DISPOSITION, disposition);

            headers.setCacheControl("max-age=3600");

            return new ResponseEntity<>(resource, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Error handling file request for path: {}", path, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private String getContentTypeForExtension(String filename) {
        if (filename == null) {
            return "application/octet-stream";
        }

        String lowerName = filename.toLowerCase();

        if (lowerName.endsWith(".pdf")) return "application/pdf";
        if (lowerName.endsWith(".doc")) return "application/msword";
        if (lowerName.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lowerName.endsWith(".xls")) return "application/vnd.ms-excel";
        if (lowerName.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lowerName.endsWith(".jpg") || lowerName.endsWith(".jpeg")) return "image/jpeg";
        if (lowerName.endsWith(".png")) return "image/png";
        if (lowerName.endsWith(".gif")) return "image/gif";
        if (lowerName.endsWith(".webp")) return "image/webp";
        if (lowerName.endsWith(".txt")) return "text/plain";
        if (lowerName.endsWith(".zip")) return "application/zip";
        if (lowerName.endsWith(".csv")) return "text/csv";

        return "application/octet-stream";
    }
}