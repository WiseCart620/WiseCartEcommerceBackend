package com.wisecartecommerce.ecommerce.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.util.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

        Optional<Order> findByOrderNumber(String orderNumber);

        List<Order> findByUserId(Long userId);

        Page<Order> findByUserId(Long userId, Pageable pageable);

        @Query("SELECT o FROM Order o LEFT JOIN o.user u WHERE " +
                        "(:status IS NULL OR o.status = :status) AND " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate) AND " +
                        "(:customerEmail IS NULL OR u.email = :customerEmail OR o.guestEmail = :customerEmail)")
        Page<Order> findOrdersWithFilters(
                        @Param("status") OrderStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        @Param("customerEmail") String customerEmail,
                        Pageable pageable);

        Optional<Order> findByTrackingNumber(String trackingNumber);

        @Query("SELECT o FROM Order o WHERE DATE(o.createdAt) = CURRENT_DATE")
        Page<Order> findTodayOrders(Pageable pageable);

        @Query("SELECT o FROM Order o WHERE o.user.id = :userId ORDER BY o.createdAt DESC")
        List<Order> findRecentOrdersByUserId(@Param("userId") Long userId, Pageable pageable);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
        Long countByStatus(@Param("status") OrderStatus status);

        @Query("SELECT SUM(o.finalAmount) FROM Order o WHERE o.status = 'DELIVERED' AND " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate)")
        BigDecimal getTotalRevenue(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(o) FROM Order o WHERE DATE(o.createdAt) = CURRENT_DATE")
        Long countTodayOrders();

        @Query("SELECT o FROM Order o WHERE o.createdAt >= :startDate ORDER BY o.finalAmount DESC")
        List<Order> findTopOrdersByRevenue(@Param("startDate") LocalDateTime startDate, Pageable pageable);

        @Query("SELECT FUNCTION('DATE', o.createdAt) as date, COUNT(o) as count, SUM(o.finalAmount) as revenue " +
                        "FROM Order o WHERE " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate) " +
                        "GROUP BY FUNCTION('DATE', o.createdAt) " +
                        "ORDER BY date DESC")
        List<Object[]> getDailySales(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        // Add these methods for DashboardService

        @Query("SELECT o.status, COUNT(o) FROM Order o WHERE " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate) " +
                        "GROUP BY o.status")
        List<Object[]> findOrderCountByStatus(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT o.user.id, CONCAT(o.user.firstName, ' ', o.user.lastName), o.user.email, " +
                        "COUNT(o), SUM(o.finalAmount) " +
                        "FROM Order o WHERE " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate) " +
                        "GROUP BY o.user.id, o.user.firstName, o.user.lastName, o.user.email " +
                        "ORDER BY SUM(o.finalAmount) DESC")
        List<Object[]> findTopCustomersBySpending(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate,
                        Pageable pageable);

        @Query("SELECT c.name, SUM(oi.price * oi.quantity), COUNT(DISTINCT o.id) " +
                        "FROM Order o " +
                        "JOIN o.items oi " +
                        "JOIN oi.product p " +
                        "JOIN p.category c " +
                        "WHERE o.status = 'DELIVERED' AND " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate) " +
                        "GROUP BY c.id, c.name " +
                        "ORDER BY SUM(oi.price * oi.quantity) DESC")
        List<Object[]> findRevenueByCategory(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT FUNCTION('DATE', o.createdAt), SUM(o.finalAmount) " +
                        "FROM Order o WHERE o.status = 'DELIVERED' AND " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate) " +
                        "GROUP BY FUNCTION('DATE', o.createdAt) " +
                        "ORDER BY FUNCTION('DATE', o.createdAt)")
        List<Object[]> findDailyRevenue(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query(value = "SELECT YEAR(o.created_at) as year, " +
                        "WEEK(o.created_at) as week, " +
                        "MIN(DATE(o.created_at)) as weekStart, " +
                        "COUNT(o.id) as count, " +
                        "SUM(o.final_amount) as revenue " +
                        "FROM orders o " +
                        "WHERE o.status = 'DELIVERED' " +
                        "AND (:startDate IS NULL OR o.created_at >= :startDate) " +
                        "AND (:endDate IS NULL OR o.created_at <= :endDate) " +
                        "GROUP BY YEAR(o.created_at), WEEK(o.created_at) " +
                        "ORDER BY year DESC, week DESC", nativeQuery = true)
        List<Object[]> getWeeklySales(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT YEAR(o.createdAt) as year, MONTH(o.createdAt) as month, " +
                        "COUNT(o) as count, SUM(o.finalAmount) as revenue " +
                        "FROM Order o WHERE o.status = 'DELIVERED' AND " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate) " +
                        "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
                        "ORDER BY year DESC, month DESC")
        List<Object[]> getMonthlySales(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT YEAR(o.createdAt) as year, " +
                        "COUNT(o) as count, SUM(o.finalAmount) as revenue " +
                        "FROM Order o WHERE o.status = 'DELIVERED' AND " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate) " +
                        "GROUP BY YEAR(o.createdAt) " +
                        "ORDER BY year DESC")
        List<Object[]> getYearlySales(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        @Query("SELECT COUNT(o) FROM Order o WHERE " +
                        "(:startDate IS NULL OR o.createdAt >= :startDate) AND " +
                        "(:endDate IS NULL OR o.createdAt <= :endDate)")
        Long countOrdersInRange(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        Optional<Order> findByOrderNumberAndGuestEmail(String orderNumber, String guestEmail);

        @Query(value = """
                        SELECT
                            COALESCE(o.guest_email, u.email)                           AS email,
                            COALESCE(o.guest_phone, a.phone)                           AS phone,
                            COALESCE(
                                CONCAT(o.guest_first_name, ' ', o.guest_last_name),
                                CONCAT(u.first_name, ' ', u.last_name)
                            )                                                           AS name
                        FROM orders o
                        LEFT JOIN users u     ON o.user_id = u.id
                        LEFT JOIN addresses a ON o.shipping_address_id = a.id
                        WHERE COALESCE(o.guest_email, u.email) IS NOT NULL
                        GROUP BY
                            COALESCE(o.guest_email, u.email),
                            COALESCE(o.guest_phone, a.phone),
                            COALESCE(
                                CONCAT(o.guest_first_name, ' ', o.guest_last_name),
                                CONCAT(u.first_name, ' ', u.last_name)
                            )
                        """, nativeQuery = true)
        List<Object[]> findAllCustomerContacts();
}