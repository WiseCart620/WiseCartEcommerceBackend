package com.wisecartecommerce.ecommerce.controller.admin;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.InfoNoteRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.InfoNoteResponse;
import com.wisecartecommerce.ecommerce.service.InfoNoteService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/admin/info-notes")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Admin Info Notes", description = "Manage product info notes (global or per-product)")
public class AdminInfoNoteController {

    private final InfoNoteService infoNoteService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InfoNoteResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success("Info notes retrieved", infoNoteService.getAll()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<InfoNoteResponse>> create(@Valid @RequestBody InfoNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Info note created", infoNoteService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<InfoNoteResponse>> update(
            @PathVariable Long id, @Valid @RequestBody InfoNoteRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Info note updated", infoNoteService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        infoNoteService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Info note deleted", null));
    }

    @PostMapping("/{id}/icon")
    public ResponseEntity<ApiResponse<InfoNoteResponse>> uploadIcon(
            @PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success("Icon uploaded", infoNoteService.uploadIcon(id, file)));
    }
}