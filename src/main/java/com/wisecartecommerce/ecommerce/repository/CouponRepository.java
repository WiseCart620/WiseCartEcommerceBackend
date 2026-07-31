// CouponRepository.java
package com.wisecartecommerce.ecommerce.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.Coupon;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    Optional<Coupon> findByCodeAndIsActiveTrue(String code);

    boolean existsByCode(String code);

    List<Coupon> findByIsActiveTrueAndIsAutomaticTrue();

    Page<Coupon> findByCodeContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String code, String description, Pageable pageable);
}