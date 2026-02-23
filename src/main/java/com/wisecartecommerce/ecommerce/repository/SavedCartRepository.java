// SavedCartRepository.java
package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.SavedCart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedCartRepository extends JpaRepository<SavedCart, Long> {
    
    List<SavedCart> findByUserId(Long userId);
    
    @Query("SELECT sc FROM SavedCart sc LEFT JOIN FETCH sc.items WHERE sc.id = :id AND sc.user.id = :userId")
    Optional<SavedCart> findByIdAndUserIdWithItems(@Param("id") Long id, @Param("userId") Long userId);
    
    boolean existsByUserIdAndName(Long userId, String name);
}