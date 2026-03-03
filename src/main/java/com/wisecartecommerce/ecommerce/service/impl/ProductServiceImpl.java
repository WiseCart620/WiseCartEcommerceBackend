package com.wisecartecommerce.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.ProductRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.ProductVariationRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductVariationResponse;
import com.wisecartecommerce.ecommerce.entity.Category;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.ProductImage;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.CategoryRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.repository.ProductVariationRepository;
import com.wisecartecommerce.ecommerce.repository.ReviewRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import com.wisecartecommerce.ecommerce.service.ProductService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final FileStorageService fileStorageService;
    private final ProductVariationRepository productVariationRepository;

    @Override
    @Transactional
    public ProductResponse createProduct(ProductRequest request, MultipartFile image) {
        if (request.getSku() != null && productRepository.findBySku(request.getSku()).isPresent()) {
            throw new CustomException("SKU already exists: " + request.getSku());
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Category not found with id: " + request.getCategoryId()));

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .category(category)
                .sku(request.getSku())
                .upc(request.getUpc())
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .label(request.getLabel())
                .active(true)
                .build();

        Product savedProduct = productRepository.save(product);

        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = fileStorageService.uploadProductImage(image, savedProduct.getId());

                ProductImage productImage = ProductImage.builder()
                        .imageUrl(imageUrl)
                        .primary(true)
                        .displayOrder(0)
                        .build();

                savedProduct.addImage(productImage);
                savedProduct.setImageUrl(imageUrl);
                savedProduct = productRepository.save(savedProduct);

                log.info("Image uploaded for new product: {} (ID: {})",
                        savedProduct.getName(), savedProduct.getId());
            } catch (Exception e) {
                log.error("Failed to upload image for product: {}", savedProduct.getId(), e);
            }
        }
        if (request.getVariations() != null && !request.getVariations().isEmpty()) {
            for (ProductVariationRequest varReq : request.getVariations()) {
                validateVariationUniqueness(varReq.getSku(), varReq.getUpc(), null);
                ProductVariation variation = mapToVariationEntity(varReq, savedProduct);
                savedProduct.addVariation(variation);
            }
            savedProduct = productRepository.save(savedProduct);
        }
        log.info("Product created: {} (ID: {})", savedProduct.getName(), savedProduct.getId());
        return mapToResponse(savedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Check if SKU is being changed and already exists
        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            productRepository.findBySku(request.getSku())
                    .ifPresent(p -> {
                        throw new CustomException("SKU already exists: " + request.getSku());
                    });
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Category not found with id: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setSku(request.getSku());
        product.setUpc(request.getUpc());
        product.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        product.setLabel(request.getLabel());
        if (request.getVariations() != null) {
            // Build a map of existing variations by ID and name for image preservation
            Map<Long, String> existingImagesByID = product.getVariations().stream()
                    .filter(v -> v.getId() != null && v.getImageUrl() != null)
                    .collect(Collectors.toMap(ProductVariation::getId, ProductVariation::getImageUrl));
            Map<String, String> existingImagesByName = product.getVariations().stream()
                    .filter(v -> v.getImageUrl() != null)
                    .collect(Collectors.toMap(ProductVariation::getName, ProductVariation::getImageUrl,
                            (a, b) -> a)); // keep first if duplicate names

            product.getVariations().clear();

            for (ProductVariationRequest varReq : request.getVariations()) {
                // Skip uniqueness check for variations that already belong to this product
                validateVariationUniquenessForUpdate(varReq.getSku(), varReq.getUpc(), product.getId());
                ProductVariation variation = mapToVariationEntity(varReq, product);

                // Restore image URL: prefer match by existing ID, then by name
                if (varReq.getId() != null && existingImagesByID.containsKey(varReq.getId())) {
                    variation.setImageUrl(existingImagesByID.get(varReq.getId()));
                } else if (existingImagesByName.containsKey(varReq.getName())) {
                    variation.setImageUrl(existingImagesByName.get(varReq.getName()));
                }

                product.addVariation(variation);
            }
        }
        Product updatedProduct = productRepository.save(product);
        log.info("Product updated: {} (ID: {})", updatedProduct.getName(), updatedProduct.getId());

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Delete product images from storage
        product.getImages().forEach(image -> {
            try {
                fileStorageService.deleteFile(image.getImageUrl());
            } catch (Exception e) {
                log.error("Failed to delete image: {}", image.getImageUrl(), e);
            }
        });

        productRepository.delete(product);
        log.info("Product deleted: {} (ID: {})", product.getName(), product.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Increment view count
        product.setViewCount(product.getViewCount() + 1);
        productRepository.save(product);

        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with SKU: " + sku));
        return mapToResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(Pageable pageable, Long categoryId, Boolean active, String search) {
        Page<Product> products;

        if (categoryId != null || active != null || search != null) {
            products = productRepository.findActiveProductsWithFilters(
                    categoryId, null, null, search, null, null, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        return products.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Pageable pageable, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice,
            String search, Boolean inStock, Boolean onSale) {
        Page<Product> products = productRepository.findActiveProductsWithFilters(
                categoryId, minPrice, maxPrice, search, inStock, onSale, pageable);

        return products.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        List<Product> products = productRepository.findByFeaturedTrueAndActiveTrue();
        return products.stream()
                .limit(limit)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by("createdAt").descending());

        List<Product> products = productRepository.findNewArrivals(pageable);
        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getTopSellingProducts(int limit) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by("soldCount").descending());

        List<Product> products = productRepository.findTopSellingProducts(pageable);
        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsOnSale(Pageable pageable) {
        return productRepository.findProductsOnSale(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getCategory() == null) {
            return Collections.emptyList();
        }

        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        List<Product> relatedProducts = productRepository.findRelatedProducts(
                product.getCategory().getId(), productId, pageable);

        return relatedProducts.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        return productRepository.findProductNameSuggestions(query.trim(), pageable);
    }

    @Override
    @Transactional
    public ProductResponse addProductImage(Long productId, MultipartFile file, boolean isPrimary, int displayOrder) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        try {
            String imageUrl = fileStorageService.uploadProductImage(file, productId);

            ProductImage productImage = ProductImage.builder()
                    .imageUrl(imageUrl)
                    .primary(isPrimary)
                    .displayOrder(displayOrder)
                    .build();

            // If this is primary, unset other primary images
            if (isPrimary) {
                product.getImages().forEach(img -> img.setPrimary(false));
            }

            product.addImage(productImage);

            // If this is the first image, set it as primary and main image
            if (product.getImages().size() == 1) {
                productImage.setPrimary(true);
                product.setImageUrl(imageUrl);
            }

            // If this is primary, update main image URL
            if (isPrimary) {
                product.setImageUrl(imageUrl);
            }

            Product savedProduct = productRepository.save(product);
            log.info("Image added to product: {} (ID: {})", product.getName(), productId);

            return mapToResponse(savedProduct);

        } catch (Exception e) {
            log.error("Failed to upload product image", e);
            throw new CustomException("Failed to upload image: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void deleteProductImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductImage imageToDelete = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found"));

        // Delete file from storage
        try {
            fileStorageService.deleteFile(imageToDelete.getImageUrl());
        } catch (Exception e) {
            log.error("Failed to delete image file: {}", imageToDelete.getImageUrl(), e);
        }

        product.getImages().remove(imageToDelete);

        // If we deleted the primary image, set a new primary
        if (imageToDelete.isPrimary() && !product.getImages().isEmpty()) {
            product.getImages().get(0).setPrimary(true);
            product.setImageUrl(product.getImages().get(0).getImageUrl());
        }

        productRepository.save(product);
        log.info("Image deleted from product: {} (ID: {})", product.getName(), productId);
    }

    @Override
    @Transactional
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (quantity < 0) {
            throw new CustomException("Stock quantity cannot be negative");
        }

        product.setStockQuantity(quantity);
        Product updatedProduct = productRepository.save(product);

        log.info("Stock updated for product: {} (ID: {}) to {}",
                product.getName(), id, quantity);

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse toggleProductStatus(Long id, boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setActive(active);
        Product updatedProduct = productRepository.save(product);

        log.info("Product status updated: {} (ID: {}) to {}",
                product.getName(), id, active ? "active" : "inactive");

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse updatePrice(Long id, BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("Price must be greater than 0");
        }

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setPrice(price);
        Product updatedProduct = productRepository.save(product);

        log.info("Price updated for product: {} (ID: {}) to {}",
                product.getName(), id, price);

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    public ProductResponse toggleFeaturedStatus(Long id, boolean featured) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setFeatured(featured);
        Product updatedProduct = productRepository.save(product);

        log.info("Featured status updated for product: {} (ID: {}) to {}",
                product.getName(), id, featured);

        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(int threshold) {
        List<Product> products = productRepository.findLowStockProducts(threshold);
        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Object getProductStats() {
        Long totalProducts = productRepository.count();
        Long activeProducts = productRepository.countActiveProducts();
        Long outOfStock = Long.valueOf(productRepository.findLowStockProducts(1).size());
        Long onSale = productRepository.findProductsOnSale(
                org.springframework.data.domain.Pageable.unpaged()).getTotalElements();

        BigDecimal totalSold = BigDecimal.valueOf(productRepository.getTotalSoldCount());
        BigDecimal avgRating = productRepository.getAverageRating();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts);
        stats.put("activeProducts", activeProducts);
        stats.put("outOfStock", outOfStock);
        stats.put("onSale", onSale);
        stats.put("totalSold", totalSold);
        stats.put("averageRating", avgRating != null ? avgRating.setScale(2, RoundingMode.HALF_UP) : 0);

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Object getPublicConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("currency", "USD");
        config.put("currencySymbol", "$");
        config.put("taxRate", 0.08); // 8%
        config.put("shippingCost", 5.99);
        config.put("freeShippingThreshold", 50.00);
        config.put("returnPeriodDays", 30);

        return config;
    }

    @Override
    @Transactional(readOnly = true)
    public Object getPublicStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", productRepository.countActiveProducts());
        stats.put("totalCategories", categoryRepository.count());
        stats.put("featuredProducts", productRepository.findByFeaturedTrueAndActiveTrue().size());

        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable) {
        Page<Product> products = productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable); // ✅ Fixed
        return products.map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, limit);
        List<Product> products = productRepository.searchProducts(query.trim(), pageable);

        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ─── REPLACE the existing mapToResponse method in ProductServiceImpl.java
    // ───────
    // This method now aggregates price and stock from variations when they exist.

    private ProductResponse mapToResponse(Product product) {
        // Build image list
        ProductResponse.ProductImageResponse[] primaryImage = { null };
        List<ProductResponse.ProductImageResponse> images = product.getImages().stream()
                .map(img -> {
                    ProductResponse.ProductImageResponse imageResponse = ProductResponse.ProductImageResponse.builder()
                            .id(img.getId())
                            .imageUrl(img.getImageUrl())
                            .isPrimary(img.isPrimary())
                            .displayOrder(img.getDisplayOrder())
                            .build();
                    if (img.isPrimary())
                        primaryImage[0] = imageResponse;
                    return imageResponse;
                })
                .collect(Collectors.toList());

        // Map variations
        List<ProductVariationResponse> variationResponses = product.getVariations().stream()
                .map(this::mapToVariationResponse)
                .collect(Collectors.toList());

        boolean hasVariations = !variationResponses.isEmpty();

        // ── Variation-aware price & stock ─────────────────────────────────────────
        BigDecimal displayPrice;
        BigDecimal displayDiscountedPrice;
        Integer displayStock;
        boolean displayInStock;
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        BigDecimal minDiscountedPrice = null;

        if (hasVariations) {
            // Min / max raw price across variations
            minPrice = variationResponses.stream()
                    .map(ProductVariationResponse::getPrice)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(product.getPrice());

            maxPrice = variationResponses.stream()
                    .map(ProductVariationResponse::getPrice)
                    .filter(Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(product.getPrice());

            // Min discounted price (best deal shown to customer)
            minDiscountedPrice = variationResponses.stream()
                    .map(ProductVariationResponse::getDiscountedPrice)
                    .filter(Objects::nonNull)
                    .min(Comparator.naturalOrder())
                    .orElse(minPrice);

            // Display price = cheapest variation; discounted = cheapest discounted
            displayPrice = minPrice;
            displayDiscountedPrice = minDiscountedPrice;

            // Total stock = sum of all variation stocks
            displayStock = variationResponses.stream()
                    .map(ProductVariationResponse::getStockQuantity)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();

            // In stock if at least one variation has stock > 0
            displayInStock = variationResponses.stream()
                    .anyMatch(ProductVariationResponse::isInStock);
        } else {
            displayPrice = product.getPrice();
            displayDiscountedPrice = product.getDiscountedPrice();
            displayStock = product.getStockQuantity();
            displayInStock = product.isInStock();
        }

        // Review summary
        Object reviewSummary = reviewRepository.getProductReviewSummary(product.getId());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                // Prices
                .price(displayPrice)
                .discountedPrice(displayDiscountedPrice)
                .hasVariations(hasVariations)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minDiscountedPrice(minDiscountedPrice)
                // Stock
                .stockQuantity(displayStock)
                .inStock(displayInStock)
                // Category
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                // Identity
                .sku(product.getSku())
                .upc(product.getUpc())
                // Images
                .imageUrl(product.getImageUrl())
                .images(images)
                .primaryImage(primaryImage[0])
                // Metadata
                .discount(product.getDiscount())
                .rating(product.getRating())
                .reviewCount(product.getReviewCount())
                .soldCount(product.getSoldCount())
                .viewCount(product.getViewCount())
                .variations(variationResponses)
                .active(product.isActive())
                .featured(product.isFeatured())
                .label(product.getLabel())
                .reviewSummary(reviewSummary)
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public ProductVariationResponse addVariation(Long productId, ProductVariationRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        validateVariationUniqueness(request.getSku(), request.getUpc(), null);

        ProductVariation variation = mapToVariationEntity(request, product);
        product.addVariation(variation);
        productRepository.save(product);

        log.info("Variation '{}' added to product ID: {}", variation.getName(), productId);
        return mapToVariationResponse(variation);
    }

    @Override
    @Transactional
    public ProductVariationResponse updateVariation(Long productId, Long variationId, ProductVariationRequest request) {
        ProductVariation variation = getVariationOrThrow(productId, variationId);

        validateVariationUniqueness(request.getSku(), request.getUpc(), variationId);

        variation.setName(request.getName());
        variation.setSku(request.getSku());
        variation.setUpc(request.getUpc());
        variation.setPrice(request.getPrice());
        variation.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        variation.setStockQuantity(request.getStockQuantity());

        ProductVariation saved = productVariationRepository.save(variation);
        log.info("Variation ID: {} updated for product ID: {}", variationId, productId);
        return mapToVariationResponse(saved);
    }

    @Override
    @Transactional
    public void deleteVariation(Long productId, Long variationId) {
        ProductVariation variation = getVariationOrThrow(productId, variationId);

        if (variation.getImageUrl() != null) {
            try {
                fileStorageService.deleteFile(variation.getImageUrl());
            } catch (Exception e) {
                log.error("Failed to delete variation image: {}", variation.getImageUrl(), e);
            }
        }

        productVariationRepository.delete(variation);
        log.info("Variation ID: {} deleted from product ID: {}", variationId, productId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVariationResponse> getVariations(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return productVariationRepository.findByProductId(productId)
                .stream().map(this::mapToVariationResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductVariationResponse uploadVariationImage(Long productId, Long variationId, MultipartFile file) {
        ProductVariation variation = getVariationOrThrow(productId, variationId);

        try {
            // Delete old image if exists
            if (variation.getImageUrl() != null) {
                fileStorageService.deleteFile(variation.getImageUrl());
            }
            String imageUrl = fileStorageService.uploadProductImage(file, productId);
            variation.setImageUrl(imageUrl);
            ProductVariation saved = productVariationRepository.save(variation);
            return mapToVariationResponse(saved);
        } catch (Exception e) {
            log.error("Failed to upload variation image", e);
            throw new CustomException("Failed to upload variation image: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ProductVariationResponse updateVariationStock(Long productId, Long variationId, Integer quantity) {
        if (quantity < 0)
            throw new CustomException("Stock quantity cannot be negative");

        ProductVariation variation = getVariationOrThrow(productId, variationId);
        variation.setStockQuantity(quantity);
        ProductVariation saved = productVariationRepository.save(variation);

        log.info("Stock updated for variation ID: {} to {}", variationId, quantity);
        return mapToVariationResponse(saved);
    }

    // ---- Private helpers ----

    private ProductVariation getVariationOrThrow(Long productId, Long variationId) {
        ProductVariation variation = productVariationRepository.findById(variationId)
                .orElseThrow(() -> new ResourceNotFoundException("Variation not found with id: " + variationId));
        if (!variation.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException("Variation does not belong to product id: " + productId);
        }
        return variation;
    }

    private void validateVariationUniqueness(String sku, String upc, Long excludeId) {
        if (sku != null) {
            productVariationRepository.findBySku(sku).ifPresent(v -> {
                if (excludeId == null || !v.getId().equals(excludeId))
                    throw new CustomException("Variation SKU already exists: " + sku);
            });
        }
        if (upc != null) {
            productVariationRepository.findByUpc(upc).ifPresent(v -> {
                if (excludeId == null || !v.getId().equals(excludeId))
                    throw new CustomException("Variation UPC already exists: " + upc);
            });
        }
    }

    @Override
    @Transactional
    public List<ProductResponse.ProductImageResponse> addProductImages(Long productId, List<MultipartFile> files) {
        if (files == null || files.isEmpty())
            throw new CustomException("No files provided");
        if (files.size() > 10)
            throw new CustomException("Maximum 10 images allowed at once");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int nextOrder = product.getImages().stream()
                .mapToInt(ProductImage::getDisplayOrder)
                .max().orElse(-1) + 1;

        boolean hasNoPrimary = product.getImages().isEmpty();
        List<ProductResponse.ProductImageResponse> results = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            try {
                String imageUrl = fileStorageService.uploadProductImage(file, productId);
                boolean isPrimary = hasNoPrimary && i == 0;

                ProductImage productImage = ProductImage.builder()
                        .imageUrl(imageUrl)
                        .primary(isPrimary)
                        .displayOrder(nextOrder + i)
                        .build();

                product.addImage(productImage);

                if (isPrimary)
                    product.setImageUrl(imageUrl);

                results.add(ProductResponse.ProductImageResponse.builder()
                        .imageUrl(imageUrl)
                        .isPrimary(isPrimary)
                        .displayOrder(nextOrder + i)
                        .build());

            } catch (Exception e) {
                log.error("Failed to upload image {} for product {}", file.getOriginalFilename(), productId, e);
                throw new CustomException("Failed to upload image: " + file.getOriginalFilename());
            }
        }

        productRepository.save(product);
        return results;
    }

    private void validateVariationUniquenessForUpdate(String sku, String upc, Long productId) {
        if (sku != null) {
            productVariationRepository.findBySku(sku).ifPresent(v -> {
                if (!v.getProduct().getId().equals(productId))
                    throw new CustomException("Variation SKU already exists: " + sku);
            });
        }
        if (upc != null) {
            productVariationRepository.findByUpc(upc).ifPresent(v -> {
                if (!v.getProduct().getId().equals(productId))
                    throw new CustomException("Variation UPC already exists: " + upc);
            });
        }
    }

    private ProductVariation mapToVariationEntity(ProductVariationRequest request, Product product) {
        return ProductVariation.builder()
                .product(product)
                .name(request.getName())
                .sku(request.getSku())
                .upc(request.getUpc())
                .price(request.getPrice())
                .discount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO)
                .stockQuantity(request.getStockQuantity())
                .weightKg(request.getWeightKg())
                .heightCm(request.getHeightCm())
                .widthCm(request.getWidthCm())
                .lengthCm(request.getLengthCm())
                .active(true)
                .build();
    }

    private ProductVariationResponse mapToVariationResponse(ProductVariation v) {
        return ProductVariationResponse.builder()
                .id(v.getId())
                .name(v.getName())
                .sku(v.getSku())
                .upc(v.getUpc())
                .price(v.getPrice())
                .discount(v.getDiscount())
                .discountedPrice(v.getDiscountedPrice())
                .stockQuantity(v.getStockQuantity())
                .inStock(v.isInStock())
                .imageUrl(v.getImageUrl())
                .weightKg(v.getWeightKg())
                .heightCm(v.getHeightCm())
                .widthCm(v.getWidthCm())
                .lengthCm(v.getLengthCm())
                .active(v.isActive())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}