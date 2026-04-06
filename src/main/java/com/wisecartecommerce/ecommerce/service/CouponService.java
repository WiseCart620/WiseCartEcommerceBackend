package com.wisecartecommerce.ecommerce.service;

import java.util.HashSet;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.CouponRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.CouponResponse;
import com.wisecartecommerce.ecommerce.entity.Coupon;
import com.wisecartecommerce.ecommerce.repository.CouponRepository;
import com.wisecartecommerce.ecommerce.repository.CouponUsageRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    public Page<CouponResponse> getAllCoupons(Pageable pageable, String search) {
        Page<Coupon> coupons;
        if (search != null && !search.isBlank()) {
            coupons = couponRepository.findByCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, pageable);
        } else {
            coupons = couponRepository.findAll(pageable);
        }
        return coupons.map(this::toResponse);
    }

    public CouponResponse getCouponById(Long id) {
        return toResponse(findById(id));
    }

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        if (couponRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("Coupon code already exists: " + request.getCode());
        }
        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .type(request.getType())
                .discountValue(request.getDiscountValue())
                .minimumPurchaseAmount(request.getMinimumPurchaseAmount())
                .maximumDiscountAmount(request.getMaximumDiscountAmount())
                .maxUsageCount(request.getMaxUsageCount())
                .maxUsagePerUser(request.getMaxUsagePerUser())
                .minimumProductQuantity(request.getMinimumProductQuantity() != null
                        ? request.getMinimumProductQuantity() : 0)
                .startDate(request.getStartDate())
                .expirationDate(request.getExpirationDate())
                .isActive(request.getActive() != null ? request.getActive() : true)
                .applicableProducts(request.getApplicableProducts() != null ? request.getApplicableProducts() : new java.util.HashSet<>())
                .applicableCategories(request.getApplicableCategories() != null ? request.getApplicableCategories() : new java.util.HashSet<>())
                .isCombinable(request.getCombinable() != null ? request.getCombinable() : false)
                .combinableWith(request.getCombinableWith() != null ? request.getCombinableWith() : new HashSet<>())
                .build();
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = findById(id);
        coupon.setDescription(request.getDescription());
        coupon.setType(request.getType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinimumPurchaseAmount(request.getMinimumPurchaseAmount());
        coupon.setMaximumDiscountAmount(request.getMaximumDiscountAmount());
        coupon.setMaxUsageCount(request.getMaxUsageCount());
        coupon.setMaxUsagePerUser(request.getMaxUsagePerUser());
        coupon.setIsCombinable(request.getCombinable() != null ? request.getCombinable() : false);
        if (request.getCombinableWith() != null) {
            coupon.setCombinableWith(request.getCombinableWith());
        }
        if (request.getMinimumProductQuantity() != null) {
            coupon.setMinimumProductQuantity(request.getMinimumProductQuantity());
        }
        coupon.setStartDate(request.getStartDate());
        coupon.setExpirationDate(request.getExpirationDate());
        if (request.getActive() != null) {
            coupon.setIsActive(request.getActive());
        }
        if (request.getApplicableProducts() != null) {
            coupon.setApplicableProducts(request.getApplicableProducts());
        }
        if (request.getApplicableCategories() != null) {
            coupon.setApplicableCategories(request.getApplicableCategories());
        }
        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public void deleteCoupon(Long id) {
        couponRepository.delete(findById(id));
    }

    @Transactional
    public CouponResponse toggleStatus(Long id, boolean active) {
        Coupon coupon = findById(id);
        coupon.setIsActive(active);
        return toResponse(couponRepository.save(coupon));
    }

    public Object getCouponStats(Long id) {
        Coupon coupon = findById(id);
        var usages = couponUsageRepository.findByCouponId(id);
        return Map.of(
                "totalUsage", coupon.getCurrentUsageCount(),
                "maxUsage", coupon.getMaxUsageCount() != null ? coupon.getMaxUsageCount() : "Unlimited",
                "uniqueUsers", usages.stream().map(u -> u.getUser().getId()).distinct().count()
        );
    }

    private Coupon findById(Long id) {
        return couponRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coupon not found with id: " + id));
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .type(coupon.getType())
                .discountValue(coupon.getDiscountValue())
                .minimumPurchaseAmount(coupon.getMinimumPurchaseAmount())
                .maximumDiscountAmount(coupon.getMaximumDiscountAmount())
                .maxUsageCount(coupon.getMaxUsageCount())
                .currentUsageCount(coupon.getCurrentUsageCount())
                .maxUsagePerUser(coupon.getMaxUsagePerUser())
                .minimumProductQuantity(coupon.getMinimumProductQuantity())
                .startDate(coupon.getStartDate())
                .expirationDate(coupon.getExpirationDate())
                .active(coupon.getIsActive())
                .combinable(coupon.getIsCombinable())
                .combinableWith(coupon.getCombinableWith())
                .applicableProducts(coupon.getApplicableProducts())
                .applicableCategories(coupon.getApplicableCategories())
                .createdAt(coupon.getCreatedAt())
                .updatedAt(coupon.getUpdatedAt())
                .build();
    }
}
