package com.wisecartecommerce.ecommerce.controller.admin;


import com.wisecartecommerce.ecommerce.Dto.Response.AnnouncementBannerDTO;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.service.AnnouncementBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/announcements")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnnouncementController {

    private final AnnouncementBannerService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AnnouncementBannerDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AnnouncementBannerDTO>> create(
            @RequestBody AnnouncementBannerDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Created", service.create(dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AnnouncementBannerDTO>> update(
            @PathVariable Long id, @RequestBody AnnouncementBannerDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Updated", service.update(id, dto)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> toggle(
            @PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        service.toggleStatus(id, Boolean.TRUE.equals(body.get("active")));
        return ResponseEntity.ok(ApiResponse.success("Updated", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Deleted", null));
    }
}