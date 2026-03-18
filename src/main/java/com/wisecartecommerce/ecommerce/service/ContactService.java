package com.wisecartecommerce.ecommerce.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.ContactReplyRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.ContactRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ContactMessageResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ContactReplyResponse;
import com.wisecartecommerce.ecommerce.entity.ContactMessage;
import com.wisecartecommerce.ecommerce.entity.ContactMessage.ContactStatus;
import com.wisecartecommerce.ecommerce.entity.ContactReply;
import com.wisecartecommerce.ecommerce.entity.ContactReply.SenderType;
import com.wisecartecommerce.ecommerce.repository.ContactMessageRepository;
import com.wisecartecommerce.ecommerce.repository.ContactReplyRepository;
import com.wisecartecommerce.ecommerce.repository.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContactService {

    private final ContactMessageRepository messageRepository;
    private final ContactReplyRepository replyRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    // ─── PUBLIC ──────────────────────────────────────────────────────────────
    @Transactional
    public ContactMessageResponse submitMessage(ContactRequest request, Long userId) {
        ContactMessage toSave = ContactMessage.builder()
                .userId(userId)
                .name(request.getName())
                .email(request.getEmail())
                .subject(request.getSubject())
                .message(request.getMessage())
                .status(ContactStatus.OPEN)
                .build();

        ContactMessage saved = messageRepository.save(toSave);
        emailService.sendContactEmail(request);

        log.info("Contact message #{} submitted by {} (userId={})", saved.getId(), request.getEmail(), userId);
        return toResponse(saved, false);
    }

    // ─── CUSTOMER ────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public List<ContactMessageResponse> getMyMessages(Long userId) {
        return messageRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(m -> toResponse(m, false))
                .toList();
    }

    @Transactional
    public ContactMessageResponse getMyMessageThread(Long messageId, Long userId) {
        ContactMessage message = messageRepository.findByIdAndUserId(messageId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        int marked = replyRepository.markAdminRepliesAsRead(messageId, SenderType.ADMIN);
        if (marked > 0) {
            log.debug("Marked {} admin replies as read for message #{}", marked, messageId);
        }

        return toResponse(message, true);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return replyRepository.countUnreadAdminRepliesForUser(userId, SenderType.ADMIN);
    }

    // ─── ADMIN ───────────────────────────────────────────────────────────────
    @Transactional(readOnly = true)
    public Page<ContactMessageResponse> getAllMessages(ContactStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<ContactMessage> messages = (status != null)
                ? messageRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : messageRepository.findAllByOrderByCreatedAtDesc(pageable);
        return messages.map(m -> toResponse(m, false));
    }


    @Transactional
    public ContactReplyResponse adminReply(Long messageId, ContactReplyRequest request) {
        ContactMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message #" + messageId + " not found"));

        ContactReply toSave = ContactReply.builder()
                .contactMessage(message)
                .senderType(SenderType.ADMIN)
                .message(request.getMessage())
                .isRead(false)
                .build();

        ContactReply reply = replyRepository.save(toSave);

        // Bump status to IN_PROGRESS if still OPEN
        if (message.getStatus() == ContactStatus.OPEN) {
            message.setStatus(ContactStatus.IN_PROGRESS);
            messageRepository.save(message);
        }

        // ── Create in-app notification for logged-in customer ──
        if (message.getUserId() != null) {
            userRepository.findById(message.getUserId()).ifPresent(user
                    -> notificationService.createNotification(
                            user,
                            "Support Reply — Ticket #" + String.format("%06d", messageId),
                            "WiseCart support replied to your message: \"" + message.getSubject() + "\"",
                            "SUPPORT",
                            messageId,
                            "CONTACT_MESSAGE"
                    )
            );
        }

        // ── Email the customer (async, non-blocking) ──
        emailService.sendAdminReplyNotification(
                message.getEmail(),
                message.getName(),
                message.getSubject(),
                request.getMessage()
        );

        log.info("Admin replied to message #{} — customer: {}", messageId, message.getEmail());
        return toReplyResponse(reply);
    }

    @Transactional
    public ContactMessageResponse updateStatus(Long messageId, ContactStatus newStatus) {
        ContactMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message #" + messageId + " not found"));
        message.setStatus(newStatus);
        return toResponse(messageRepository.save(message), false);
    }

    @Transactional
    public void deleteMessage(Long messageId) {
        if (!messageRepository.existsById(messageId)) {
            throw new EntityNotFoundException("Message #" + messageId + " not found");
        }
        messageRepository.deleteById(messageId);
        log.info("Admin deleted contact message #{}", messageId);
    }

    @Transactional(readOnly = true)
    public long countOpenMessages() {
        return messageRepository.countByStatus(ContactStatus.OPEN);
    }

    // ─── SCHEDULED CLEANUP ───────────────────────────────────────────────────
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupResolvedMessages() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
        int deleted = messageRepository.deleteResolvedBefore(cutoff);
        if (deleted > 0) {
            log.info("Cleanup: deleted {} resolved contact messages older than 90 days", deleted);
        }
    }

    private ContactMessageResponse toResponse(ContactMessage m, boolean includeReplies) {
        long unread = (m.getUserId() != null)
                ? replyRepository.countUnreadBySenderType(m.getId(), SenderType.CUSTOMER)
                : 0;

        return ContactMessageResponse.builder()
                .id(m.getId())
                .name(m.getName())
                .email(m.getEmail())
                .subject(m.getSubject())
                .message(m.getMessage())
                .status(m.getStatus())
                .createdAt(m.getCreatedAt())
                .unreadReplies(unread)
                .replies(includeReplies
                        ? m.getReplies().stream().map(this::toReplyResponse).toList()
                        : List.of())
                .build();
    }

    private ContactReplyResponse toReplyResponse(ContactReply r) {
        return ContactReplyResponse.builder()
                .id(r.getId())
                .senderType(r.getSenderType())
                .message(r.getMessage())
                .isRead(r.getIsRead())
                .createdAt(r.getCreatedAt())
                .build();
    }

    @Transactional
    public ContactReplyResponse customerReply(Long messageId, ContactReplyRequest request, Long userId) {
        ContactMessage message = messageRepository.findByIdAndUserId(messageId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found"));

        if (message.getStatus() == ContactStatus.RESOLVED) {
            throw new IllegalStateException("Cannot reply to a resolved ticket");
        }

        ContactReply toSave = ContactReply.builder()
                .contactMessage(message)
                .senderType(SenderType.CUSTOMER)
                .message(request.getMessage())
                .isRead(false)
                .build();

        ContactReply reply = replyRepository.save(toSave);
        log.info("Customer replied to message #{}", messageId);
        return toReplyResponse(reply);
    }

    @Transactional
    public ContactMessageResponse getMessageThread(Long messageId) {
        ContactMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new EntityNotFoundException("Message #" + messageId + " not found"));

        // Mark customer replies as read when admin views the thread
        int marked = replyRepository.markAdminRepliesAsRead(messageId, SenderType.CUSTOMER);
        if (marked > 0) {
            log.debug("Marked {} customer replies as read for message #{}", marked, messageId);
        }

        return toResponse(message, true);
    }
}
