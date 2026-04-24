package com.wisecartecommerce.ecommerce.controller.customer;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.BadgeColorResponse;
import com.wisecartecommerce.ecommerce.service.BadgeColorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/badge-colors")
@RequiredArgsConstructor
@Tag(name = "Public Badge Colors", description = "Public badge color APIs")
public class PublicBadgeColorController {

    private final BadgeColorService badgeColorService;

    @GetMapping
    @Operation(summary = "Get active badge colors for frontend")
    public ResponseEntity<ApiResponse<List<BadgeColorResponse>>> getActiveBadgeColors() {
        return ResponseEntity.ok(ApiResponse.success("Active badge colors retrieved", badgeColorService.getActiveBadgeColors()));
    }
}