package com.wisecartecommerce.ecommerce.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.ProductRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.ProductVariationRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.DescriptionImageResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductResponse.ProductAddOnResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ProductVariationResponse;
import com.wisecartecommerce.ecommerce.entity.Category;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.ProductImage;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.CategoryRepository;
import com.wisecartecommerce.ecommerce.repository.ProductImageRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.repository.ProductVariationRepository;
import com.wisecartecommerce.ecommerce.repository.ReviewRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import com.wisecartecommerce.ecommerce.service.ProductService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final FileStorageService fileStorageService;
    private final ProductVariationRepository productVariationRepository;
    private final ProductImageRepository productImageRepository;

    // ── Write methods with cache eviction ─────────────────────────────────────
    @Override
    @Transactional
    @CacheEvict(value = {"products", "activeProducts", "featuredProducts", "newArrivals", "topSelling"}, allEntries = true)
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
                .lazadaUrl(request.getLazadaUrl())
                .shopeeUrl(request.getShopeeUrl())
                .recommendationCategory(request.getRecommendationCategoryId() != null
                        ? categoryRepository.findById(request.getRecommendationCategoryId()).orElse(null)
                        : null)
                .recommendedProducts(request.getRecommendedProductIds() != null
                        ? productRepository.findAllById(request.getRecommendedProductIds())
                        : new ArrayList<>())
                .build();

        Product savedProduct = productRepository.save(product);

        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = fileStorageService.uploadProductImage(image, savedProduct.getId());
                ProductImage productImage = ProductImage.builder()
                        .imageUrl(imageUrl)
                        .primary(true)
                        .displayOrder(0)
                        .imageType(ProductImage.ImageType.GALLERY) // Set image type
                        .build();
                savedProduct.addImage(productImage);
                savedProduct.setImageUrl(imageUrl);
                savedProduct = productRepository.save(savedProduct);
                log.info("Image uploaded for new product: {} (ID: {})", savedProduct.getName(), savedProduct.getId());
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

    // ── Description Image Methods ─────────────────────────────────────────────
    @Override
    @Transactional
    public List<DescriptionImageResponse> uploadDescriptionImages(Long productId, List<MultipartFile> files) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        List<DescriptionImageResponse> responses = new ArrayList<>();

        for (MultipartFile file : files) {
            // Validate file
            validateImageFile(file);

            try {
                // Generate unique filename
                String fileName = generateFileName(file);

                // Upload file using FileStorageService
                String fileUrl = fileStorageService.uploadProductImage(file, productId);

                // Create and save ProductImage entity with DESCRIPTION type
                ProductImage productImage = ProductImage.builder()
                        .product(product)
                        .imageUrl(fileUrl)
                        .altText(file.getOriginalFilename())
                        .imageType(ProductImage.ImageType.DESCRIPTION)
                        .displayOrder(0)
                        .primary(false)
                        .build();

                productImage = productImageRepository.save(productImage);

                responses.add(DescriptionImageResponse.builder()
                        .id(productImage.getId())
                        .imageUrl(fileUrl)
                        .altText(productImage.getAltText())
                        .fileName(fileName)
                        .build());

                log.info("Description image uploaded for product ID: {}", productId);

            } catch (Exception e) {
                log.error("Failed to upload description image for product {}: {}", productId, e.getMessage());
                throw new CustomException("Failed to upload image: " + file.getOriginalFilename());
            }
        }

        return responses;
    }

    @Override
    @Transactional
    public void deleteDescriptionImage(Long productId, Long imageId) {
        ProductImage image = productImageRepository.findById(imageId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found with id: " + imageId));

        // Verify the image belongs to the product and is a description image
        if (!image.getProduct().getId().equals(productId)) {
            throw new CustomException("Image does not belong to product id: " + productId);
        }

        if (image.getImageType() != ProductImage.ImageType.DESCRIPTION) {
            throw new CustomException("Image is not a description image");
        }

        try {
            // Delete from storage
            fileStorageService.deleteFile(image.getImageUrl());

            // Delete from database
            productImageRepository.delete(image);

            log.info("Description image deleted: {} for product ID: {}", imageId, productId);
        } catch (Exception e) {
            log.error("Failed to delete description image: {}", imageId, e);
            throw new CustomException("Failed to delete image");
        }
    }

    @Override
    @Transactional
    public void processDescriptionImages(Long productId, String description) {
        // This method can be called when saving a product to clean up
        // unused description images or to process any special formatting
        if (description == null || description.isEmpty()) {
            return;
        }

        // Get all description images for the product
        List<ProductImage> descriptionImages = productImageRepository
                .findByProductIdAndImageType(productId, ProductImage.ImageType.DESCRIPTION);

        // Extract image URLs from the description HTML
        List<String> usedImageUrls = new ArrayList<>();

        // Simple regex to find image src attributes in HTML
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("src=\"([^\"]+)\"");
        java.util.regex.Matcher matcher = pattern.matcher(description);

        while (matcher.find()) {
            usedImageUrls.add(matcher.group(1));
        }

        // Find and delete unused images
        for (ProductImage image : descriptionImages) {
            if (!usedImageUrls.contains(image.getImageUrl())) {
                try {
                    fileStorageService.deleteFile(image.getImageUrl());
                    productImageRepository.delete(image);
                    log.info("Deleted unused description image: {} for product ID: {}", image.getId(), productId);
                } catch (Exception e) {
                    log.error("Failed to delete unused description image: {}", image.getId(), e);
                }
            }
        }
    }

    // ── Helper Methods for Image Validation ───────────────────────────────────
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new CustomException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new CustomException("Only image files are allowed. Received: " + contentType);
        }

        // Max file size (5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new CustomException("File size must be less than 5MB");
        }

        // Check allowed image types
        List<String> allowedTypes = List.of("image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp");
        if (!allowedTypes.contains(contentType)) {
            throw new CustomException("Only JPG, PNG, GIF, and WEBP images are allowed");
        }
    }

    private String generateFileName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";

        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }

        return UUID.randomUUID().toString() + extension;
    }

    // ── Existing Methods ─────────────────────────────────────────────────────
    @Override
    @Transactional
    @CacheEvict(value = {"products", "activeProducts", "featuredProducts", "newArrivals", "topSelling"}, allEntries = true)
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        if (request.getSku() != null && !request.getSku().equals(product.getSku())) {
            productRepository.findBySku(request.getSku()).ifPresent(p -> {
                throw new CustomException("SKU already exists: " + request.getSku());
            });
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException(
                "Category not found with id: " + request.getCategoryId()));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);
        product.setSku(request.getSku());
        product.setUpc(request.getUpc());
        product.setDiscount(request.getDiscount() != null ? request.getDiscount() : BigDecimal.ZERO);
        product.setLabel(request.getLabel());
        if (request.getLazadaUrl() != null) {
            product.setLazadaUrl(request.getLazadaUrl());
        }
        if (request.getShopeeUrl() != null) {
            product.setShopeeUrl(request.getShopeeUrl());
        }
        if (request.getRecommendationCategoryId() != null) {
            product.setRecommendationCategory(
                    categoryRepository.findById(request.getRecommendationCategoryId()).orElse(null));
        } else {
            product.setRecommendationCategory(null);
        }
        if (request.getRecommendedProductIds() != null) {
            product.setRecommendedProducts(productRepository.findAllById(request.getRecommendedProductIds()));
        }
        if (request.getVariations() != null) {
            Map<Long, String> existingImagesByID = product.getVariations().stream()
                    .filter(v -> v.getId() != null && v.getImageUrl() != null)
                    .collect(Collectors.toMap(ProductVariation::getId, ProductVariation::getImageUrl));
            Map<String, String> existingImagesByName = product.getVariations().stream()
                    .filter(v -> v.getImageUrl() != null)
                    .collect(Collectors.toMap(ProductVariation::getName, ProductVariation::getImageUrl, (a, b) -> a));

            product.getVariations().clear();

            for (ProductVariationRequest varReq : request.getVariations()) {
                validateVariationUniquenessForUpdate(varReq.getSku(), varReq.getUpc(), product.getId());
                ProductVariation variation = mapToVariationEntity(varReq, product);
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
    @CacheEvict(value = {"products", "activeProducts", "featuredProducts", "newArrivals", "topSelling"}, allEntries = true)
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        // Delete all images (both gallery and description)
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
    @Transactional
    @CacheEvict(value = {"products", "activeProducts"}, allEntries = true)
    public ProductResponse updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (quantity < 0) {
            throw new CustomException("Stock quantity cannot be negative");
        }
        product.setStockQuantity(quantity);
        Product updatedProduct = productRepository.save(product);
        log.info("Stock updated for product: {} (ID: {}) to {}", product.getName(), id, quantity);
        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "activeProducts", "featuredProducts"}, allEntries = true)
    public ProductResponse toggleProductStatus(Long id, boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setActive(active);
        Product updatedProduct = productRepository.save(product);
        log.info("Product status updated: {} (ID: {}) to {}", product.getName(), id, active ? "active" : "inactive");
        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "activeProducts"}, allEntries = true)
    public ProductResponse updatePrice(Long id, BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CustomException("Price must be greater than 0");
        }
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setPrice(price);
        Product updatedProduct = productRepository.save(product);
        log.info("Price updated for product: {} (ID: {}) to {}", product.getName(), id, price);
        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "featuredProducts"}, allEntries = true)
    public ProductResponse toggleFeaturedStatus(Long id, boolean featured) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        product.setFeatured(featured);
        Product updatedProduct = productRepository.save(product);
        log.info("Featured status updated for product: {} (ID: {}) to {}", product.getName(), id, featured);
        return mapToResponse(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "activeProducts", "featuredProducts", "newArrivals", "topSelling"}, allEntries = true)
    public ProductResponse addProductImage(Long productId, MultipartFile file, boolean isPrimary, int displayOrder) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        try {
            String imageUrl = fileStorageService.uploadProductImage(file, productId);
            ProductImage productImage = ProductImage.builder()
                    .imageUrl(imageUrl)
                    .primary(isPrimary)
                    .displayOrder(displayOrder)
                    .imageType(ProductImage.ImageType.GALLERY)
                    .build();
            if (isPrimary) {
                product.getImages().stream()
                        .filter(img -> img.getImageType() == ProductImage.ImageType.GALLERY)
                        .forEach(img -> img.setPrimary(false));
            }
            product.addImage(productImage);
            if (product.getGalleryImages().size() == 1) {
                productImage.setPrimary(true);
                product.setImageUrl(imageUrl);
            }
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
    @CacheEvict(value = {"products", "activeProducts", "featuredProducts", "newArrivals", "topSelling"}, allEntries = true)
    public void deleteProductImage(Long productId, Long imageId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        ProductImage imageToDelete = product.getImages().stream()
                .filter(img -> img.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Product image not found"));

        try {
            fileStorageService.deleteFile(imageToDelete.getImageUrl());
        } catch (Exception e) {
            log.error("Failed to delete image file: {}", imageToDelete.getImageUrl(), e);
        }

        product.getImages().remove(imageToDelete);

        // Update primary image if needed
        if (imageToDelete.isPrimary() && !product.getGalleryImages().isEmpty()) {
            ProductImage newPrimary = product.getGalleryImages().get(0);
            newPrimary.setPrimary(true);
            product.setImageUrl(newPrimary.getImageUrl());
        } else if (product.getGalleryImages().isEmpty()) {
            product.setImageUrl(null);
        }

        productRepository.save(product);
        log.info("Image deleted from product: {} (ID: {})", product.getName(), productId);
    }

    @Override
    @Transactional
    @CacheEvict(value = {"products", "activeProducts", "featuredProducts", "newArrivals", "topSelling"}, allEntries = true)
    public List<ProductResponse.ProductImageResponse> addProductImages(Long productId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new CustomException("No files provided");
        }
        if (files.size() > 10) {
            throw new CustomException("Maximum 10 images allowed at once");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        int nextOrder = product.getGalleryImages().stream()
                .mapToInt(ProductImage::getDisplayOrder)
                .max().orElse(-1) + 1;

        boolean hasNoPrimary = product.getGalleryImages().isEmpty();
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
                        .imageType(ProductImage.ImageType.GALLERY)
                        .build();
                product.addImage(productImage);
                if (isPrimary) {
                    product.setImageUrl(imageUrl);
                }
                results.add(ProductResponse.ProductImageResponse.builder()
                        .id(productImage.getId())
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

    // Variation write methods
    @Override
    @Transactional
    @CacheEvict(value = {"products", "activeProducts"}, allEntries = true)
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
    @CacheEvict(value = {"products", "activeProducts"}, allEntries = true)
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
    @CacheEvict(value = {"products", "activeProducts"}, allEntries = true)
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
    @Transactional
    @CacheEvict(value = {"products", "activeProducts"}, allEntries = true)
    public ProductVariationResponse uploadVariationImage(Long productId, Long variationId, MultipartFile file) {
        ProductVariation variation = getVariationOrThrow(productId, variationId);
        try {
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
    @CacheEvict(value = {"products", "activeProducts"}, allEntries = true)
    public ProductVariationResponse updateVariationStock(Long productId, Long variationId, Integer quantity) {
        if (quantity < 0) {
            throw new CustomException("Stock quantity cannot be negative");
        }
        ProductVariation variation = getVariationOrThrow(productId, variationId);
        variation.setStockQuantity(quantity);
        ProductVariation saved = productVariationRepository.save(variation);
        log.info("Stock updated for variation ID: {} to {}", variationId, quantity);
        return mapToVariationResponse(saved);
    }

    // ── Read methods with caching ─────────────────────────────────────────────
    @Override
    @Cacheable(value = "products", key = "#id")
    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return mapToResponse(product);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long id) {
        productRepository.incrementViewCount(id);
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
            products = productRepository.findActiveProductsWithFilters(categoryId, null, null, search, null, null, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }
        return products.map(this::mapToResponse);
    }

    @Override
    @Cacheable(value = "activeProducts", key = "#categoryId + '-' + #search + '-' + #pageable.pageNumber + '-' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<ProductResponse> getActiveProducts(Pageable pageable, Long categoryId,
            BigDecimal minPrice, BigDecimal maxPrice, String search, Boolean inStock, Boolean onSale) {
        Page<Product> products = productRepository.findActiveProductsWithFilters(
                categoryId, minPrice, maxPrice, search, inStock, onSale, pageable);
        return products.map(this::mapToResponse);
    }

    @Override
    @Cacheable(value = "featuredProducts", key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getFeaturedProducts(int limit) {
        List<Product> products = productRepository.findByFeaturedTrueAndActiveTrue();
        return products.stream().limit(limit).map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "newArrivals", key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getNewArrivals(int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by("createdAt").descending());
        List<Product> products = productRepository.findNewArrivals(pageable);
        return products.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "topSelling", key = "#limit")
    @Transactional(readOnly = true)
    public List<ProductResponse> getTopSellingProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit,
                org.springframework.data.domain.Sort.by("soldCount").descending());
        List<Product> products = productRepository.findTopSellingProducts(pageable);
        return products.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductResponse> getProductsOnSale(Pageable pageable) {
        return productRepository.findProductsOnSale(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(Long productId, int limit) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        if (product.getCategory() == null) {
            return Collections.emptyList();
        }
        Pageable pageable = PageRequest.of(0, limit);
        List<Product> relatedProducts = productRepository.findRelatedProducts(
                product.getCategory().getId(), productId, pageable);
        return relatedProducts.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSearchSuggestions(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.findProductNameSuggestions(query.trim(), pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getLowStockProducts(int threshold) {
        return productRepository.findLowStockProducts(threshold).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Object getProductStats() {
        Long totalProducts = productRepository.count();
        Long activeProducts = productRepository.countActiveProducts();
        Long outOfStock = Long.valueOf(productRepository.findLowStockProducts(1).size());
        Long onSale = productRepository.findProductsOnSale(Pageable.unpaged()).getTotalElements();
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
        config.put("taxRate", 0.08);
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
        return productRepository.findByCategoryIdAndActiveTrue(categoryId, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }
        Pageable pageable = PageRequest.of(0, limit);
        return productRepository.searchProducts(query.trim(), pageable).stream()
                .map(this::mapToResponse).collect(Collectors.toList());
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

    // ── Private helpers ───────────────────────────────────────────────────────
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
                if (excludeId == null || !v.getId().equals(excludeId)) {
                    throw new CustomException("Variation SKU already exists: " + sku);
                }
            });
        }
        if (upc != null) {
            productVariationRepository.findByUpc(upc).ifPresent(v -> {
                if (excludeId == null || !v.getId().equals(excludeId)) {
                    throw new CustomException("Variation UPC already exists: " + upc);
                }
            });
        }
    }

    private void validateVariationUniquenessForUpdate(String sku, String upc, Long productId) {
        if (sku != null) {
            productVariationRepository.findBySku(sku).ifPresent(v -> {
                if (!v.getProduct().getId().equals(productId)) {
                    throw new CustomException("Variation SKU already exists: " + sku);
                }
            });
        }
        if (upc != null) {
            productVariationRepository.findByUpc(upc).ifPresent(v -> {
                if (!v.getProduct().getId().equals(productId)) {
                    throw new CustomException("Variation UPC already exists: " + upc);
                }
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

    private ProductResponse mapToResponse(Product product) {
        ProductResponse.ProductImageResponse[] primaryImage = {null};

        // Get gallery images only
        List<ProductResponse.ProductImageResponse> images = product.getGalleryImages().stream()
                .map(img -> {
                    ProductResponse.ProductImageResponse imageResponse = ProductResponse.ProductImageResponse.builder()
                            .id(img.getId())
                            .imageUrl(img.getImageUrl())
                            .isPrimary(img.isPrimary())
                            .displayOrder(img.getDisplayOrder())
                            .build();
                    if (img.isPrimary()) {
                        primaryImage[0] = imageResponse;
                    }
                    return imageResponse;
                }).collect(Collectors.toList());

        List<ProductVariationResponse> variationResponses = product.getVariations().stream()
                .map(this::mapToVariationResponse).collect(Collectors.toList());

        boolean hasVariations = !variationResponses.isEmpty();
        BigDecimal displayPrice, displayDiscountedPrice;
        Integer displayStock;
        boolean displayInStock;
        BigDecimal minPrice = null, maxPrice = null, minDiscountedPrice = null;

        if (hasVariations) {
            minPrice = variationResponses.stream().map(ProductVariationResponse::getPrice)
                    .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(product.getPrice());
            maxPrice = variationResponses.stream().map(ProductVariationResponse::getPrice)
                    .filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(product.getPrice());
            minDiscountedPrice = variationResponses.stream().map(ProductVariationResponse::getDiscountedPrice)
                    .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(minPrice);
            displayPrice = minPrice;
            displayDiscountedPrice = minDiscountedPrice;
            displayStock = variationResponses.stream().map(ProductVariationResponse::getStockQuantity)
                    .filter(Objects::nonNull).mapToInt(Integer::intValue).sum();
            displayInStock = variationResponses.stream().anyMatch(ProductVariationResponse::isInStock);
        } else {
            displayPrice = product.getPrice();
            displayDiscountedPrice = product.getDiscountedPrice();
            displayStock = product.getStockQuantity();
            displayInStock = product.isInStock();
        }

        Object reviewSummary = reviewRepository.getProductReviewSummary(product.getId());

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(displayPrice)
                .discountedPrice(displayDiscountedPrice)
                .hasVariations(hasVariations)
                .minPrice(minPrice)
                .maxPrice(maxPrice)
                .minDiscountedPrice(minDiscountedPrice)
                .stockQuantity(displayStock)
                .inStock(displayInStock)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .sku(product.getSku())
                .upc(product.getUpc())
                .imageUrl(product.getImageUrl())
                .images(images)
                .primaryImage(primaryImage[0])
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
                .lazadaUrl(product.getLazadaUrl())
                .shopeeUrl(product.getShopeeUrl())
                .addOns(product.getAddOns().stream().map(a -> {
                    Product ap = a.getAddOnProduct();
                    List<ProductVariation> activeVars = ap.getVariations().stream()
                            .filter(ProductVariation::isActive).collect(Collectors.toList());
                    boolean hasVars = !activeVars.isEmpty();
                    BigDecimal orig = hasVars
                            ? activeVars.stream()
                                    .map(v -> v.getDiscountedPrice() != null ? v.getDiscountedPrice() : v.getPrice())
                                    .filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(ap.getPrice())
                            : (ap.getDiscountedPrice() != null ? ap.getDiscountedPrice() : ap.getPrice());
                    BigDecimal spec = a.getSpecialPrice();
                    BigDecimal eff = (spec != null && spec.compareTo(orig) < 0) ? spec : orig;
                    int pct = 0;
                    if (spec != null && orig.compareTo(BigDecimal.ZERO) > 0) {
                        pct = orig.subtract(eff).multiply(BigDecimal.valueOf(100))
                                .divide(orig, 0, RoundingMode.HALF_UP).intValue();
                        if (pct < 0) {
                            pct = 0;
                        }
                    }
                    return ProductAddOnResponse.builder()
                            .id(a.getId())
                            .addOnProductId(ap.getId())
                            .addOnProductName(ap.getName())
                            .addOnProductImage(ap.getImageUrl())
                            .originalPrice(orig)
                            .specialPrice(spec)
                            .effectivePrice(eff)
                            .discountPercent(pct)
                            .inStock(hasVars ? activeVars.stream()
                                    .anyMatch(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0)
                                    : ap.isInStock())
                            .displayOrder(a.getDisplayOrder())
                            .hasVariations(hasVars)
                            .variations(activeVars.stream().map(this::mapToVariationResponse).collect(Collectors.toList()))
                            .build();
                }).collect(Collectors.toList()))
                .recommendedProducts(product.getRecommendedProducts().stream()
                        .map(this::mapToSummaryResponse).collect(Collectors.toList()))
                .recommendationCategoryId(product.getRecommendationCategory() != null
                        ? product.getRecommendationCategory().getId() : null)
                .recommendationCategoryName(product.getRecommendationCategory() != null
                        ? product.getRecommendationCategory().getName() : null)
                .build();
    }

    private ProductResponse.ProductSummaryResponse mapToSummaryResponse(Product p) {
        return ProductResponse.ProductSummaryResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .imageUrl(p.getImageUrl())
                .price(p.getPrice())
                .discountedPrice(p.getDiscountedPrice())
                .inStock(p.isInStock())
                .rating(p.getRating())
                .label(p.getLabel())
                .build();
    }

    @Override
    public List<ProductImage> getDescriptionImages(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }
        return productImageRepository.findByProductIdAndImageType(productId, ProductImage.ImageType.DESCRIPTION);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DescriptionImageResponse> getDescriptionImageResponses(Long productId) {
        // Verify product exists
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found with id: " + productId);
        }

        // Get description images from repository
        List<ProductImage> descriptionImages = productImageRepository
                .findByProductIdAndImageType(productId, ProductImage.ImageType.DESCRIPTION);

        // Convert to response DTOs
        return descriptionImages.stream()
                .map(image -> DescriptionImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .altText(image.getAltText())
                .fileName(extractFileName(image.getImageUrl()))
                .build())
                .collect(Collectors.toList());
    }

    private String extractFileName(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return null;
        }
        int lastSlash = imageUrl.lastIndexOf('/');
        return lastSlash >= 0 ? imageUrl.substring(lastSlash + 1) : imageUrl;
    }
}
