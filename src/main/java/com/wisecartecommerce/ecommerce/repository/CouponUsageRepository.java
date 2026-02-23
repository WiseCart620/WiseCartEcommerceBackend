// CouponUsageRepository.java
package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.CouponUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CouponUsageRepository extends JpaRepository<CouponUsage, Long> {
    
    Integer countByUserIdAndCouponId(Long userId, Long couponId);
    
    List<CouponUsage> findByUserId(Long userId);
    
    List<CouponUsage> findByCouponId(Long couponId);
}