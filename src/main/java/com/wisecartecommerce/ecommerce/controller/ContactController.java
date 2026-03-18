package com.wisecartecommerce.ecommerce.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Request.ContactRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.ContactMessageResponse;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.service.ContactService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/public/contact")
@RequiredArgsConstructor
public class ContactController {

    private final ContactService contactService;

    @PostMapping
    public ResponseEntity<ApiResponse<ContactMessageResponse>> submitContact(
            @Valid @RequestBody ContactRequest request,
            @AuthenticationPrincipal User user) {
        Long userId = (user != null) ? user.getId() : null;

        ContactMessageResponse response = contactService.submitMessage(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Message sent successfully", response));
    }
}