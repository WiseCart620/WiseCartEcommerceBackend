package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.Dto.Request.ReviewRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ReviewResponse;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.Review;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.repository.ReviewRepository;
import com.wisecartecommerce.ecommerce.repository.UserRepository;
import com.wisecartecommerce.ecommerce.service.ReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

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
                .build();
    }

    private void updateProductRating(Product product) {
        if (product == null) return;
        List<Review> reviews = reviewRepository.findByProductId(product.getId());
        double avg = reviews.stream()
                .filter(Review::isActive)
                .mapToInt(Review::getRating)
                .average()
                .orElse(0.0);
        long count = reviews.stream().filter(Review::isActive).count();
        product.setRating(java.math.BigDecimal.valueOf(avg)
                .setScale(2, java.math.RoundingMode.HALF_UP));
        product.setReviewCount((int) count);
        productRepository.save(product);
    }

    // ── Create ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
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

        Review saved = reviewRepository.save(review);
        updateProductRating(product);
        log.info("Review created for product {} by user {}", productId, user.getId());
        return mapToResponse(saved);
    }

    // ── Update ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse updateReview(Long reviewId, ReviewRequest request) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only edit your own reviews");
        }

        review.setRating(request.getRating());
        review.setComment(request.getComment());
        Review saved = reviewRepository.save(review);
        updateProductRating(review.getProduct());
        return mapToResponse(saved);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteReview(Long reviewId) {
        User user = getCurrentUser();
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        boolean isAdmin = user.getRole().name().equals("ADMIN");
        if (!isAdmin && !review.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only delete your own reviews");
        }

        Product product = review.getProduct();
        reviewRepository.delete(review);
        updateProductRating(product);
        log.info("Review {} deleted", reviewId);
    }

    // ── Get by ID ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReviewResponse getReviewById(Long id) {
        return mapToResponse(reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + id)));
    }

    // ── Get product reviews ───────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
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

    // ── Review summary ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Object getProductReviewSummary(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw new ResourceNotFoundException("Product not found: " + productId);
        }

        Object raw = reviewRepository.getProductReviewSummary(productId);

        // The JPQL aggregate query returns Object[] — unpack it into a readable map
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
            summary.put("totalReviews", 0L);
            summary.put("ratingCounts", Map.of(5,0,4,0,3,0,2,0,1,0));
        }
        return summary;
    }

    // ── Recent reviews ────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
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