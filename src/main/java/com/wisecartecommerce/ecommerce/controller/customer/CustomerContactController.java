package com.wisecartecommerce.ecommerce.controller.customer;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Request.ContactReplyRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ContactMessageResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ContactReplyResponse;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.service.ContactService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/customer/contact")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerContactController {

    private final ContactService contactService;

    /**
     * GET /api/customer/contact Returns all the logged-in customer's contact
     * threads (summary, no replies).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ContactMessageResponse>>> getMyMessages(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success("Messages retrieved", contactService.getMyMessages(user.getId()))
        );
    }

    /**
     * GET /api/customer/contact/unread-count Returns the total unread admin
     * reply count — used for the navbar notification badge. Lightweight: single
     * COUNT query, no joins.
     */
    @GetMapping("/unread-count/summary")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User user) {
        long count = contactService.getUnreadCount(user.getId());
        return ResponseEntity.ok(
                ApiResponse.success("Unread count", Map.of("unreadCount", count))
        );
    }

    /**
     * GET /api/customer/contact/{id} Returns a single thread with all replies.
     * Marks admin replies as read.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> getThread(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success("Thread retrieved", contactService.getMyMessageThread(id, user.getId()))
        );
    }

    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<ContactReplyResponse>> customerReply(
            @PathVariable Long id,
            @Valid @RequestBody ContactReplyRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(
                ApiResponse.success("Reply sent", contactService.customerReply(id, request, user.getId()))
        );
    }
}
