package com.wisecartecommerce.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wisecartecommerce.ecommerce.Dto.Response.NotificationResponse;
import com.wisecartecommerce.ecommerce.entity.User;

public interface NotificationService {

    Page<NotificationResponse> getNotifications(Pageable pageable);

    Long getUnreadCount();

    NotificationResponse markAsRead(Long notificationId);

    void markAllAsRead();

    void deleteNotification(Long notificationId);

    void createNotification(User user, String title, String message, String type,
            Long referenceId, String referenceType);

    void createAdminNotification(String title, String message, String type,
            Long referenceId, String referenceType);

    void createOrderNotification(User user, String title, String message,
            Long referenceId, String imageUrl, int totalItems);
}
