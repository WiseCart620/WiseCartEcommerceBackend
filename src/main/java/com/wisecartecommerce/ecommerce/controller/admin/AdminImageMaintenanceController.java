package com.wisecartecommerce.ecommerce.controller.admin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/admin/maintenance")
@PreAuthorize("hasRole('ADMIN')")
@Slf4j
public class AdminImageMaintenanceController {

    @Value("${app.file.upload-dir:./uploads}")
    private String uploadDir;

    private static final long RESIZE_THRESHOLD_BYTES = 400 * 1024; // only touch files > 400KB
    private static final Set<String> IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png");
    // intentionally excludes .gif and .webp — gif resizing breaks animation, webp needs different handling

    @PostMapping("/resize-existing-images")
    public ResponseEntity<Map<String, Object>> resizeExistingImages(
            @RequestParam(defaultValue = "false") boolean dryRun) {

        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        List<String> resized = new ArrayList<>();
        List<String> skipped = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        long totalBytesBefore = 0;
        long totalBytesAfter = 0;

        if (!Files.exists(root)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Upload directory not found: " + root));
        }

        try (Stream<Path> walk = Files.walk(root)) {
            List<Path> files = walk.filter(Files::isRegularFile).toList();

            for (Path path : files) {
                String name = path.getFileName().toString().toLowerCase();
                boolean isImage = IMAGE_EXTENSIONS.stream().anyMatch(name::endsWith);
                if (!isImage) {
                    continue;
                }

                try {
                    long sizeBefore = Files.size(path);
                    if (sizeBefore <= RESIZE_THRESHOLD_BYTES) {
                        skipped.add(path.getFileName().toString() + " (already small: " + sizeBefore + " bytes)");
                        continue;
                    }

                    totalBytesBefore += sizeBefore;

                    if (!dryRun) {
                        // Resize into a temp file first, then atomically replace —
                        // avoids corrupting the original if something goes wrong mid-write
                        Path tempFile = path.resolveSibling(path.getFileName() + ".tmp");

                        net.coobird.thumbnailator.Thumbnails.of(path.toFile())
                                .size(1200, 1200)
                                .outputQuality(0.82)
                                .keepAspectRatio(true)
                                .toFile(tempFile.toFile());

                        long sizeAfter = Files.size(tempFile);

                        // Only replace if the resize actually made it smaller
                        if (sizeAfter < sizeBefore) {
                            Files.move(tempFile, path, StandardCopyOption.REPLACE_EXISTING);
                            totalBytesAfter += sizeAfter;
                            resized.add(path.getFileName().toString() + " (" + sizeBefore + " -> " + sizeAfter + " bytes)");
                        } else {
                            Files.deleteIfExists(tempFile);
                            totalBytesAfter += sizeBefore;
                            skipped.add(path.getFileName().toString() + " (resize didn't shrink it, kept original)");
                        }
                    } else {
                        resized.add(path.getFileName().toString() + " (" + sizeBefore + " bytes, would resize)");
                        totalBytesAfter += sizeBefore; // unknown in dry run
                    }

                } catch (Exception e) {
                    log.error("Failed to resize {}: {}", path, e.getMessage());
                    failed.add(path.getFileName().toString() + " (" + e.getMessage() + ")");
                }
            }

        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to walk upload directory: " + e.getMessage()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dryRun", dryRun);
        result.put("resizedCount", resized.size());
        result.put("skippedCount", skipped.size());
        result.put("failedCount", failed.size());
        result.put("totalBytesBefore", totalBytesBefore);
        result.put("totalBytesAfter", totalBytesAfter);
        result.put("resized", resized);
        result.put("skipped", skipped);
        result.put("failed", failed);

        log.info("Image resize job complete: {} resized, {} skipped, {} failed, dryRun={}",
                resized.size(), skipped.size(), failed.size(), dryRun);

        return ResponseEntity.ok(result);
    }
}