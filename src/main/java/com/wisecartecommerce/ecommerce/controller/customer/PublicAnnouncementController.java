package com.wisecartecommerce.ecommerce.controller.customer;


import com.wisecartecommerce.ecommerce.Dto.Response.AnnouncementBannerDTO;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.service.AnnouncementBannerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class PublicAnnouncementController {

    private final AnnouncementBannerService service;

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<AnnouncementBannerDTO>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success("OK", service.getActive()));
    }
}