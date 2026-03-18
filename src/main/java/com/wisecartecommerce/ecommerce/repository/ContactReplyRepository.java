package com.wisecartecommerce.ecommerce.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.ContactReply;
import com.wisecartecommerce.ecommerce.entity.ContactReply.SenderType;

@Repository
public interface ContactReplyRepository extends JpaRepository<ContactReply, Long> {

    // Count unread ADMIN replies for a specific message — used for customer notification badge
    @Query("""
        SELECT COUNT(r) FROM ContactReply r
        WHERE r.contactMessage.id = :messageId
          AND r.senderType = :senderType
          AND r.isRead = false
        """)
    long countUnreadBySenderType(@Param("messageId") Long messageId, @Param("senderType") SenderType senderType);

    // Total unread admin replies across ALL of a user's threads — navbar badge
    @Query("""
    SELECT COUNT(r) FROM ContactReply r
    WHERE r.contactMessage.userId = :userId
      AND r.senderType = :senderType
      AND r.isRead = false
    """)
    long countUnreadAdminRepliesForUser(@Param("userId") Long userId,
            @Param("senderType") SenderType senderType);

    @Modifying
    @Query("""
    UPDATE ContactReply r SET r.isRead = true
    WHERE r.contactMessage.id = :messageId
      AND r.senderType = :senderType
      AND r.isRead = false
    """)
    int markAdminRepliesAsRead(@Param("messageId") Long messageId,
            @Param("senderType") SenderType senderType);
}
