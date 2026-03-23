package com.wisecartecommerce.ecommerce.controller.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.HomepageSectionRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.HomepageSectionResponse;
import com.wisecartecommerce.ecommerce.service.HomepageSectionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/homepage-sections")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Homepage Sections", description = "Manage homepage section content")
public class AdminHomepageSectionController {

    private final HomepageSectionService homepageSectionService;

    @GetMapping
    @Operation(summary = "Get all homepage sections with config")
    public ResponseEntity<ApiResponse<List<HomepageSectionResponse>>> getAllSections() {
        return ResponseEntity.ok(ApiResponse.success("Sections retrieved",
                homepageSectionService.getAllSections()));
    }

    @GetMapping("/{sectionKey}")
    @Operation(summary = "Get a specific section by key")
    public ResponseEntity<ApiResponse<HomepageSectionResponse>> getSection(
            @PathVariable String sectionKey) {
        return ResponseEntity.ok(ApiResponse.success("Section retrieved",
                homepageSectionService.getSectionByKey(sectionKey)));
    }

    @PutMapping("/{sectionKey}")
    @Operation(summary = "Update a homepage section configuration")
    public ResponseEntity<ApiResponse<HomepageSectionResponse>> updateSection(
            @PathVariable String sectionKey,
            @Valid @RequestBody HomepageSectionRequest request) {
        HomepageSectionResponse response = homepageSectionService.updateSection(sectionKey, request);
        return ResponseEntity.ok(ApiResponse.success("Section updated successfully", response));
    }

    @PostMapping("/initialize")
    @Operation(summary = "Initialize default sections (run once on setup)")
    public ResponseEntity<ApiResponse<Void>> initializeSections() {
        homepageSectionService.initializeDefaultSections();
        return ResponseEntity.ok(ApiResponse.success("Default sections initialized", null));
    }

    @PostMapping("/{sectionKey}/banner-image")
    @Operation(summary = "Upload section banner image")
    public ResponseEntity<ApiResponse<String>> uploadBannerImage(
            @PathVariable String sectionKey,
            @RequestParam("file") MultipartFile file) {
        String url = homepageSectionService.uploadBannerImage(sectionKey, file);
        return ResponseEntity.ok(ApiResponse.success("Banner image uploaded", url));
    }

    @PostMapping
    public ResponseEntity<HomepageSectionResponse> createSection(
            @RequestBody HomepageSectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(homepageSectionService.createSection(request));
    }

    @DeleteMapping("/{sectionKey}")
    public ResponseEntity<Void> deleteSection(@PathVariable String sectionKey) {
        homepageSectionService.deleteSection(sectionKey);
        return ResponseEntity.noContent().build();
    }
}
