package com.wisecartecommerce.ecommerce.controller.customer;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.HomepageSectionResponse;
import com.wisecartecommerce.ecommerce.service.HomepageSectionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/homepage-sections")
@RequiredArgsConstructor
@Tag(name = "Public Homepage Sections", description = "Retrieve homepage section config for storefront")
public class PublicHomepageSectionController {

    private final HomepageSectionService homepageSectionService;

    @GetMapping
    @Operation(summary = "Get all active section configs (storefront use)")
    public ResponseEntity<ApiResponse<List<HomepageSectionResponse>>> getActiveSections() {
        return ResponseEntity.ok(ApiResponse.success("Active sections retrieved",
                homepageSectionService.getActiveSections()));
    }

    @GetMapping("/{sectionKey}")
    @Operation(summary = "Get a specific active section config")
    public ResponseEntity<ApiResponse<HomepageSectionResponse>> getSection(
            @PathVariable String sectionKey) {
        return ResponseEntity.ok(ApiResponse.success("Section retrieved",
                homepageSectionService.getSectionByKey(sectionKey)));
    }
}