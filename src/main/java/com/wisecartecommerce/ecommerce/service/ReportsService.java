package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.repository.PaymentRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportsService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesReport(String range, LocalDate startDate, LocalDate endDate) {
        LocalDateTime[] dates = resolveDateRange(range, startDate, endDate);
        LocalDateTime start = dates[0];
        LocalDateTime end = dates[1];

        Map<String, Object> report = new HashMap<>();

        // Total orders and revenue
        Long totalOrders = orderRepository.countOrdersInRange(start, end);
        BigDecimal totalRevenue = orderRepository.getTotalRevenue(start, end);
        if (totalRevenue == null) totalRevenue = BigDecimal.ZERO;

        report.put("totalOrders", totalOrders != null ? totalOrders : 0);
        report.put("totalRevenue", totalRevenue);
        report.put("averageOrderValue", totalOrders != null && totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        report.put("conversionRate", 0); // placeholder

        // Sales trend (daily)
        List<Object[]> dailySales = orderRepository.getDailySales(start, end);
        List<Map<String, Object>> salesTrend = dailySales.stream()
                .map(data -> {
                    Map<String, Object> day = new HashMap<>();
                    day.put("date", data[0].toString());
                    day.put("orders", data[1]);
                    day.put("revenue", data[2]);
                    return day;
                }).toList();
        report.put("salesTrend", salesTrend);

        // Payment methods
        List<Object[]> paymentData = paymentRepository.findRevenueByPaymentMethod(start, end);
        List<Map<String, Object>> paymentMethods = paymentData.stream()
                .map(data -> {
                    Map<String, Object> method = new HashMap<>();
                    method.put("name", data[0] != null ? data[0].toString() : "UNKNOWN");
                    method.put("value", ((Number) data[2]).longValue()); // transaction count
                    method.put("revenue", data[1]);
                    return method;
                }).toList();
        report.put("paymentMethods", paymentMethods);

        // Order status distribution
        List<Object[]> statusData = orderRepository.findOrderCountByStatus(start, end);
        List<Map<String, Object>> orderStatus = statusData.stream()
                .map(data -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("name", data[0].toString());
                    status.put("value", data[1]);
                    return status;
                }).toList();
        report.put("orderStatus", orderStatus);

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueReport(String range, LocalDate startDate, LocalDate endDate) {
        LocalDateTime[] dates = resolveDateRange(range, startDate, endDate);
        LocalDateTime start = dates[0];
        LocalDateTime end = dates[1];

        Map<String, Object> report = new HashMap<>();

        // Revenue by category
        List<Object[]> byCategory = orderRepository.findRevenueByCategory(start, end);
        report.put("byCategory", byCategory.stream().map(data -> {
            Map<String, Object> cat = new HashMap<>();
            cat.put("name", data[0]);
            cat.put("value", data[1]);
            cat.put("orderCount", data[2]);
            return cat;
        }).toList());

        // Revenue by payment method
        List<Object[]> byPayment = paymentRepository.findRevenueByPaymentMethod(start, end);
        report.put("paymentMethods", byPayment.stream().map(data -> {
            Map<String, Object> method = new HashMap<>();
            method.put("name", data[0] != null ? data[0].toString() : "UNKNOWN");
            method.put("value", ((Number) data[2]).longValue());
            method.put("revenue", data[1]);
            return method;
        }).toList());

        // Daily revenue trend
        List<Object[]> dailyRevenue = orderRepository.findDailyRevenue(start, end);
        report.put("salesTrend", dailyRevenue.stream().map(data -> {
            Map<String, Object> day = new HashMap<>();
            day.put("date", data[0].toString());
            day.put("revenue", data[1]);
            return day;
        }).toList());

        return report;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getProductPerformanceReport(String range, LocalDate startDate, LocalDate endDate) {
        LocalDateTime[] dates = resolveDateRange(range, startDate, endDate);
        LocalDateTime start = dates[0];
        LocalDateTime end = dates[1];

        Map<String, Object> report = new HashMap<>();

        List<Object[]> topProductsData = productRepository.findTopSellingProductsByDateRange(
                start, end, PageRequest.of(0, 20));

        List<Map<String, Object>> topProducts = topProductsData.stream().map(data -> {
            var product = (com.wisecartecommerce.ecommerce.entity.Product) data[0];
            Map<String, Object> p = new HashMap<>();
            p.put("id", product.getId());
            p.put("name", product.getName());
            p.put("quantity", ((Number) data[1]).longValue());
            p.put("revenue", data[2]);
            p.put("avgPrice", product.getPrice());
            return p;
        }).toList();

        report.put("topProducts", topProducts);
        report.put("products", topProducts);

        return report;
    }

    private LocalDateTime[] resolveDateRange(String range, LocalDate startDate, LocalDate endDate) {
        LocalDateTime start;
        LocalDateTime end = LocalDateTime.now();

        if (startDate != null && endDate != null) {
            return new LocalDateTime[]{startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay()};
        }

        switch (range != null ? range.toLowerCase() : "month") {
            case "today" -> start = LocalDate.now().atStartOfDay();
            case "week" -> start = LocalDate.now().minusWeeks(1).atStartOfDay();
            case "quarter" -> start = LocalDate.now().minusMonths(3).atStartOfDay();
            case "year" -> start = LocalDate.now().minusYears(1).atStartOfDay();
            default -> start = LocalDate.now().minusMonths(1).atStartOfDay(); // "month"
        }

        return new LocalDateTime[]{start, end};
    }
}   