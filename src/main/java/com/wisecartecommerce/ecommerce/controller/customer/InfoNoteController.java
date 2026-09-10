package com.wisecartecommerce.ecommerce.controller.customer;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.InfoNoteResponse;
import com.wisecartecommerce.ecommerce.service.InfoNoteService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/storefront/info-notes")
@RequiredArgsConstructor
public class InfoNoteController {

    private final InfoNoteService infoNoteService;

    @GetMapping("/for-product/{productId}")
    public ResponseEntity<ApiResponse<InfoNoteResponse>> forProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Info note resolved",
                infoNoteService.resolveForProduct(productId)));
    }

    @GetMapping("/for-product/{productId}/all")
    public ResponseEntity<ApiResponse<List<InfoNoteResponse>>> allForProduct(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.success("Info notes resolved",
                infoNoteService.resolveAllForProduct(productId)));
    }
}
