package com.wisecartecommerce.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Response.NotificationResponse;
import com.wisecartecommerce.ecommerce.entity.Notification;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.NotificationRepository;
import com.wisecartecommerce.ecommerce.repository.UserRepository;
import com.wisecartecommerce.ecommerce.util.Role;
import java.util.List;
import com.wisecartecommerce.ecommerce.service.NotificationService;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getNotifications(Pageable pageable) {
        User user = getCurrentUser();
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(user.getId(), pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Long getUnreadCount() {
        User user = getCurrentUser();
        return notificationRepository.countUnreadByUserId(user.getId());
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long notificationId) {
        User user = getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only update your own notifications");
        }

        notification.setRead(true);
        return mapToResponse(notificationRepository.save(notification));
    }

    @Override
    @Transactional
    public void markAllAsRead() {
        User user = getCurrentUser();
        notificationRepository.markAllAsReadByUserId(user.getId());
        log.info("Marked all notifications as read for user: {}", user.getEmail());
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId) {
        User user = getCurrentUser();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only delete your own notifications");
        }

        notificationRepository.delete(notification);
        log.info("Deleted notification {} for user: {}", notificationId, user.getEmail());
    }

    @Override
    @Transactional
    public void createNotification(User user, String title, String message, String type,
            Long referenceId, String referenceType) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .referenceType(referenceType)
                .build();

        notificationRepository.save(notification);
        log.info("Created {} notification for user: {}", type, user.getEmail());
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .read(notification.isRead())
                .referenceId(notification.getReferenceId())
                .referenceType(notification.getReferenceType())
                .imageUrl(notification.getImageUrl())
                .totalItems(notification.getTotalItems())
                .createdAt(notification.getCreatedAt())
                .build();
    }

    @Override
    @Transactional
    public void createAdminNotification(String title, String message, String type,
            Long referenceId, String referenceType) {
        List<User> admins = userRepository.findByRole(Role.ADMIN);
        for (User admin : admins) {
            createNotification(admin, title, message, type, referenceId, referenceType);
        }
        log.info("Created {} notification for {} admin(s)", type, admins.size());
    }

    @Override
    @Transactional
    public void createOrderNotification(User user, String title, String message,
            Long referenceId, String imageUrl, int totalItems) {
        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type("ORDER")
                .referenceId(referenceId)
                .referenceType("ORDER_STATUS")
                .imageUrl(imageUrl)
                .totalItems(totalItems)
                .build();
        notificationRepository.save(notification);
        log.info("Created ORDER notification with image for user: {}", user.getEmail());
    }
}
