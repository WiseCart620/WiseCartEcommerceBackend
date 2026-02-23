package com.wisecartecommerce.ecommerce.controller.customer;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.wisecartecommerce.ecommerce.Dto.Request.ReviewRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ReviewResponse;
import com.wisecartecommerce.ecommerce.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews for product")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating) {
        
        Sort sort = sortDir.equalsIgnoreCase("asc") 
            ? Sort.by(sortBy).ascending() 
            : Sort.by(sortBy).descending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<ReviewResponse> reviews = reviewService.getProductReviews(
            productId, pageable, minRating, maxRating);
        
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved", reviews));
    }

    @GetMapping("/product/{productId}/summary")
    @Operation(summary = "Get review summary for product")
    public ResponseEntity<ApiResponse<Object>> getProductReviewSummary(@PathVariable Long productId) {
        Object summary = reviewService.getProductReviewSummary(productId);
        return ResponseEntity.ok(ApiResponse.success("Review summary retrieved", summary));
    }

    @PostMapping("/product/{productId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create review")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long productId,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(productId, request);
        return ResponseEntity.ok(ApiResponse.success("Review created", response));
    }

    @PutMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update review")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.updateReview(reviewId, request);
        return ResponseEntity.ok(ApiResponse.success("Review updated", response));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get user's reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getUserReviews() {
        List<ReviewResponse> reviews = reviewService.getUserReviews();
        return ResponseEntity.ok(ApiResponse.success("User reviews retrieved", reviews));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getRecentReviews(
            @RequestParam(defaultValue = "10") int limit) {
        List<ReviewResponse> reviews = reviewService.getRecentReviews(limit);
        return ResponseEntity.ok(ApiResponse.success("Recent reviews retrieved", reviews));
    }

    @PostMapping("/{reviewId}/helpful")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark review as helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> markHelpful(@PathVariable Long reviewId) {
        ReviewResponse response = reviewService.markHelpful(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review marked as helpful", response));
    }

    @PostMapping("/{reviewId}/report")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Report review")
    public ResponseEntity<ApiResponse<Void>> reportReview(
            @PathVariable Long reviewId,
            @RequestParam String reason) {
        reviewService.reportReview(reviewId, reason);
        return ResponseEntity.ok(ApiResponse.success("Review reported", null));
    }
}