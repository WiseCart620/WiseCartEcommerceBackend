package com.wisecartecommerce.ecommerce.controller.customer;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.InfoNoteSettingsResponse;
import com.wisecartecommerce.ecommerce.service.InfoNoteSettingsService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/storefront/info-note-settings")
@RequiredArgsConstructor
public class InfoNoteSettingsController {

    private final InfoNoteSettingsService infoNoteSettingsService;

    @GetMapping
    public ResponseEntity<ApiResponse<InfoNoteSettingsResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("Settings retrieved", infoNoteSettingsService.getSettings()));
    }
}