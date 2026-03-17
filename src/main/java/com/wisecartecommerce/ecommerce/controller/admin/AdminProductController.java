package com.wisecartecommerce.ecommerce.controller.admin;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.ProductRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.ProductVariationRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.DescriptionImageResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductVariationResponse;
import com.wisecartecommerce.ecommerce.service.ProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Products", description = "Product management APIs for administrators")
public class AdminProductController {

    private final ProductService productService;

    @PostMapping(consumes = {"multipart/form-data"})
    @Operation(summary = "Create a new product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @RequestPart("product") @Valid ProductRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        ProductResponse response = productService.createProduct(request, image);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Product created successfully", response));
    }

    @PostMapping("/{productId}/description-images")
    @Operation(summary = "Upload images for product description")
    public ResponseEntity<ApiResponse<List<DescriptionImageResponse>>> uploadDescriptionImages(
            @PathVariable Long productId,
            @RequestParam("files") List<MultipartFile> files) {

        if (files.size() > 20) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Maximum 20 images allowed for description"));
        }

        List<DescriptionImageResponse> responses = productService.uploadDescriptionImages(productId, files);
        return ResponseEntity.ok(ApiResponse.success("Description images uploaded successfully", responses));
    }

    @GetMapping
    @Operation(summary = "Get all products with pagination")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) String search) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ProductResponse> products = productService.getAllProducts(pageable, categoryId, active, search);

        return ResponseEntity.ok(ApiResponse.success("Products retrieved", products));
    }

    @GetMapping("/{productId}/description-images")
    @Operation(summary = "Get all description images for a product")
    public ResponseEntity<ApiResponse<List<DescriptionImageResponse>>> getDescriptionImages(
            @PathVariable Long productId) {
        List<DescriptionImageResponse> descriptionImages = productService.getDescriptionImageResponses(productId);
        return ResponseEntity.ok(ApiResponse.success("Description images retrieved", descriptionImages));
    }

    @GetMapping("/{productId}/description-images")
    @Operation(summary = "Get product with its description images")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductWithDescriptionImages(
            @PathVariable Long productId) {
        ProductResponse product = productService.getProductById(productId);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved", product));
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "50") int size) {
        List<ProductResponse> results = q.isBlank()
                ? productService.getAllProducts(
                        org.springframework.data.domain.PageRequest.of(0, size,
                                org.springframework.data.domain.Sort.by("createdAt").descending()),
                        null, null, null).getContent()
                : productService.searchProducts(q, size);
        return ResponseEntity.ok(ApiResponse.success("Products found", results));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(
            @PathVariable Long id) {
        ProductResponse response = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success("Product retrieved", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a product")
    public ResponseEntity<ApiResponse<ProductResponse>> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        ProductResponse response = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success("Product updated successfully", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
    }

    @DeleteMapping("/{productId}/description-images/{imageId}")
    @Operation(summary = "Delete a description image")
    public ResponseEntity<ApiResponse<Void>> deleteDescriptionImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteDescriptionImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Description image deleted successfully", null));
    }

    @PostMapping("/{productId}/images")
    @Operation(summary = "Upload product image")
    public ResponseEntity<ApiResponse<ProductResponse>> uploadProductImage(
            @PathVariable Long productId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "isPrimary", defaultValue = "false") boolean isPrimary,
            @RequestParam(value = "displayOrder", defaultValue = "0") int displayOrder) {

        ProductResponse response = productService.addProductImage(productId, file, isPrimary, displayOrder);
        return ResponseEntity.ok(ApiResponse.success("Image uploaded successfully", response));
    }

    @DeleteMapping("/{productId}/images/{imageId}")
    @Operation(summary = "Delete product image")
    public ResponseEntity<ApiResponse<Void>> deleteProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiResponse.success("Image deleted successfully", null));
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update product stock")
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        ProductResponse response = productService.updateStock(id, quantity);
        return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle product active status")
    public ResponseEntity<ApiResponse<ProductResponse>> toggleProductStatus(
            @PathVariable Long id,
            @RequestParam boolean active) {
        ProductResponse response = productService.toggleProductStatus(id, active);
        return ResponseEntity.ok(ApiResponse.success(
                active ? "Product activated" : "Product deactivated", response));
    }

    @PatchMapping("/{id}/price")
    @Operation(summary = "Update product price")
    public ResponseEntity<ApiResponse<ProductResponse>> updatePrice(
            @PathVariable Long id,
            @RequestParam BigDecimal price) {
        ProductResponse response = productService.updatePrice(id, price);
        return ResponseEntity.ok(ApiResponse.success("Price updated successfully", response));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get products with low stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStockProducts(
            @RequestParam(defaultValue = "10") int threshold) {
        List<ProductResponse> products = productService.getLowStockProducts(threshold);
        return ResponseEntity.ok(ApiResponse.success("Low stock products retrieved", products));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get product statistics")
    public ResponseEntity<ApiResponse<Object>> getProductStats() {
        Object stats = productService.getProductStats();
        return ResponseEntity.ok(ApiResponse.success("Statistics retrieved", stats));
    }

    // ---- Variation Endpoints ----
    @PostMapping("/{productId}/variations")
    @Operation(summary = "Add a variation to a product")
    public ResponseEntity<ApiResponse<ProductVariationResponse>> addVariation(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariationRequest request) {
        ProductVariationResponse response = productService.addVariation(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Variation added successfully", response));
    }

    @PostMapping("/{productId}/images/bulk")
    @Operation(summary = "Upload up to 10 product images at once")
    public ResponseEntity<ApiResponse<List<ProductResponse.ProductImageResponse>>> uploadProductImages(
            @PathVariable Long productId,
            @RequestParam("files") List<MultipartFile> files) {

        if (files.size() > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Maximum 10 images allowed"));
        }
        var result = productService.addProductImages(productId, files);
        return ResponseEntity.ok(ApiResponse.success("Images uploaded successfully", result));
    }

    @GetMapping("/{productId}/variations")
    @Operation(summary = "Get all variations of a product")
    public ResponseEntity<ApiResponse<List<ProductVariationResponse>>> getVariations(
            @PathVariable Long productId) {
        List<ProductVariationResponse> variations = productService.getVariations(productId);
        return ResponseEntity.ok(ApiResponse.success("Variations retrieved", variations));
    }

    @PutMapping("/{productId}/variations/{variationId}")
    @Operation(summary = "Update a product variation")
    public ResponseEntity<ApiResponse<ProductVariationResponse>> updateVariation(
            @PathVariable Long productId,
            @PathVariable Long variationId,
            @Valid @RequestBody ProductVariationRequest request) {
        ProductVariationResponse response = productService.updateVariation(productId, variationId, request);
        return ResponseEntity.ok(ApiResponse.success("Variation updated successfully", response));
    }

    @DeleteMapping("/{productId}/variations/{variationId}")
    @Operation(summary = "Delete a product variation")
    public ResponseEntity<ApiResponse<Void>> deleteVariation(
            @PathVariable Long productId,
            @PathVariable Long variationId) {
        productService.deleteVariation(productId, variationId);
        return ResponseEntity.ok(ApiResponse.success("Variation deleted successfully", null));
    }

    @PostMapping("/{productId}/variations/{variationId}/image")
    @Operation(summary = "Upload image for a specific variation")
    public ResponseEntity<ApiResponse<ProductVariationResponse>> uploadVariationImage(
            @PathVariable Long productId,
            @PathVariable Long variationId,
            @RequestParam("file") MultipartFile file) {
        ProductVariationResponse response = productService.uploadVariationImage(productId, variationId, file);
        return ResponseEntity.ok(ApiResponse.success("Variation image uploaded successfully", response));
    }

    @PatchMapping("/{productId}/variations/{variationId}/stock")
    @Operation(summary = "Update stock for a specific variation")
    public ResponseEntity<ApiResponse<ProductVariationResponse>> updateVariationStock(
            @PathVariable Long productId,
            @PathVariable Long variationId,
            @RequestParam Integer quantity) {
        ProductVariationResponse response = productService.updateVariationStock(productId, variationId, quantity);
        return ResponseEntity.ok(ApiResponse.success("Variation stock updated successfully", response));
    }
}
