package com.wisecartecommerce.ecommerce.controller.admin;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Request.ContactReplyRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ContactMessageResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ContactReplyResponse;
import com.wisecartecommerce.ecommerce.entity.ContactMessage.ContactStatus;
import com.wisecartecommerce.ecommerce.service.ContactService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/contact")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminContactController {

    private final ContactService contactService;

    /**
     * GET /api/admin/contact?status=OPEN&page=0&size=20 Paginated list of all
     * contact messages, optionally filtered by status.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ContactMessageResponse>>> getAllMessages(
            @RequestParam(required = false) ContactStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(
                ApiResponse.success("Messages retrieved", contactService.getAllMessages(status, page, size))
        );
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getStats() {
        return ResponseEntity.ok(
                ApiResponse.success("Stats retrieved", Map.of("openMessages", contactService.countOpenMessages()))
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> getThread(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success("Thread retrieved", contactService.getMessageThread(id))
        );
    }

    /**
     * POST /api/admin/contact/{id}/reply Admin posts a reply — saves to DB and
     * emails the customer.
     */
    @PostMapping("/{id}/reply")
    public ResponseEntity<ApiResponse<ContactReplyResponse>> reply(
            @PathVariable Long id,
            @Valid @RequestBody ContactReplyRequest request) {
        return ResponseEntity.ok(
                ApiResponse.success("Reply sent", contactService.adminReply(id, request))
        );
    }

    /**
     * PATCH /api/admin/contact/{id}/status?status=RESOLVED Update the status of
     * a contact message.
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<ContactMessageResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam ContactStatus status) {
        return ResponseEntity.ok(
                ApiResponse.success("Status updated", contactService.updateStatus(id, status))
        );
    }

    /**
     * DELETE /api/admin/contact/{id} Hard delete a message and all its replies.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable Long id) {
        contactService.deleteMessage(id);
        return ResponseEntity.ok(ApiResponse.success("Message deleted", null));
    }

    /**
     * GET /api/admin/contact/stats Returns open message count — for admin
     * dashboard badge.
     */
}
