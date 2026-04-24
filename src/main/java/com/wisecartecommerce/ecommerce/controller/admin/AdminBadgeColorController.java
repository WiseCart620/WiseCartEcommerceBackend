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
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Request.BadgeColorRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.BadgeColorResponse;
import com.wisecartecommerce.ecommerce.service.BadgeColorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/badge-colors")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Badge Colors", description = "Badge color management APIs for administrators")
public class AdminBadgeColorController {

    private final BadgeColorService badgeColorService;

    @GetMapping
    @Operation(summary = "Get all badge colors")
    public ResponseEntity<ApiResponse<List<BadgeColorResponse>>> getAllBadgeColors() {
        return ResponseEntity.ok(ApiResponse.success("Badge colors retrieved", badgeColorService.getAllBadgeColors()));
    }

    @GetMapping("/active")
    @Operation(summary = "Get active badge colors for public")
    public ResponseEntity<ApiResponse<List<BadgeColorResponse>>> getActiveBadgeColors() {
        return ResponseEntity.ok(ApiResponse.success("Active badge colors retrieved", badgeColorService.getActiveBadgeColors()));
    }

    @PostMapping
    @Operation(summary = "Create or update badge color")
    public ResponseEntity<ApiResponse<BadgeColorResponse>> createOrUpdateBadgeColor(@Valid @RequestBody BadgeColorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Badge color saved", badgeColorService.createOrUpdateBadgeColor(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update badge color")
    public ResponseEntity<ApiResponse<BadgeColorResponse>> updateBadgeColor(@PathVariable Long id, @Valid @RequestBody BadgeColorRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Badge color updated", badgeColorService.updateBadgeColor(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete badge color")
    public ResponseEntity<ApiResponse<Void>> deleteBadgeColor(@PathVariable Long id) {
        badgeColorService.deleteBadgeColor(id);
        return ResponseEntity.ok(ApiResponse.success("Badge color deleted", null));
    }
}