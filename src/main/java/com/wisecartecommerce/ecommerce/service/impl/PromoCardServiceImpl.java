package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.Dto.Request.PromoCardRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.PromoCardResponse;
import com.wisecartecommerce.ecommerce.entity.PromoCard;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.PromoCardRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PromoCardServiceImpl {

    private final PromoCardRepository promoCardRepository;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<PromoCardResponse> getAll() {
        return promoCardRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PromoCardResponse> getActive() {
        return promoCardRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PromoCardResponse create(PromoCardRequest req, MultipartFile image) {
        PromoCard card = PromoCard.builder()
                .title(req.getTitle())
                .subtitle(req.getSubtitle())
                .description(req.getDescription())
                .buttonText(req.getButtonText() != null ? req.getButtonText() : "Shop Now")
                .link(req.getLink() != null ? req.getLink() : "/products")
                .color(req.getColor() != null ? req.getColor() : "from-orange-500 to-red-500")
                .displayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0)
                .overlayOpacity(req.getOverlayOpacity() != null ? req.getOverlayOpacity() : 70)
                .active(req.isActive())
                .build();

        PromoCard saved = promoCardRepository.save(card);

        if (image != null && !image.isEmpty()) {
            try {
                String url = fileStorageService.uploadProductImage(image, saved.getId());
                saved.setImageUrl(url);
                saved = promoCardRepository.save(saved);
            } catch (Exception e) {
                log.error("Failed to upload promo card image", e);
            }
        }
        return toResponse(saved);
    }

    @Transactional
    public PromoCardResponse update(Long id, PromoCardRequest req) {
        PromoCard card = promoCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo card not found: " + id));

        card.setTitle(req.getTitle());
        card.setSubtitle(req.getSubtitle());
        card.setDescription(req.getDescription());
        if (req.getButtonText() != null) card.setButtonText(req.getButtonText());
        if (req.getLink() != null) card.setLink(req.getLink());
        if (req.getColor() != null) card.setColor(req.getColor());
        if (req.getDisplayOrder() != null) card.setDisplayOrder(req.getDisplayOrder());
        if (req.getOverlayOpacity() != null) card.setOverlayOpacity(req.getOverlayOpacity());
        card.setActive(req.isActive());

        return toResponse(promoCardRepository.save(card));
    }

    @Transactional
    public PromoCardResponse uploadImage(Long id, MultipartFile file) {
        PromoCard card = promoCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo card not found: " + id));
        try {
            if (card.getImageUrl() != null) fileStorageService.deleteFile(card.getImageUrl());
            String url = fileStorageService.uploadProductImage(file, id);
            card.setImageUrl(url);
        } catch (Exception e) {
            log.error("Promo card image upload failed", e);
            throw new RuntimeException("Image upload failed: " + e.getMessage());
        }
        return toResponse(promoCardRepository.save(card));
    }

    @Transactional
    public void delete(Long id) {
        PromoCard card = promoCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo card not found: " + id));
        if (card.getImageUrl() != null) {
            try { fileStorageService.deleteFile(card.getImageUrl()); } catch (Exception ignored) {}
        }
        promoCardRepository.delete(card);
    }

    @Transactional
    public PromoCardResponse toggleActive(Long id, boolean active) {
        PromoCard card = promoCardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Promo card not found: " + id));
        card.setActive(active);
        return toResponse(promoCardRepository.save(card));
    }

    private PromoCardResponse toResponse(PromoCard c) {
        return PromoCardResponse.builder()
                .id(c.getId())
                .title(c.getTitle())
                .subtitle(c.getSubtitle())
                .description(c.getDescription())
                .buttonText(c.getButtonText())
                .link(c.getLink())
                .imageUrl(c.getImageUrl())
                .color(c.getColor())
                .displayOrder(c.getDisplayOrder())
                .active(c.isActive())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .overlayOpacity(c.getOverlayOpacity())
                .build();
    }
}