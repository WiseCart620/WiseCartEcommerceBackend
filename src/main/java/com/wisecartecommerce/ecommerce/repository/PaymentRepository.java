package com.wisecartecommerce.ecommerce.repository;

import com.wisecartecommerce.ecommerce.entity.Payment;
import com.wisecartecommerce.ecommerce.util.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

       Optional<Payment> findByTransactionId(String transactionId);

       List<Payment> findByOrderId(Long orderId);

       Optional<Payment> findByOrderIdAndStatus(Long orderId, PaymentStatus status);

       @Query("SELECT p FROM Payment p WHERE p.order.user.id = :userId ORDER BY p.createdAt DESC")
       List<Payment> findByUserId(@Param("userId") Long userId);

       @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND " +
                     "DATE(p.createdAt) = CURRENT_DATE")
       Double getTodayRevenue();

       @Query("SELECT COUNT(p) FROM Payment p WHERE p.status = 'COMPLETED' AND " +
                     "DATE(p.createdAt) = CURRENT_DATE")
       Long countTodayPayments();

       // Add this missing method for DashboardService
       @Query("SELECT p.paymentMethod, SUM(p.amount), COUNT(p) " +
                     "FROM Payment p WHERE p.status = 'COMPLETED' AND " +
                     "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
                     "(:endDate IS NULL OR p.createdAt <= :endDate) " +
                     "GROUP BY p.paymentMethod " +
                     "ORDER BY SUM(p.amount) DESC")
       List<Object[]> findRevenueByPaymentMethod(@Param("startDate") LocalDateTime startDate,
                     @Param("endDate") LocalDateTime endDate);

       @Query("SELECT p FROM Payment p WHERE p.order.id = :orderId AND p.paymentMethod = 'COD' AND p.status = 'PENDING'")
       Optional<Payment> findPendingCodByOrderId(@Param("orderId") Long orderId);

       // Gets the latest payment record for an order regardless of status — used for
       // COD payment lookup
       Optional<Payment> findFirstByOrderIdOrderByCreatedAtDesc(Long orderId);
}