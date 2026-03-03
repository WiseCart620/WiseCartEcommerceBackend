package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.HomepageSectionProduct;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

@Repository
public interface HomepageSectionProductRepository extends JpaRepository<HomepageSectionProduct, Long> {
    @Modifying
    @Transactional
    void deleteAllBySectionId(Long sectionId);
}