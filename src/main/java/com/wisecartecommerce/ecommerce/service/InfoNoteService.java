package com.wisecartecommerce.ecommerce.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.wisecartecommerce.ecommerce.Dto.Request.InfoNoteRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.InfoNoteResponse;

public interface InfoNoteService {

    List<InfoNoteResponse> getAll();

    InfoNoteResponse create(InfoNoteRequest request);

    InfoNoteResponse update(Long id, InfoNoteRequest request);

    void delete(Long id);

    InfoNoteResponse uploadIcon(Long id, MultipartFile file);

    InfoNoteResponse resolveForProduct(Long productId);

    List<InfoNoteResponse> resolveAllForProduct(Long productId);
}
