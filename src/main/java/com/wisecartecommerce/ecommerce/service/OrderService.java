package com.wisecartecommerce.ecommerce.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.wisecartecommerce.ecommerce.Dto.Request.GuestOrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.OrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.util.OrderStatus;

import java.time.LocalDate;
import java.util.List;

public interface OrderService {
    OrderResponse createOrder(OrderRequest request);

    OrderResponse getOrderById(Long id);

    OrderResponse getUserOrderById(Long id);

    OrderResponse trackOrder(String orderNumber);

    Page<OrderResponse> getUserOrders(Pageable pageable);

    Page<OrderResponse> getAllOrders(Pageable pageable, OrderStatus status, LocalDate startDate, LocalDate endDate,
            String customerEmail);

    Page<OrderResponse> getTodayOrders(Pageable pageable);

    Page<OrderResponse> getOrdersByCustomer(Long userId, Pageable pageable);

    OrderResponse updateOrderStatus(Long id, OrderStatus status, String notes);

    OrderResponse cancelOrder(Long id);

    OrderResponse cancelUserOrder(Long id);

    OrderResponse requestReturn(Long id, String reason);

    List<OrderResponse> getRecentOrders(int limit);

    List<OrderResponse> getUserRecentOrders(int limit);

    Object getOrderStats(LocalDate startDate, LocalDate endDate);

    Object getUserOrderCountsByStatus();

    OrderResponse createReview(Long orderId, String review, Integer rating);

    OrderResponse createGuestOrder(GuestOrderRequest request);

    OrderResponse trackGuestOrder(String orderNumber, String email);
}