package com.wisecartecommerce.ecommerce.controller.publicapi;

import com.wisecartecommerce.ecommerce.Dto.Request.SendGuestOtpRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.VerifyGuestOtpRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.ApiResponse;
import com.wisecartecommerce.ecommerce.service.GuestEmailVerificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/public/checkout")
@RequiredArgsConstructor
@Tag(name = "Guest Checkout", description = "Email verification for guest coupon usage")
public class PublicCheckoutController {

    private final GuestEmailVerificationService verificationService;

    @PostMapping("/send-email-otp")
    @Operation(summary = "Send a verification code to a guest's email before honoring a coupon")
    public ResponseEntity<ApiResponse<Void>> sendOtp(@Valid @RequestBody SendGuestOtpRequest request) {
        verificationService.sendOtp(request.getEmail(), request.getCouponCode(),
                com.wisecartecommerce.ecommerce.entity.GuestEmailVerification.OtpPurpose.GUEST_CHECKOUT);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent", null));
    }

    @PostMapping("/verify-email-otp")
    @Operation(summary = "Verify a guest's email with the OTP they received")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(@Valid @RequestBody VerifyGuestOtpRequest request) {
        verificationService.verifyOtp(request.getEmail(), request.getOtp(),
                com.wisecartecommerce.ecommerce.entity.GuestEmailVerification.OtpPurpose.GUEST_CHECKOUT);
        return ResponseEntity.ok(ApiResponse.success("Email verified", null));
    }
}
