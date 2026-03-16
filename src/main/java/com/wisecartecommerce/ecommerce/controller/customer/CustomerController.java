package com.wisecartecommerce.ecommerce.controller.customer;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.AddressRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.ChangePasswordRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.UpdateProfileRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.UserResponse;
import com.wisecartecommerce.ecommerce.service.UserService;
import com.wisecartecommerce.ecommerce.service.NotificationService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.NotificationResponse;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customer")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer", description = "Customer management APIs")
public class CustomerController {

    private final UserService userService;
    private final NotificationService notificationService;

    // ==================== Profile Management ====================

    @GetMapping("/profile")
    @Operation(summary = "Get customer profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile() {
        UserResponse response = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", response));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update customer profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response));
    }

    @PostMapping("/profile/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @PostMapping("/profile/upload-avatar")
    @Operation(summary = "Upload profile avatar")
    public ResponseEntity<ApiResponse<UserResponse>> uploadAvatar(
            @RequestParam("file") MultipartFile file) {
        UserResponse response = userService.uploadAvatar(file);
        return ResponseEntity.ok(ApiResponse.success("Avatar uploaded successfully", response));
    }

    @DeleteMapping("/profile/avatar")
    @Operation(summary = "Remove profile avatar")
    public ResponseEntity<ApiResponse<UserResponse>> removeAvatar() {
        UserResponse response = userService.removeAvatar();
        return ResponseEntity.ok(ApiResponse.success("Avatar removed successfully", response));
    }

    // ==================== Address Management ====================

    @GetMapping("/addresses")
    @Operation(summary = "Get customer addresses")
    public ResponseEntity<ApiResponse<Object>> getAddresses() {
        Object addresses = userService.getUserAddresses();
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
    }

    @PostMapping("/addresses")
    @Operation(summary = "Add new address")
    public ResponseEntity<ApiResponse<Object>> addAddress(
            @Valid @RequestBody AddressRequest request) {
        Object address = userService.addAddress(request);
        return ResponseEntity.ok(ApiResponse.success("Address added successfully", address));
    }

    @PutMapping("/addresses/{addressId}")
    @Operation(summary = "Update address")
    public ResponseEntity<ApiResponse<Object>> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request) {
        Object address = userService.updateAddress(addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", address));
    }

    @DeleteMapping("/addresses/{addressId}")
    @Operation(summary = "Delete address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long addressId) {
        userService.deleteAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
    }

    @PatchMapping("/addresses/{addressId}/default")
    @Operation(summary = "Set default address")
    public ResponseEntity<ApiResponse<Object>> setDefaultAddress(@PathVariable Long addressId) {
        Object address = userService.setDefaultAddress(addressId);
        return ResponseEntity.ok(ApiResponse.success("Default address set successfully", address));
    }

    // ==================== Account Management ====================

    @GetMapping("/dashboard")
    @Operation(summary = "Get customer dashboard")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        // Get various stats for customer dashboard
        Map<String, Object> dashboard = Map.of(
                "wishlistCount", 0);

        return ResponseEntity.ok(ApiResponse.success("Dashboard retrieved", dashboard));
    }

    @PostMapping("/subscribe/newsletter")
    @Operation(summary = "Subscribe to newsletter")
    public ResponseEntity<ApiResponse<Void>> subscribeNewsletter() {
        // Implement newsletter subscription logic
        return ResponseEntity.ok(ApiResponse.success("Subscribed to newsletter successfully", null));
    }

    @PostMapping("/unsubscribe/newsletter")
    @Operation(summary = "Unsubscribe from newsletter")
    public ResponseEntity<ApiResponse<Void>> unsubscribeNewsletter() {
        // Implement newsletter unsubscription logic
        return ResponseEntity.ok(ApiResponse.success("Unsubscribed from newsletter successfully", null));
    }

    @DeleteMapping("/account")
    @Operation(summary = "Delete account (soft delete)")
    public ResponseEntity<ApiResponse<Void>> deleteAccount() {
        // Implement account deletion logic (soft delete)
        return ResponseEntity.ok(ApiResponse.success("Account deletion request submitted", null));
    }

    // ==================== Support Tickets ====================

    @PostMapping("/support/ticket")
    @Operation(summary = "Create support ticket")
    public ResponseEntity<ApiResponse<Void>> createSupportTicket(
            @RequestParam String subject,
            @RequestParam String message,
            @RequestParam(required = false) Long orderId) {
        // Implement support ticket creation
        return ResponseEntity.ok(ApiResponse.success("Support ticket created successfully", null));
    }

    @GetMapping("/support/tickets")
    @Operation(summary = "Get support tickets")
    public ResponseEntity<ApiResponse<Object>> getSupportTickets(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Implement support ticket retrieval
        Object tickets = Map.of("tickets", List.of(), "total", 0);
        return ResponseEntity.ok(ApiResponse.success("Support tickets retrieved", tickets));
    }

    // ==================== Notifications ====================

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications")
    public ResponseEntity<ApiResponse<Page<NotificationResponse>>> getNotifications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<NotificationResponse> notifications = notificationService.getNotifications(
                PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.success("Notifications retrieved", notifications));
    }

    @GetMapping("/notifications/unread-count")
    @Operation(summary = "Get unread notification count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        Long count = notificationService.getUnreadCount();
        return ResponseEntity.ok(ApiResponse.success("Unread count retrieved", count));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Mark notification as read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markNotificationAsRead(
            @PathVariable Long notificationId) {
        NotificationResponse response = notificationService.markAsRead(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", response));
    }

    @PatchMapping("/notifications/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllNotificationsAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/notifications/{notificationId}")
    @Operation(summary = "Delete notification")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable Long notificationId) {
        notificationService.deleteNotification(notificationId);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    // ==================== Payment Methods ====================

    @GetMapping("/payment-methods")
    @Operation(summary = "Get saved payment methods")
    public ResponseEntity<ApiResponse<Object>> getPaymentMethods() {
        // Implement payment method retrieval
        Object paymentMethods = List.of();
        return ResponseEntity.ok(ApiResponse.success("Payment methods retrieved", paymentMethods));
    }

    @PostMapping("/payment-methods")
    @Operation(summary = "Add payment method")
    public ResponseEntity<ApiResponse<Void>> addPaymentMethod(@RequestBody Map<String, Object> paymentMethod) {
        // Implement add payment method logic
        return ResponseEntity.ok(ApiResponse.success("Payment method added successfully", null));
    }

    @DeleteMapping("/payment-methods/{paymentMethodId}")
    @Operation(summary = "Remove payment method")
    public ResponseEntity<ApiResponse<Void>> removePaymentMethod(@PathVariable String paymentMethodId) {
        // Implement remove payment method logic
        return ResponseEntity.ok(ApiResponse.success("Payment method removed successfully", null));
    }

    // ==================== Downloads ====================

    @GetMapping("/downloads")
    @Operation(summary = "Get digital downloads")
    public ResponseEntity<ApiResponse<Object>> getDigitalDownloads(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        // Implement digital downloads retrieval
        Object downloads = Map.of("downloads", List.of(), "total", 0);
        return ResponseEntity.ok(ApiResponse.success("Digital downloads retrieved", downloads));
    }
}