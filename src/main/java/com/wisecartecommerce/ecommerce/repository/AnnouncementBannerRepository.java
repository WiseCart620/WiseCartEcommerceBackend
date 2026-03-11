package com.wisecartecommerce.ecommerce.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.wisecartecommerce.ecommerce.entity.AnnouncementBanner;

import java.util.List;

public interface AnnouncementBannerRepository extends JpaRepository<AnnouncementBanner, Long> {
    List<AnnouncementBanner> findByActiveTrueOrderByDisplayOrderAsc();
}

