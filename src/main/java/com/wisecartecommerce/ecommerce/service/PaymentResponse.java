package com.wisecartecommerce.ecommerce.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.wisecartecommerce.ecommerce.util.PaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private Long id;
    private String transactionId;
    private Long orderId;
    private String orderNumber;
    private BigDecimal amount;
    private PaymentStatus status;
    private String paymentMethod;
    private String paymentGateway;
    private String cardLastFour;
    private String cardBrand;
    private String payerEmail;
    private String payerName;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime refundedAt;
    private String gatewayResponse;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private LocalDateTime failedAt;
}