package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.Dto.Response.AnnouncementBannerDTO;
import com.wisecartecommerce.ecommerce.entity.AnnouncementBanner;
import com.wisecartecommerce.ecommerce.repository.AnnouncementBannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnouncementBannerService {

    private final AnnouncementBannerRepository repo;

    public List<AnnouncementBannerDTO> getAll() {
        return repo.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<AnnouncementBannerDTO> getActive() {
        return repo.findByActiveTrueOrderByDisplayOrderAsc()
                   .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public AnnouncementBannerDTO create(AnnouncementBannerDTO dto) {
        AnnouncementBanner entity = AnnouncementBanner.builder()
            .text(dto.getText())
            .link(dto.getLink())
            .bgColor(dto.getBgColor())
            .textColor(dto.getTextColor())
            .active(dto.getActive() != null ? dto.getActive() : true)
            .displayOrder(dto.getDisplayOrder() != null ? dto.getDisplayOrder() : 0)
            .build();
        return toDTO(repo.save(entity));
    }

    public AnnouncementBannerDTO update(Long id, AnnouncementBannerDTO dto) {
        AnnouncementBanner entity = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Announcement not found"));
        if (dto.getText()         != null) entity.setText(dto.getText());
        if (dto.getLink()         != null) entity.setLink(dto.getLink());
        if (dto.getBgColor()      != null) entity.setBgColor(dto.getBgColor());
        if (dto.getTextColor()    != null) entity.setTextColor(dto.getTextColor());
        if (dto.getActive()       != null) entity.setActive(dto.getActive());
        if (dto.getDisplayOrder() != null) entity.setDisplayOrder(dto.getDisplayOrder());
        return toDTO(repo.save(entity));
    }

    public void toggleStatus(Long id, boolean active) {
        AnnouncementBanner entity = repo.findById(id)
            .orElseThrow(() -> new RuntimeException("Announcement not found"));
        entity.setActive(active);
        repo.save(entity);
    }

    public void delete(Long id) { repo.deleteById(id); }

    private AnnouncementBannerDTO toDTO(AnnouncementBanner e) {
        AnnouncementBannerDTO dto = new AnnouncementBannerDTO();
        dto.setId(e.getId());
        dto.setText(e.getText());
        dto.setLink(e.getLink());
        dto.setBgColor(e.getBgColor());
        dto.setTextColor(e.getTextColor());
        dto.setActive(e.getActive());
        dto.setDisplayOrder(e.getDisplayOrder());
        return dto;
    }
}