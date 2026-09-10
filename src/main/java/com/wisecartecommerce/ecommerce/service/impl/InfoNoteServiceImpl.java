package com.wisecartecommerce.ecommerce.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.InfoNoteRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.InfoNoteResponse;
import com.wisecartecommerce.ecommerce.entity.InfoNote;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.InfoNoteRepository;
import com.wisecartecommerce.ecommerce.service.FileStorageService;
import com.wisecartecommerce.ecommerce.service.InfoNoteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service

@RequiredArgsConstructor
@Slf4j
public class InfoNoteServiceImpl implements InfoNoteService {

    private final InfoNoteRepository repository;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<InfoNoteResponse> getAll() {
        return repository.findAllByOrderByDisplayOrderAsc().stream()
                .map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @CacheEvict(value = "infoNoteResolveAll", allEntries = true)
    public InfoNoteResponse create(InfoNoteRequest request) {
        InfoNote note = InfoNote.builder()
                .note(request.getNote())
                .iconType(request.getIconType() != null ? request.getIconType() : "PRESET")
                .iconKey(request.getIconKey() != null ? request.getIconKey() : "FiTruck")
                .iconUrl(request.getIconUrl())
                .appliesToAll(request.isAppliesToAll())
                .active(request.isActive())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .productIds(request.isAppliesToAll() || request.getProductIds() == null
                        ? new HashSet<>()
                        : new HashSet<>(request.getProductIds()))
                .build();
        InfoNote saved = repository.save(note);
        log.info("Info note created: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "infoNoteResolveAll", allEntries = true)
    public InfoNoteResponse update(Long id, InfoNoteRequest request) {
        InfoNote note = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Info note not found: " + id));
        note.setNote(request.getNote());
        if (request.getIconType() != null) {
            note.setIconType(request.getIconType());
        }
        if (request.getIconKey() != null) {
            note.setIconKey(request.getIconKey());
        }
        note.setAppliesToAll(request.isAppliesToAll());
        note.setActive(request.isActive());
        if (request.getDisplayOrder() != null) {
            note.setDisplayOrder(request.getDisplayOrder());
        }
        note.setProductIds(request.isAppliesToAll() || request.getProductIds() == null
                ? new HashSet<>()
                : new HashSet<>(request.getProductIds()));
        InfoNote saved = repository.save(note);
        log.info("Info note updated: {}", id);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = "infoNoteResolveAll", allEntries = true)
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new ResourceNotFoundException("Info note not found: " + id);
        }
        repository.deleteById(id);
        log.info("Info note deleted: {}", id);
    }

    @Override
    @Transactional
    @CacheEvict(value = "infoNoteResolveAll", allEntries = true)
    public InfoNoteResponse uploadIcon(Long id, MultipartFile file) {
        InfoNote note = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Info note not found: " + id));
        try {
            String url = fileStorageService.uploadFile(file, "info-note-icons");
            note.setIconType("CUSTOM");
            note.setIconUrl(url);
            InfoNote saved = repository.save(note);
            return mapToResponse(saved);
        } catch (Exception e) {
            log.error("Failed to upload info note icon", e);
            throw new RuntimeException("Failed to upload icon: " + e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public InfoNoteResponse resolveForProduct(Long productId) {
        return resolveAllForProduct(productId).stream().findFirst().orElse(null);
    }

    @Override
    @Cacheable(value = "infoNoteResolveAll", key = "#productId")
    @Transactional(readOnly = true)
    public List<InfoNoteResponse> resolveAllForProduct(Long productId) {
        List<InfoNote> result = new ArrayList<>();
        if (productId != null) {
            List<InfoNote> specific = repository.findByActiveTrueAndAppliesToAllFalseOrderByDisplayOrderAsc();
            specific.stream()
                    .filter(n -> n.getProductIds() != null && n.getProductIds().contains(productId))
                    .forEach(result::add);
        }

        if (result.isEmpty()) {
            result.addAll(repository.findByActiveTrueAndAppliesToAllTrueOrderByDisplayOrderAsc());
        }
        return result.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private InfoNoteResponse mapToResponse(InfoNote n) {
        return InfoNoteResponse.builder()
                .id(n.getId())
                .note(n.getNote())
                .iconType(n.getIconType())
                .iconKey(n.getIconKey())
                .iconUrl(n.getIconUrl())
                .appliesToAll(n.isAppliesToAll())
                .active(n.isActive())
                .displayOrder(n.getDisplayOrder())
                .productIds(n.getProductIds() == null ? new ArrayList<>() : new ArrayList<>(n.getProductIds()))
                .updatedAt(n.getUpdatedAt())
                .build();
    }
}
