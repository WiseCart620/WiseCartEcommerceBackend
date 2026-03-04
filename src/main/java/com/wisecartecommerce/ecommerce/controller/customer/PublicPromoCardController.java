package com.wisecartecommerce.ecommerce.controller.customer;

import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.PromoCardResponse;
import com.wisecartecommerce.ecommerce.service.impl.PromoCardServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/public/promo-cards")
@RequiredArgsConstructor
public class PublicPromoCardController {

    private final PromoCardServiceImpl promoCardService;

    /**
     * Returns only active promo cards, sorted by displayOrder.
     * Called by the homepage on load — no auth required.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PromoCardResponse>>> getActive() {
        return ResponseEntity.ok(ApiResponse.success("Promo cards retrieved", promoCardService.getActive()));
    }
}