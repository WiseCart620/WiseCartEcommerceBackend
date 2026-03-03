package com.wisecartecommerce.ecommerce.controller.admin;

import com.wisecartecommerce.ecommerce.Dto.Request.TestimonialRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.TestimonialResponse;
import com.wisecartecommerce.ecommerce.service.impl.TestimonialServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/admin/testimonials")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTestimonialController {

    private final TestimonialServiceImpl testimonialService;
    private final ObjectMapper objectMapper;

    @GetMapping
    public ResponseEntity<ApiResponse<List<TestimonialResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Testimonials retrieved", testimonialService.getAll()));
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<TestimonialResponse>> create(
            @RequestPart("testimonial") String json,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar) throws Exception {
        TestimonialRequest req = objectMapper.readValue(json, TestimonialRequest.class);
        return ResponseEntity.ok(ApiResponse.success("Testimonial created", testimonialService.create(req, avatar)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TestimonialResponse>> update(
            @PathVariable Long id, @RequestBody TestimonialRequest req) {
        return ResponseEntity.ok(ApiResponse.success("Testimonial updated", testimonialService.update(id, req)));
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<TestimonialResponse>> uploadAvatar(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded", testimonialService.uploadAvatar(id, file)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<TestimonialResponse>> toggleStatus(
            @PathVariable Long id, @RequestParam boolean active) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", testimonialService.toggleActive(id, active)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        testimonialService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Testimonial deleted", null));
    }
}