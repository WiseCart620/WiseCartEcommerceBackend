package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.Dto.Request.HeroBannerRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.HeroBannerResponse;
import com.wisecartecommerce.ecommerce.entity.HeroBanner;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.HeroBannerRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Slf4j
public class HeroBannerServiceImpl {

    private final HeroBannerRepository bannerRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<HeroBannerResponse> getAllBanners() {
        return bannerRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HeroBannerResponse> getActiveBanners() {
        return bannerRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public HeroBannerResponse create(HeroBannerRequest req, MultipartFile image) {
        HeroBanner banner = HeroBanner.builder()
                .title(req.getTitle())
                .badge(req.getBadge())
                .subtitle(req.getSubtitle())
                .buttonText(req.getButtonText() != null ? req.getButtonText() : "Shop Now")
                .buttonLink(req.getButtonLink() != null ? req.getButtonLink() : "/products")
                .textColor(req.getTextColor() != null ? req.getTextColor() : "light")
                .overlayOpacity(req.getOverlayOpacity() != null ? req.getOverlayOpacity() : 40)
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .active(req.isActive())
                .build();

        HeroBanner saved = bannerRepository.save(banner);

        if (image != null && !image.isEmpty()) {
            try {
                // Reuse product image upload — stores in same bucket/folder
                String url = fileStorageService.uploadProductImage(image, saved.getId());
                saved.setImageUrl(url);
                saved = bannerRepository.save(saved);
            } catch (Exception e) {
                log.error("Failed to upload banner image", e);
            }
        }
        return toResponse(saved);
    }

    @Transactional
    public HeroBannerResponse update(Long id, HeroBannerRequest req) {
        HeroBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found: " + id));
        banner.setTitle(req.getTitle());
        banner.setBadge(req.getBadge());
        banner.setSubtitle(req.getSubtitle());
        if (req.getButtonText() != null) banner.setButtonText(req.getButtonText());
        if (req.getButtonLink() != null) banner.setButtonLink(req.getButtonLink());
        if (req.getTextColor() != null) banner.setTextColor(req.getTextColor());
        if (req.getOverlayOpacity() != null) banner.setOverlayOpacity(req.getOverlayOpacity());
        if (req.getDisplayOrder() != null) banner.setDisplayOrder(req.getDisplayOrder());
        banner.setActive(req.isActive());
        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public HeroBannerResponse uploadImage(Long id, MultipartFile file) {
        HeroBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found: " + id));
        try {
            if (banner.getImageUrl() != null) fileStorageService.deleteFile(banner.getImageUrl());
            String url = fileStorageService.uploadProductImage(file, id);
            banner.setImageUrl(url);
        } catch (Exception e) {
            log.error("Banner image upload failed", e);
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
        return toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public void delete(Long id) {
        HeroBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found: " + id));
        if (banner.getImageUrl() != null) {
            try { fileStorageService.deleteFile(banner.getImageUrl()); } catch (Exception ignored) {}
        }
        bannerRepository.delete(banner);
    }

    @Transactional
    public HeroBannerResponse toggleActive(Long id, boolean active) {
        HeroBanner banner = bannerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Banner not found: " + id));
        banner.setActive(active);
        return toResponse(bannerRepository.save(banner));
    }

    private HeroBannerResponse toResponse(HeroBanner b) {
        return HeroBannerResponse.builder()
                .id(b.getId()).title(b.getTitle()).badge(b.getBadge())
                .subtitle(b.getSubtitle()).buttonText(b.getButtonText())
                .buttonLink(b.getButtonLink()).imageUrl(b.getImageUrl())
                .textColor(b.getTextColor()).overlayOpacity(b.getOverlayOpacity())
                .displayOrder(b.getDisplayOrder()).active(b.isActive())
                .createdAt(b.getCreatedAt()).updatedAt(b.getUpdatedAt())
                .build();
    }
}