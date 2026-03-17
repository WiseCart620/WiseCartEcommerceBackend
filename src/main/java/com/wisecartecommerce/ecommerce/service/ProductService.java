package com.wisecartecommerce.ecommerce.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.ProductRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.ProductVariationRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.DescriptionImageResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductVariationResponse;
import com.wisecartecommerce.ecommerce.entity.ProductImage;

public interface ProductService {

    ProductResponse updateProduct(Long id, ProductRequest request);

    void deleteProduct(Long id);

    ProductResponse getProductById(Long id);

    ProductResponse getProductBySku(String sku);

    Page<ProductResponse> getAllProducts(Pageable pageable, Long categoryId, Boolean active, String search);

    Page<ProductResponse> getActiveProducts(Pageable pageable, Long categoryId, BigDecimal minPrice,
            BigDecimal maxPrice, String search, Boolean inStock, Boolean onSale);

    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);

    List<ProductResponse> getFeaturedProducts(int limit);

    List<ProductResponse> getNewArrivals(int limit);

    List<ProductResponse> getTopSellingProducts(int limit);

    Page<ProductResponse> getProductsOnSale(Pageable pageable);

    List<ProductResponse> getRelatedProducts(Long productId, int limit);

    List<String> getSearchSuggestions(String query, int limit);

    ProductResponse addProductImage(Long productId, MultipartFile file, boolean isPrimary, int displayOrder);

    void deleteProductImage(Long productId, Long imageId);

    ProductResponse updateStock(Long id, Integer quantity);

    ProductResponse toggleProductStatus(Long id, boolean active);

    ProductResponse updatePrice(Long id, BigDecimal price);

    ProductResponse toggleFeaturedStatus(Long id, boolean featured);

    List<ProductResponse> getLowStockProducts(int threshold);

    List<ProductResponse> searchProducts(String query, int limit);

    void incrementViewCount(Long id);

    ProductResponse createProduct(ProductRequest request, MultipartFile image);

    default ProductResponse createProduct(ProductRequest request) {
        return createProduct(request, null);
    }

    Object getProductStats();

    Object getPublicConfig();

    Object getPublicStats();

    ProductVariationResponse addVariation(Long productId, ProductVariationRequest request);

    ProductVariationResponse updateVariation(Long productId, Long variationId, ProductVariationRequest request);

    void deleteVariation(Long productId, Long variationId);

    List<ProductVariationResponse> getVariations(Long productId);

    ProductVariationResponse uploadVariationImage(Long productId, Long variationId, MultipartFile file);

    ProductVariationResponse updateVariationStock(Long productId, Long variationId, Integer quantity);

    List<ProductResponse.ProductImageResponse> addProductImages(Long productId, List<MultipartFile> files);

    // NEW: Description image management methods
    List<DescriptionImageResponse> uploadDescriptionImages(Long productId, List<MultipartFile> files);

    void deleteDescriptionImage(Long productId, Long imageId);

    void processDescriptionImages(Long productId, String description);

    List<ProductImage> getDescriptionImages(Long productId);
    
    // ADD THIS NEW METHOD
    List<DescriptionImageResponse> getDescriptionImageResponses(Long productId);
}