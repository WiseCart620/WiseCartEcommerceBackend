package com.wisecartecommerce.ecommerce.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wisecartecommerce.ecommerce.entity.PendingCheckout;

public interface PendingCheckoutRepository extends JpaRepository<PendingCheckout, Long> {
    Optional<PendingCheckout> findByCheckoutRef(String checkoutRef);
    Optional<PendingCheckout> findByOrderId(Long orderId);
    Optional<PendingCheckout> findByMayaTransactionReference(String mayaTransactionReference);
}