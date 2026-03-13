package com.wisecartecommerce.ecommerce.controller.publicapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.CategoryResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductResponse;
import com.wisecartecommerce.ecommerce.service.CategoryService;
import com.wisecartecommerce.ecommerce.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Public", description = "Public APIs (no authentication required)")
public class PublicController {

    private final CategoryService categoryService;
    private final ProductService productService;

    @GetMapping("/categories")
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAllCategories() {
        List<CategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.success("Categories retrieved", categories));
    }

    @GetMapping("/categories/{id}")
    @Operation(summary = "Get category by ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable Long id) {
        CategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.success("Category retrieved", category));
    }

    @GetMapping("/categories/tree")
    @Operation(summary = "Get category tree")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategoryTree() {
        List<CategoryResponse> categories = categoryService.getCategoryTree();
        return ResponseEntity.ok(ApiResponse.success("Category tree retrieved", categories));
    }

    @GetMapping("/products/featured")
    @Operation(summary = "Get featured products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getFeaturedProducts() {
        List<ProductResponse> products = productService.getFeaturedProducts(8);
        return ResponseEntity.ok(ApiResponse.success("Featured products retrieved", products));
    }

    @GetMapping("/products/new-arrivals")
    @Operation(summary = "Get new arrivals")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getNewArrivals() {
        List<ProductResponse> products = productService.getNewArrivals(8);
        return ResponseEntity.ok(ApiResponse.success("New arrivals retrieved", products));
    }

    @GetMapping("/products/top-selling")
    @Operation(summary = "Get top selling products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getTopSellingProducts() {
        List<ProductResponse> products = productService.getTopSellingProducts(10);
        return ResponseEntity.ok(ApiResponse.success("Top selling products retrieved", products));
    }

    @GetMapping("/products/search")
    @Operation(summary = "Search products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        List<ProductResponse> products = productService.searchProducts(query, limit);
        return ResponseEntity.ok(ApiResponse.success("Search results retrieved", products));
    }

    @GetMapping("/health")
    @Operation(summary = "Health check")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Service is healthy", "OK"));
    }

    @GetMapping("/config")
    @Operation(summary = "Get public configuration")
    public ResponseEntity<ApiResponse<Object>> getConfig() {
        Object config = productService.getPublicConfig();
        return ResponseEntity.ok(ApiResponse.success("Configuration retrieved", config));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get public statistics")
    public ResponseEntity<ApiResponse<Object>> getPublicStats() {
        Object stats = productService.getPublicStats();
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", stats));
    }
}