package com.wisecartecommerce.ecommerce.controller.admin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.entity.Category;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.ProductAddOn;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import com.wisecartecommerce.ecommerce.repository.CategoryRepository;
import com.wisecartecommerce.ecommerce.repository.ProductAddOnRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import java.util.Comparator;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/products/{productId}")
@RequiredArgsConstructor
public class AdminProductAddOnController {

    private final ProductAddOnRepository addOnRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // ── Add-ons ────────────────────────────────────────────────────────────

    @GetMapping("/addons")
    public ResponseEntity<?> getAddOns(@PathVariable Long productId) {
        List<Map<String, Object>> result = addOnRepository
                .findByProductIdOrderByDisplayOrderAsc(productId)
                .stream()
                .map(a -> {
                    Product ap = a.getAddOnProduct();
                    List<ProductVariation> activeVariations = ap.getVariations().stream()
                            .filter(ProductVariation::isActive)
                            .collect(Collectors.toList());
                    boolean hasVariations = !activeVariations.isEmpty();
                    BigDecimal orig = hasVariations
                            ? activeVariations.stream()
                                    .map(v -> v.getDiscountedPrice() != null ? v.getDiscountedPrice() : v.getPrice())
                                    .min(Comparator.naturalOrder())
                                    .orElse(ap.getPrice())
                            : (ap.getDiscountedPrice() != null ? ap.getDiscountedPrice() : ap.getPrice());

                    // Always use base product image — customer picking variation will update it in
                    // UI
                    String displayImage = ap.getImageUrl();

                    BigDecimal spec = a.getSpecialPrice();
                    BigDecimal eff = (spec != null) ? spec : orig;
                    int pct = 0;
                    if (spec != null && orig.compareTo(BigDecimal.ZERO) > 0) {
                        pct = orig.subtract(eff)
                                .multiply(BigDecimal.valueOf(100))
                                .divide(orig, 0, RoundingMode.HALF_UP)
                                .intValue();
                        if (pct < 0)
                            pct = 0;
                    }

                    List<Map<String, Object>> variations = activeVariations.stream()
                            .map(v -> {
                                Map<String, Object> vm = new LinkedHashMap<>();
                                vm.put("id", v.getId());
                                vm.put("name", v.getName());
                                vm.put("sku", v.getSku());
                                vm.put("price", v.getPrice());
                                vm.put("discountedPrice", v.getDiscountedPrice());
                                vm.put("stockQuantity", v.getStockQuantity());
                                vm.put("imageUrl", v.getImageUrl());
                                vm.put("inStock", v.getStockQuantity() != null && v.getStockQuantity() > 0);
                                return (Map<String, Object>) vm;
                            })
                            .collect(Collectors.toList());

                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("addOnProductId", ap.getId());
                    m.put("addOnProductName", ap.getName());
                    m.put("addOnProductImage", displayImage);
                    m.put("originalPrice", orig);
                    m.put("specialPrice", spec);
                    m.put("effectivePrice", eff);
                    m.put("discountPercent", pct);
                    m.put("inStock", hasVariations
                            ? activeVariations.stream()
                                    .anyMatch(v -> v.getStockQuantity() != null && v.getStockQuantity() > 0)
                            : ap.isInStock());
                    m.put("displayOrder", a.getDisplayOrder());
                    m.put("hasVariations", hasVariations);
                    m.put("variations", variations);
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success("Add-ons retrieved", result));
    }



    @PostMapping("/addons")
    public ResponseEntity<?> addAddOn(@PathVariable Long productId,
            @RequestBody AddOnRequest req) {
        if (productId.equals(req.getAddOnProductId()))
            return ResponseEntity.badRequest().body("Cannot add a product as its own add-on");
        if (addOnRepository.existsByProductIdAndAddOnProductId(productId, req.getAddOnProductId()))
            return ResponseEntity.badRequest().body("Add-on already exists");

        Product product = productRepository.findById(productId).orElseThrow();
        Product addOnProd = productRepository.findById(req.getAddOnProductId()).orElseThrow();

        ProductAddOn addOn = new ProductAddOn();
        addOn.setProduct(product);
        addOn.setAddOnProduct(addOnProd);
        addOn.setSpecialPrice(req.getSpecialPrice());
        addOn.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);

        return ResponseEntity.ok(addOnRepository.save(addOn));
    }

    @PatchMapping("/addons/{addOnId}")
    public ResponseEntity<?> updateAddOn(@PathVariable Long productId,
            @PathVariable Long addOnId,
            @RequestBody AddOnRequest req) {
        ProductAddOn addOn = addOnRepository.findById(addOnId).orElseThrow();
        if (req.getSpecialPrice() != null)
            addOn.setSpecialPrice(req.getSpecialPrice());
        if (req.getDisplayOrder() != null)
            addOn.setDisplayOrder(req.getDisplayOrder());
        return ResponseEntity.ok(addOnRepository.save(addOn));
    }

    @DeleteMapping("/addons/{addOnId}")
    public ResponseEntity<?> deleteAddOn(@PathVariable Long productId,
            @PathVariable Long addOnId) {
        addOnRepository.deleteByProductIdAndId(productId, addOnId);
        return ResponseEntity.noContent().build();
    }

    // ── Recommendations ────────────────────────────────────────────────────

    @PatchMapping("/recommendations")
    public ResponseEntity<?> updateRecommendations(@PathVariable Long productId,
            @RequestBody RecommendationRequest req) {
        Product product = productRepository.findById(productId).orElseThrow();

        if ("category".equals(req.getMode())) {
            Category cat = categoryRepository.findById(req.getCategoryId()).orElse(null);
            product.setRecommendationCategory(cat);
            product.setRecommendedProducts(new ArrayList<>());
        } else {
            product.setRecommendationCategory(null);
            if (req.getProductIds() != null) {
                product.setRecommendedProducts(productRepository.findAllById(req.getProductIds()));
            }
        }
        productRepository.save(product);
        return ResponseEntity.ok().build();
    }

    // ── Marketplace links ──────────────────────────────────────────────────

    @PatchMapping("/marketplace-links")
    public ResponseEntity<?> updateMarketplaceLinks(@PathVariable Long productId,
            @RequestBody MarketplaceLinksRequest req) {
        Product product = productRepository.findById(productId).orElseThrow();
        product.setLazadaUrl(req.getLazadaUrl());
        product.setShopeeUrl(req.getShopeeUrl());
        productRepository.save(product);
        return ResponseEntity.ok().build();
    }

    // ── Request DTOs ───────────────────────────────────────────────────────

    @Data
    static class AddOnRequest {
        private Long addOnProductId;
        private BigDecimal specialPrice;
        private Integer displayOrder;
    }

    @Data
    static class RecommendationRequest {
        private String mode;
        private Long categoryId;
        private List<Long> productIds;
    }

    @Data
    static class MarketplaceLinksRequest {
        private String lazadaUrl;
        private String shopeeUrl;
    }
}