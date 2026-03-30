package com.wisecartecommerce.ecommerce.Dto.Response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.wisecartecommerce.ecommerce.util.OrderStatus;
import com.wisecartecommerce.ecommerce.util.PaymentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userEmail;
    private String userName;
    private OrderStatus status;
    private BigDecimal totalAmount;
    private BigDecimal discountAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal finalAmount;
    private AddressResponse shippingAddress;
    private AddressResponse billingAddress;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private String couponCode;
    private String trackingNumber;
    private String shippingCarrier;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime deliveredAt;
    private List<OrderItemResponse> items;
    private List<PaymentResponse> payments;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String mayaPaymentMethod;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long id;
        private Long productId;
        private String productName;
        private String productImage;
        private BigDecimal price;
        private Integer quantity;
        private BigDecimal subtotal;
        private String variationName;
        private boolean isAddon;
        private Long addonProductId;
        private String addonProductName;
        private String addonVariationId;
        private String addonVariationName;
        private BigDecimal addonPrice;
        
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentResponse {
        private Long id;
        private String transactionId;
        private BigDecimal amount;
        private com.wisecartecommerce.ecommerce.util.PaymentStatus status;
        private String paymentMethod;
        private String paymentGateway;
        private LocalDateTime createdAt;
        private LocalDateTime completedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private Long id;
        private String firstName;
        private String lastName;
        private String phone;
        private String addressLine1;
        private String addressLine2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String companyName;
    }
}