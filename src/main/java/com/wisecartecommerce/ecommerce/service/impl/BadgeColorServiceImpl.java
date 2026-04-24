package com.wisecartecommerce.ecommerce.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.BadgeColorRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.BadgeColorResponse;
import com.wisecartecommerce.ecommerce.entity.BadgeColor;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.BadgeColorRepository;
import com.wisecartecommerce.ecommerce.service.BadgeColorService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BadgeColorServiceImpl implements BadgeColorService {

    private final BadgeColorRepository badgeColorRepository;

    @Override
    @Cacheable(value = "badgeColors", key = "'all'")
    public List<BadgeColorResponse> getAllBadgeColors() {
        return badgeColorRepository.findAllByOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "badgeColors", key = "'active'")
    public List<BadgeColorResponse> getActiveBadgeColors() {
        return badgeColorRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "badgeColors", allEntries = true)
    public BadgeColorResponse createOrUpdateBadgeColor(BadgeColorRequest request) {
        BadgeColor badgeColor;

        if (request.getId() != null && badgeColorRepository.existsById(request.getId())) {
            badgeColor = badgeColorRepository.findById(request.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Badge color not found"));
        } else {
            // Check by name before creating new
            badgeColor = badgeColorRepository.findByBadgeName(request.getBadgeName())
                    .orElse(BadgeColor.builder().build());
        }

        badgeColor.setBadgeName(request.getBadgeName());
        badgeColor.setColorClass(request.getColorClass());
        badgeColor.setActive(request.isActive());
        badgeColor.setDisplayOrder(request.getDisplayOrder());

        return toResponse(badgeColorRepository.save(badgeColor));
    }

    @Override
    @Transactional
    @CacheEvict(value = "badgeColors", allEntries = true)
    public void deleteBadgeColor(Long id) {
        badgeColorRepository.deleteById(id);
        log.info("Deleted badge color with id: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "badgeColors", allEntries = true)
    public BadgeColorResponse updateBadgeColor(Long id, BadgeColorRequest request) {
        BadgeColor badgeColor = badgeColorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Badge color not found"));

        badgeColor.setBadgeName(request.getBadgeName());
        badgeColor.setColorClass(request.getColorClass());
        badgeColor.setActive(request.isActive());
        badgeColor.setDisplayOrder(request.getDisplayOrder());

        return toResponse(badgeColorRepository.save(badgeColor));
    }

    private BadgeColorResponse toResponse(BadgeColor entity) {
        return BadgeColorResponse.builder()
                .id(entity.getId())
                .badgeName(entity.getBadgeName())
                .colorClass(entity.getColorClass())
                .active(entity.isActive())
                .displayOrder(entity.getDisplayOrder())
                .build();
    }
}
