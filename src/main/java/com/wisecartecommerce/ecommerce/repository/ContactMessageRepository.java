package com.wisecartecommerce.ecommerce.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.ContactMessage;
import com.wisecartecommerce.ecommerce.entity.ContactMessage.ContactStatus;

@Repository
public interface ContactMessageRepository extends JpaRepository<ContactMessage, Long> {
    Page<ContactMessage> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<ContactMessage> findByStatusOrderByCreatedAtDesc(ContactStatus status, Pageable pageable);

    // Customer: their own threads
    List<ContactMessage> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ContactMessage> findByIdAndUserId(Long id, Long userId);

    // Cleanup: delete RESOLVED messages older than X days
    @Modifying
    @Query("DELETE FROM ContactMessage cm WHERE cm.status = 'RESOLVED' AND cm.updatedAt < :cutoff")
    int deleteResolvedBefore(@Param("cutoff") LocalDateTime cutoff);

    // Admin: count open messages (for dashboard badge)
    long countByStatus(ContactStatus status);
}