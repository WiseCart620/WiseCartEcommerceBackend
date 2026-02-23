package com.wisecartecommerce.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wisecartecommerce.ecommerce.Dto.Request.ReviewRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ReviewResponse;

import java.util.List;

public interface ReviewService {
    ReviewResponse createReview(Long productId, ReviewRequest request);
    ReviewResponse updateReview(Long reviewId, ReviewRequest request);
    void deleteReview(Long reviewId);
    ReviewResponse getReviewById(Long id);
    Page<ReviewResponse> getProductReviews(Long productId, Pageable pageable, Integer minRating, Integer maxRating);
    List<ReviewResponse> getUserReviews();
    Object getProductReviewSummary(Long productId);
    List<ReviewResponse> getRecentReviews(int limit);
    ReviewResponse markHelpful(Long reviewId);
    void reportReview(Long reviewId, String reason);
}