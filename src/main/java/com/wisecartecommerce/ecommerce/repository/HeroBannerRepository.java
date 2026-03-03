package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.HeroBanner;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HeroBannerRepository extends JpaRepository<HeroBanner, Long> {
    List<HeroBanner> findAllByOrderByDisplayOrderAsc();
    List<HeroBanner> findByActiveTrueOrderByDisplayOrderAsc();
}