package com.wisecartecommerce.ecommerce.Dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {
    
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;
    
    private BigDecimal amount;
    
    // Credit/Debit Card details
    @Pattern(regexp = "^[0-9]{13,19}$", message = "Card number must be 13-19 digits")
    private String cardNumber;
    
    private String cardHolder;
    
    @Pattern(regexp = "^(0[1-9]|1[0-2])/[0-9]{2}$", message = "Expiry date must be in MM/YY format")
    private String expiryDate;
    
    @Pattern(regexp = "^[0-9]{3,4}$", message = "CVV must be 3-4 digits")
    private String cvv;
    
    // PayPal details
    private String paypalEmail;
    private String paypalOrderId;
    
    // Bank Transfer details
    private String bankAccount;
    private String bankName;
    private String accountHolder;
    
    // Cash on Delivery
    private boolean codAccepted;
    
    // Additional information
    private String notes;
    
    @Builder.Default
    private boolean saveCard = false;
}