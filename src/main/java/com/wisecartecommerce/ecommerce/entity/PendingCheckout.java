package com.wisecartecommerce.ecommerce.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pending_checkouts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingCheckout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String checkoutRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "guest_email")
    private String guestEmail;

    @Column(nullable = false)
    private String paymentMethod;

    private String verificationToken;

    private Long shippingAddressId;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
    private String firstName;
    private String lastName;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "maya_checkout_id")
    private String mayaCheckoutId;

    @Column(name = "maya_checkout_url")
    private String mayaCheckoutUrl;

    @Column(name = "maya_transaction_reference")
    private String mayaTransactionReference;

    @Column(name = "maya_payment_id", length = 100)
    private String mayaPaymentId;

    private BigDecimal shippingFee;
    private Integer expressCategory;
    private String couponCode;
    private String notes;
    private Long orderId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PendingCheckoutStatus status = PendingCheckoutStatus.PENDING;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusMinutes(30);
        }
        if (paymentMethod == null) {
            paymentMethod = "maya";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // Auto-update handled by @UpdateTimestamp
    }

    public boolean isGuest() {
        return user == null;
    }

    public String getCustomerEmail() {
        if (user != null) {
            return user.getEmail();
        }
        return guestEmail;
    }

    public enum PendingCheckoutStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, EXPIRED
    }


}
