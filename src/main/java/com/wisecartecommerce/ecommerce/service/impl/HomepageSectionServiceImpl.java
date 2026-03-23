package com.wisecartecommerce.ecommerce.service.impl;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.HomepageSectionRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.HomepageSectionResponse;
import com.wisecartecommerce.ecommerce.entity.Category;
import com.wisecartecommerce.ecommerce.entity.HomepageSectionConfig;
import com.wisecartecommerce.ecommerce.entity.HomepageSectionProduct;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.enums.SectionMode;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.CategoryRepository;
import com.wisecartecommerce.ecommerce.repository.HomepageSectionProductRepository;
import com.wisecartecommerce.ecommerce.repository.HomepageSectionRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import com.wisecartecommerce.ecommerce.service.HomepageSectionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class HomepageSectionServiceImpl implements HomepageSectionService {

    private final HomepageSectionRepository sectionRepository;
    private final HomepageSectionProductRepository sectionProductRepository;
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageService fileStorageService;

    private static final List<String[]> DEFAULT_SECTIONS = List.of(
            new String[]{"FEATURED", "Featured Products", "Handpicked just for you"},
            new String[]{"HOT_DEALS", "Hot Deals", "Limited time discounts"},
            new String[]{"NEW_ARRIVALS", "New Arrivals", "Fresh from the collection"},
            new String[]{"BEST_SELLERS", "Best Sellers", "Most popular products"},
            new String[]{"COMING_SOON", "Coming Soon", "Launching shortly — stay tuned"});

    private static final Set<String> PROTECTED_KEYS = Set.of(
            "FEATURED", "HOT_DEALS", "NEW_ARRIVALS", "BEST_SELLERS", "COMING_SOON");

    @Override
    @CacheEvict(value = "homepageSections", allEntries = true)
    public HomepageSectionResponse createSection(HomepageSectionRequest request) {
        String key = request.getSectionKey();
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("sectionKey is required");
        }
        // Normalise: upper-snake
        key = key.trim().toUpperCase().replaceAll("[^A-Z0-9]+", "_");

        if (sectionRepository.findBySectionKey(key).isPresent()) {
            throw new IllegalArgumentException("A section with key '" + key + "' already exists");
        }

        long maxOrder = sectionRepository.findAllByOrderByDisplayOrderAsc()
                .stream().mapToLong(HomepageSectionConfig::getDisplayOrder).max().orElse(-1L);

        HomepageSectionConfig s = HomepageSectionConfig.builder()
                .sectionKey(key)
                .title(request.getTitle() != null ? request.getTitle() : key)
                .subtitle(request.getSubtitle() != null ? request.getSubtitle() : "")
                .mode(request.getMode() != null ? request.getMode() : SectionMode.AUTO)
                .limit(request.getLimit() != null ? request.getLimit() : 8)
                .active(request.isActive())
                .displayOrder((int) (maxOrder + 1))
                .build();

        return mapToResponse(sectionRepository.save(s));
    }

    @Override
    @CacheEvict(value = "homepageSections", allEntries = true)
    public String uploadBannerImage(String sectionKey, MultipartFile file) {
        HomepageSectionConfig section = sectionRepository.findBySectionKey(sectionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionKey));
        try {
            String url = fileStorageService.uploadFile(file, "section-banners");
            section.setSectionBannerUrl(url);
            sectionRepository.save(section);
            log.info("Uploaded banner image for section {}: {}", sectionKey, url);
            return url;
        } catch (java.io.IOException e) {
            log.error("Failed to upload banner image for section {}", sectionKey, e);
            throw new RuntimeException("Failed to upload banner image: " + e.getMessage());
        }
    }

    @Override
    @CacheEvict(value = "homepageSections", allEntries = true)
    public void deleteSection(String sectionKey) {
        if (PROTECTED_KEYS.contains(sectionKey.toUpperCase())) {
            throw new IllegalArgumentException("Default sections cannot be deleted");
        }
        HomepageSectionConfig s = sectionRepository.findBySectionKey(sectionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionKey));
        sectionProductRepository.deleteAllBySectionId(s.getId());
        sectionRepository.delete(s);
        log.info("Deleted custom homepage section: {}", sectionKey);
    }

    @Override
    @Transactional(readOnly = true)
    public List<HomepageSectionResponse> getAllSections() {
        return sectionRepository.findAllByOrderByDisplayOrderAsc()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Cacheable("homepageSections")
    @Transactional(readOnly = true)
    public List<HomepageSectionResponse> getActiveSections() {
        return sectionRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream().map(this::mapToResponseWithProducts).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public HomepageSectionResponse getSectionByKey(String sectionKey) {
        HomepageSectionConfig section = sectionRepository.findBySectionKey(sectionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionKey));
        return mapToResponseWithProducts(section);
    }

    @Override
    @CacheEvict(value = "homepageSections", allEntries = true)
    public HomepageSectionResponse updateSection(String sectionKey, HomepageSectionRequest request) {
        log.info("Updating homepage section: {}", sectionKey);

        HomepageSectionConfig section = sectionRepository.findBySectionKey(sectionKey)
                .orElseThrow(() -> new ResourceNotFoundException("Section not found: " + sectionKey));

        section.setTitle(request.getTitle());
        section.setSubtitle(request.getSubtitle());
        section.setMode(request.getMode());
        section.setActive(request.isActive());
        section.setDisplayOrder(
                request.getDisplayOrder() != null ? request.getDisplayOrder() : section.getDisplayOrder());
        section.setLimit(request.getLimit() != null ? request.getLimit() : section.getLimit());
        section.setShowTimer(request.isShowTimer());
        section.setTimerEndsAt(request.getTimerEndsAt());
        section.setTimerLabel(request.getTimerLabel());
        section.setSectionBannerUrl(request.getSectionBannerUrl());
        section.setSectionBannerLink(request.getSectionBannerLink());
        section.setSectionBannerTitle(request.getSectionBannerTitle());
        section.setSectionBannerDescription(request.getSectionBannerDescription());
        section.setSectionBannerOverlay(request.getSectionBannerOverlay() != null ? request.getSectionBannerOverlay() : 40);

        // Handle category mode
        if (request.getMode() == SectionMode.CATEGORY && request.getCategoryId() != null) {
            Category cat = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found"));
            section.setCategory(cat);
        } else if (request.getMode() != SectionMode.CATEGORY) {
            section.setCategory(null);
        }

        // Handle manual product selection — replace all
        sectionProductRepository.deleteAllBySectionId(section.getId());
        sectionProductRepository.flush();
        section.getSectionProducts().clear();

        if (request.getMode() == SectionMode.MANUAL && request.getProductIds() != null) {
            int order = 0;
            for (Long productId : request.getProductIds()) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));
                HomepageSectionProduct sp = HomepageSectionProduct.builder()
                        .section(section)
                        .product(product)
                        .displayOrder(order++)
                        .build();
                section.getSectionProducts().add(sp);
            }
        }

        HomepageSectionConfig saved = sectionRepository.save(section);
        return mapToResponse(saved);
    }

    @Override
    public void initializeDefaultSections() {
        int order = 0;
        for (String[] def : DEFAULT_SECTIONS) {
            if (sectionRepository.findBySectionKey(def[0]).isEmpty()) {
                HomepageSectionConfig s = HomepageSectionConfig.builder()
                        .sectionKey(def[0])
                        .title(def[1])
                        .subtitle(def[2])
                        .mode(SectionMode.AUTO)
                        .limit(8)
                        .active(true)
                        .displayOrder(order)
                        .build();
                sectionRepository.save(s);
                log.info("Initialized default homepage section: {}", def[0]);
            }
            order++;
        }
    }

    // ── Mapping helpers ───────────────────────────────────────────────────────
    private HomepageSectionResponse mapToResponse(HomepageSectionConfig s) {
        List<Long> productIds = s.getSectionProducts().stream()
                .map(sp -> sp.getProduct().getId())
                .collect(Collectors.toList());

        return HomepageSectionResponse.builder()
                .id(s.getId())
                .sectionKey(s.getSectionKey())
                .title(s.getTitle())
                .subtitle(s.getSubtitle())
                .mode(s.getMode())
                .categoryId(s.getCategory() != null ? s.getCategory().getId() : null)
                .categoryName(s.getCategory() != null ? s.getCategory().getName() : null)
                .limit(s.getLimit())
                .active(s.isActive())
                .displayOrder(s.getDisplayOrder())
                .productIds(productIds)
                .updatedAt(s.getUpdatedAt())
                .showTimer(s.isShowTimer())
                .timerEndsAt(s.getTimerEndsAt())
                .timerLabel(s.getTimerLabel())
                .sectionBannerUrl(s.getSectionBannerUrl())
                .sectionBannerLink(s.getSectionBannerLink())
                .sectionBannerTitle(s.getSectionBannerTitle())
                .sectionBannerDescription(s.getSectionBannerDescription())
                .timerRemainingMs(
                        s.isShowTimer() && s.getTimerEndsAt() != null
                        ? Math.max(0L, java.time.Duration.between(
                                java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                                s.getTimerEndsAt()).toMillis())
                        : null)
                .build();
    }


    private HomepageSectionResponse mapToResponseWithProducts(HomepageSectionConfig s) {
        // Products are resolved by the frontend via existing product endpoints for AUTO
        // mode.
        // For MANUAL and CATEGORY we just return the config; the controller/service
        // can hydrate products if needed — keeping this lightweight.
        return mapToResponse(s);
    }
}
