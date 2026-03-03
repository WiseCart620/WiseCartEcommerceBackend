package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.Dto.Request.TestimonialRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.TestimonialResponse;
import com.wisecartecommerce.ecommerce.entity.Testimonial;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.TestimonialRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class TestimonialServiceImpl {

    private final TestimonialRepository testimonialRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<TestimonialResponse> getAll() {
        return testimonialRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TestimonialResponse> getActive() {
        return testimonialRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public TestimonialResponse create(TestimonialRequest req, MultipartFile avatar) {
        Testimonial t = Testimonial.builder()
                .customerName(req.getCustomerName())
                .customerTitle(req.getCustomerTitle())
                .review(req.getReview())
                .rating(req.getRating() != null ? req.getRating() : 5)
                .productName(req.getProductName())
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .active(req.isActive())
                .build();
        Testimonial saved = testimonialRepository.save(t);
        if (avatar != null && !avatar.isEmpty()) {
            try {
                String url = fileStorageService.uploadProductImage(avatar, saved.getId());
                saved.setAvatarUrl(url);
                saved = testimonialRepository.save(saved);
            } catch (Exception e) { log.error("Avatar upload failed", e); }
        }
        return toResponse(saved);
    }

    @Transactional
    public TestimonialResponse update(Long id, TestimonialRequest req) {
        Testimonial t = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found: " + id));
        t.setCustomerName(req.getCustomerName());
        t.setCustomerTitle(req.getCustomerTitle());
        t.setReview(req.getReview());
        if (req.getRating() != null) t.setRating(req.getRating());
        t.setProductName(req.getProductName());
        if (req.getDisplayOrder() != null) t.setDisplayOrder(req.getDisplayOrder());
        t.setActive(req.isActive());
        return toResponse(testimonialRepository.save(t));
    }

    @Transactional
    public TestimonialResponse uploadAvatar(Long id, MultipartFile file) {
        Testimonial t = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found: " + id));
        try {
            if (t.getAvatarUrl() != null) fileStorageService.deleteFile(t.getAvatarUrl());
            t.setAvatarUrl(fileStorageService.uploadProductImage(file, id));
        } catch (Exception e) { throw new RuntimeException("Avatar upload failed: " + e.getMessage()); }
        return toResponse(testimonialRepository.save(t));
    }

    @Transactional
    public void delete(Long id) {
        Testimonial t = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found: " + id));
        if (t.getAvatarUrl() != null) {
            try { fileStorageService.deleteFile(t.getAvatarUrl()); } catch (Exception ignored) {}
        }
        testimonialRepository.delete(t);
    }

    @Transactional
    public TestimonialResponse toggleActive(Long id, boolean active) {
        Testimonial t = testimonialRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Testimonial not found: " + id));
        t.setActive(active);
        return toResponse(testimonialRepository.save(t));
    }

    private TestimonialResponse toResponse(Testimonial t) {
        return TestimonialResponse.builder()
                .id(t.getId()).customerName(t.getCustomerName()).customerTitle(t.getCustomerTitle())
                .avatarUrl(t.getAvatarUrl()).review(t.getReview()).rating(t.getRating())
                .productName(t.getProductName()).displayOrder(t.getDisplayOrder())
                .active(t.isActive()).createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .build();
    }
}