package com.wisecartecommerce.ecommerce.service;

import java.util.List;
import java.util.Map;

import com.wisecartecommerce.ecommerce.Dto.Request.PaymentRequest;
import com.wisecartecommerce.ecommerce.util.PaymentStatus;

public interface PaymentService {

    PaymentResponse processPayment(PaymentRequest request);

    PaymentResponse getPaymentById(Long paymentId);

    PaymentResponse getPaymentByOrderId(Long orderId);

    PaymentResponse requestRefund(Long paymentId, String reason);

    List<PaymentResponse> getPaymentHistory();

    List<String> getAvailablePaymentMethods();

    void handleWebhook(String payload, String signature);

    PaymentResponse updatePaymentStatus(Long paymentId, PaymentStatus status);

    PaymentResponse getPaymentByTransactionId(String transactionId);

    String createMayaCheckout(Long orderId);

    void handleMayaWebhook(Map<String, Object> payload);
}
