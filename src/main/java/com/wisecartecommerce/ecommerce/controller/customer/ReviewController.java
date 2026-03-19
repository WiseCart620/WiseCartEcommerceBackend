package com.wisecartecommerce.ecommerce.controller.customer;

import com.wisecartecommerce.ecommerce.Dto.Request.ReviewRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ReviewResponse;
import com.wisecartecommerce.ecommerce.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Product review APIs")
public class ReviewController {

    private final ReviewService reviewService;

    // ── Public endpoints ──────────────────────────────────────────────────────

    @GetMapping("/product/{productId}")
    @Operation(summary = "Get reviews for a product")
    public ResponseEntity<ApiResponse<Page<ReviewResponse>>> getProductReviews(
            @PathVariable Long productId,
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "10")   int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir,
            @RequestParam(required = false) Integer minRating,
            @RequestParam(required = false) Integer maxRating) {

        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved",
                reviewService.getProductReviews(productId, pageable, minRating, maxRating)));
    }

    @GetMapping("/product/{productId}/summary")
    @Operation(summary = "Get review summary (avg rating + counts)")
    public ResponseEntity<ApiResponse<Object>> getProductReviewSummary(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Review summary retrieved",
                reviewService.getProductReviewSummary(productId)));
    }

    @GetMapping("/recent")
    @Operation(summary = "Get recent reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getRecentReviews(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Recent reviews retrieved",
                reviewService.getRecentReviews(limit)));
    }

    
    @PostMapping(value = "/product/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create review with optional photos (max 5)")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long productId,
            @RequestParam                       Integer rating,
            @RequestParam                       String  comment,
            @RequestParam(required = false)     List<MultipartFile> images) {

        ReviewRequest request = ReviewRequest.builder()
                .rating(rating)
                .comment(comment)
                .images(images)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Review created",
                reviewService.createReview(productId, request)));
    }

    /**
     * PUT /reviews/{reviewId}
     *
     * Same multipart approach — send new images to replace existing ones,
     * or omit the images field to keep the current photos unchanged.
     */
    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update review (optionally replace photos)")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @RequestParam                       Integer rating,
            @RequestParam                       String  comment,
            @RequestParam(required = false)     List<MultipartFile> images) {

        ReviewRequest request = ReviewRequest.builder()
                .rating(rating)
                .comment(comment)
                .images(images)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Review updated",
                reviewService.updateReview(reviewId, request)));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete own review")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long reviewId) {
        reviewService.deleteReview(reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    @GetMapping("/user")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current user's reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getUserReviews() {
        return ResponseEntity.ok(ApiResponse.success("User reviews retrieved",
                reviewService.getUserReviews()));
    }

    @PostMapping("/{reviewId}/helpful")
    @PreAuthorize("hasRole('CUSTOMER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Mark review as helpful")
    public ResponseEntity<ApiResponse<ReviewResponse>> markHelpful(@PathVariable Long reviewId) {
        return ResponseEntity.ok(ApiResponse.success("Review marked as helpful",
                reviewService.markHelpful(reviewId)));
    }

    @PostMapping("/{reviewId}/report")
    @PreAuthorize("isAuthenticated()")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Report a review")
    public ResponseEntity<ApiResponse<Void>> reportReview(
            @PathVariable Long reviewId,
            @RequestParam  String reason) {
        reviewService.reportReview(reviewId, reason);
        return ResponseEntity.ok(ApiResponse.success("Review reported", null));
    }
}