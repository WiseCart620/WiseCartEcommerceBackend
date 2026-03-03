package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Request.HomepageSectionRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.HomepageSectionResponse;
import com.wisecartecommerce.ecommerce.service.HomepageSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
}