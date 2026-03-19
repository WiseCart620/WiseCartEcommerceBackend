package com.wisecartecommerce.ecommerce.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.ReviewRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ReviewResponse;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.Review;
import com.wisecartecommerce.ecommerce.entity.ReviewImage;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.repository.ReviewImageRepository;
import com.wisecartecommerce.ecommerce.repository.ReviewRepository;
import com.wisecartecommerce.ecommerce.repository.UserRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import com.wisecartecommerce.ecommerce.service.ReviewService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private static final int MAX_IMAGES_PER_REVIEW = 5;

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    // ── Private helpers ───────────────────────────────────────────────────────
    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            throw new CustomException("User not authenticated");
        }
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ReviewResponse mapToResponse(Review review) {
        String userName = review.getUser() != null
                ? review.getUser().getFirstName() + " " + review.getUser().getLastName()
                : "Anonymous";

        List<String> imageUrls = review.getImages() == null ? List.of()
                : review.getImages().stream()
                        .map(ReviewImage::getImageUrl)
                        .collect(Collectors.toList());

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct() != null ? review.getProduct().getId() : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userName(userName)
                .rating(review.getRating())
                .comment(review.getComment())
                .helpfulCount(review.getHelpfulCount())
                .verifiedPurchase(review.isVerifiedPurchase())
                .active(review.isActive())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .imageUrls(imageUrls)
                .build();
    }

    private void updateProductRating(Product product) {
        if (product == null) return;
        List<Review> reviews = reviewRepository.findByProductId(product.getId());
        double avg = reviews.stream()
                .filter(Review::isActive)
                .mapToInt(Review::getRating)
                .average().orElse(0.0);
        long count = reviews.stream().filter(Review::isActive).count();
        product.setRating(java.math.BigDecimal.valueOf(avg)
                .setScale(2, java.math.RoundingMode.HALF_UP));
        product.setReviewCount((int) count);
        productRepository.save(product);
    }

    private void attachImages(Review review, List<MultipartFile> files) {
        if (CollectionUtils.isEmpty(files)) return;

        List<MultipartFile> valid = files.stream()
                .filter(f -> f != null && !f.isEmpty())
                .collect(Collectors.toList());

        if (valid.size() > MAX_IMAGES_PER_REVIEW) {
            throw new CustomException("Maximum " + MAX_IMAGES_PER_REVIEW + " photos allowed per review");
        }

        for (int i = 0; i < valid.size(); i++) {
            try {
                String url = fileStorageService.uploadFile(valid.get(i), "reviews");
                ReviewImage img = ReviewImage.builder()
                        .review(review)
                        .imageUrl(url)
                        .displayOrder(i)
                        .build();
                review.getImages().add(img);
            } catch (java.io.IOException e) {
                log.error("Failed to upload review image at index {}: {}", i, e.getMessage());
                throw new CustomException("Failed to upload photo " + (i + 1) + ": " + e.getMessage());
            }
        }
    }

    private void deleteImages(Review review) {
        if (review.getImages() == null) return;
        review.getImages().forEach(img -> fileStorageService.deleteFile(img.getImageUrl()));
        review.getImages().clear();
    }

    // ── Create ────────────────────────────────────────────────────────────────
    // FIX: use allEntries=true so the composite cache key is fully cleared.
    // The read key is "#productId + '_' + pageNumber + '_' + pageSize" which
    // cannot be matched with a single key= expression on evict.
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "reviews",       allEntries = true),
        @CacheEvict(value = "reviewSummary", allEntries = true),
        @CacheEvict(value = "recentReviews", allEntries = true)
    })
    public ReviewResponse createReview(Long productId, ReviewRequest request) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        reviewRepository.findByUserIdAndProductId(user.getId(), productId).ifPresent(r -> {
            throw new CustomException("You have already reviewed this product");
        });

        Review review = Review.builder()
                .user(user)
                .product(product)
                .rating(request.getRating())
                .comment(request.getComment())
                .helpfulCount(0)
                .verifiedPurchase(false)
                .active(true)
                .build();

        attachImages(review, request.getImages());

        Review saved = reviewRepository.save(review);
        updateProductRating(product);
        log.info("Review created for product {} by user {}", productId, user.getId());
        return mapToResponse(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "reviews",       allEntries = true),
        @CacheEvict(value = "reviewSummary", allEntries = true),
        @CacheEvict(value = "recentReviews", allEntries = true)
    })
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());

        if (!CollectionUtils.isEmpty(request.getImages())) {
            deleteImages(review);
            attachImages(review, request.getImages());
        }

        Review saved = reviewRepository.save(review);
        updateProductRating(review.getProduct());
        return mapToResponse(saved);
    }

    // ── Delete (customer) ─────────────────────────────────────────────────────
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "reviews",       allEntries = true),
        @CacheEvict(value = "reviewSummary", allEntries = true),
        @CacheEvict(value = "recentReviews", allEntries = true)
    })
    public void deleteReview(Long reviewId) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only delete your own reviews");
        }

        Product product = review.getProduct();
        deleteImages(review);
        reviewRepository.delete(review);
        updateProductRating(product);
        log.info("Review {} deleted by user {}", reviewId, user.getId());
    }

    // ── Admin hard-delete ─────────────────────────────────────────────────────
    @Override
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "reviews",       allEntries = true),
        @CacheEvict(value = "reviewSummary", allEntries = true),
        @CacheEvict(value = "recentReviews", allEntries = true)
    })
    public void adminDeleteReview(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        Product product = review.getProduct();
        deleteImages(review);
        reviewRepository.delete(review);
        updateProductRating(product);
        log.info("Review {} hard-deleted by admin", reviewId);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long id) {
        return mapToResponse(reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id)));
    }

    // ── Get product reviews (cached) ──────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviews", key = "#productId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable,
            Integer minRating, Integer maxRating) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
        return reviewRepository
                .findProductReviewsWithFilters(productId, minRating, maxRating, pageable)
                .map(this::mapToResponse);
    }

    // ── Get user reviews ──────────────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    public List<ReviewResponse> getUserReviews() {
        User user = getCurrentUser();
        return reviewRepository.findByUserId(user.getId())
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── Review summary (cached) ───────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "reviewSummary", key = "#productId")
    public Object getProductReviewSummary(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }
        Object raw = reviewRepository.getProductReviewSummary(productId);
        Map<String, Object> summary = new HashMap<>();
        if (raw instanceof Object[] row) {
            summary.put("averageRating", row[0] != null ? row[0] : 0.0);
            summary.put("totalReviews",  row[1] != null ? row[1] : 0L);
            Map<Integer, Object> counts = new HashMap<>();
            counts.put(5, row[2] != null ? row[2] : 0L);
            counts.put(4, row[3] != null ? row[3] : 0L);
            counts.put(3, row[4] != null ? row[4] : 0L);
            counts.put(2, row[5] != null ? row[5] : 0L);
            counts.put(1, row[6] != null ? row[6] : 0L);
            summary.put("ratingCounts", counts);
        } else {
            summary.put("averageRating", 0.0);
            summary.put("totalReviews",  0L);
            summary.put("ratingCounts",  Map.of(5, 0, 4, 0, 3, 0, 2, 0, 1, 0));
        }
        return summary;
    }

    // ── Recent reviews (cached) ───────────────────────────────────────────────
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "recentReviews", key = "#limit")
    public List<ReviewResponse> getRecentReviews(int limit) {
        return reviewRepository.findRecentReviews(PageRequest.of(0, limit))
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // ── Mark helpful ──────────────────────────────────────────────────────────
    @Override
    @Transactional
    public ReviewResponse markHelpful(Long reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        review.setHelpfulCount(review.getHelpfulCount() + 1);
        return mapToResponse(reviewRepository.save(review));
    }

    // ── Report review ─────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void reportReview(Long reviewId, String reason) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        review.setReported(true);
        review.setReportReason(reason);
        reviewRepository.save(review);
        log.info("Review {} reported: {}", reviewId, reason);
    }
}