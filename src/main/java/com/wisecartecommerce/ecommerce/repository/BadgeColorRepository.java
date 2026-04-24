package com.wisecartecommerce.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.BadgeColor;

@Repository
public interface BadgeColorRepository extends JpaRepository<BadgeColor, Long> {
    Optional<BadgeColor> findByBadgeName(String badgeName);
    List<BadgeColor> findAllByOrderByDisplayOrderAsc();
    List<BadgeColor> findByActiveTrueOrderByDisplayOrderAsc();
}