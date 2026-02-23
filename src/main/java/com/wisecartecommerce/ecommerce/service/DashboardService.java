package com.wisecartecommerce.ecommerce.service;

import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.repository.PaymentRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getDashboardStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;

        Map<String, Object> stats = new HashMap<>();

        // Total revenue
        BigDecimal totalRevenue = orderRepository.getTotalRevenue(startDateTime, endDateTime);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);

        // Total orders
        Long totalOrders = orderRepository.count();
        stats.put("totalOrders", totalOrders);

        // Today's orders
        Long todayOrders = orderRepository.countTodayOrders();
        stats.put("todayOrders", todayOrders);

        // Total products
        Long totalProducts = productRepository.count();
        stats.put("totalProducts", totalProducts);

        // Total customers
        Long totalCustomers = userRepository.countCustomers();
        stats.put("totalCustomers", totalCustomers);

        // Today's revenue
        Double todayRevenue = paymentRepository.getTodayRevenue();
        stats.put("todayRevenue", todayRevenue != null ? todayRevenue : 0);

        // Low stock products
        List<Map<String, Object>> lowStockProducts = productRepository.findLowStockProducts(10).stream()
                .map(p -> {
                    Map<String, Object> product = new HashMap<>();
                    product.put("id", p.getId());
                    product.put("name", p.getName());
                    product.put("stock", p.getStockQuantity());
                    return product;
                })
                .toList();
        stats.put("lowStockProducts", lowStockProducts);
        stats.put("lowStockCount", lowStockProducts.size());

        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSalesAnalytics(String period, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> analytics = new HashMap<>();

        if (startDate == null) {
            // Default to last 30 days
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay();

        switch (period.toLowerCase()) {
            case "daily":
                analytics.put("data", getDailySales(startDateTime, endDateTime));
                analytics.put("period", "Daily");
                break;
            case "weekly":
                analytics.put("data", getWeeklySales(startDateTime, endDateTime));
                analytics.put("period", "Weekly");
                break;
            case "monthly":
                analytics.put("data", getMonthlySales(startDateTime, endDateTime));
                analytics.put("period", "Monthly");
                break;
            case "yearly":
                analytics.put("data", getYearlySales(startDateTime, endDateTime));
                analytics.put("period", "Yearly");
                break;
            default:
                analytics.put("data", getDailySales(startDateTime, endDateTime));
                analytics.put("period", "Daily");
        }

        return analytics;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getOrderStatusDistribution(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay()
                : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

        Map<String, Object> distribution = new HashMap<>();

        // Count orders by status within date range
        List<Object[]> statusCounts = orderRepository.findOrderCountByStatus(startDateTime, endDateTime);

        long totalOrders = 0;
        Map<String, Long> counts = new HashMap<>();

        for (Object[] row : statusCounts) {
            String status = row[0].toString();
            Long count = (Long) row[1];
            counts.put(status, count);
            totalOrders += count;
        }

        distribution.put("counts", counts);
        distribution.put("totalOrders", totalOrders);

        // Add percentages
        Map<String, Double> percentages = new HashMap<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            double percentage = totalOrders > 0 ? (entry.getValue() * 100.0 / totalOrders) : 0;
            percentages.put(entry.getKey(), Math.round(percentage * 100.0) / 100.0);
        }
        distribution.put("percentages", percentages);

        return distribution;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLowStockAlerts(int threshold) {
        return productRepository.findLowStockProducts(threshold).stream()
                .map(product -> {
                    Map<String, Object> alert = new HashMap<>();
                    alert.put("id", product.getId());
                    alert.put("name", product.getName());
                    alert.put("sku", product.getSku());
                    alert.put("currentStock", product.getStockQuantity());
                    alert.put("threshold", threshold);
                    alert.put("category",
                            product.getCategory() != null ? product.getCategory().getName() : "Uncategorized");
                    alert.put("status", product.getStockQuantity() == 0 ? "OUT_OF_STOCK" : "LOW_STOCK");
                    return alert;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRecentActivities(int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));

        List<Map<String, Object>> activities = new ArrayList<>();

        // Get recent orders
        List<Order> recentOrders = orderRepository.findAll(pageRequest).getContent();
        for (Order order : recentOrders) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "ORDER");
            activity.put("id", order.getId());
            activity.put("title", "New Order #" + order.getOrderNumber());
            activity.put("description", order.getUser().getEmail() + " placed a new order");
            activity.put("amount", order.getFinalAmount());
            activity.put("status", order.getStatus());
            activity.put("timestamp", order.getCreatedAt());
            activity.put("user", order.getUser().getEmail());
            activities.add(activity);
        }

        // Sort all activities by timestamp (most recent first)
        activities.sort((a, b) -> ((LocalDateTime) b.get("timestamp")).compareTo((LocalDateTime) a.get("timestamp")));

        // Return only the requested limit
        return activities.stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopProducts(int limit, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay()
                : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

        // Use the new date-filtered query
        List<Object[]> topProductsData = productRepository.findTopSellingProductsByDateRange(
                startDateTime, endDateTime, PageRequest.of(0, limit));

        return topProductsData.stream()
                .map(data -> {
                    Product product = (Product) data[0];
                    Long totalSold = ((Number) data[1]).longValue();
                    BigDecimal totalRevenue = (BigDecimal) data[2];
                    
                    Map<String, Object> productMap = new HashMap<>();
                    productMap.put("id", product.getId());
                    productMap.put("name", product.getName());
                    productMap.put("price", product.getPrice());
                    productMap.put("soldCount", totalSold);
                    productMap.put("revenue", totalRevenue);
                    return productMap;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopCustomers(int limit, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay()
                : LocalDate.now().minusMonths(3).atStartOfDay();
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

        PageRequest pageRequest = PageRequest.of(0, limit);
        List<Object[]> topCustomersData = orderRepository.findTopCustomersBySpending(startDateTime, endDateTime,
                pageRequest);

        return topCustomersData.stream()
                .map(data -> {
                    Map<String, Object> customer = new HashMap<>();
                    customer.put("customerId", data[0]);
                    customer.put("customerName", data[1]);
                    customer.put("customerEmail", data[2]);
                    customer.put("totalOrders", data[3]);
                    customer.put("totalSpent", data[4]);
                    return customer;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getRevenueBreakdown(LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay()
                : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime endDateTime = endDate != null ? endDate.plusDays(1).atStartOfDay() : LocalDateTime.now();

        Map<String, Object> revenueBreakdown = new HashMap<>();

        // Get revenue by category
        List<Object[]> revenueByCategory = orderRepository.findRevenueByCategory(startDateTime, endDateTime);
        revenueBreakdown.put("byCategory", revenueByCategory.stream()
                .map(data -> {
                    Map<String, Object> category = new HashMap<>();
                    category.put("categoryName", data[0]);
                    category.put("revenue", data[1]);
                    category.put("orderCount", data[2]);
                    return category;
                })
                .toList());

        // Get revenue by payment method
        List<Object[]> revenueByPaymentMethod = paymentRepository.findRevenueByPaymentMethod(startDateTime,
                endDateTime);
        revenueBreakdown.put("byPaymentMethod", revenueByPaymentMethod.stream()
                .map(data -> {
                    Map<String, Object> method = new HashMap<>();
                    method.put("paymentMethod", data[0]);
                    method.put("revenue", data[1]);
                    method.put("transactionCount", data[2]);
                    return method;
                })
                .toList());

        // Get daily revenue trend
        List<Object[]> dailyRevenue = orderRepository.findDailyRevenue(startDateTime, endDateTime);
        List<Map<String, Object>> dailyTrend = new ArrayList<>();
        for (Object[] data : dailyRevenue) {
            Map<String, Object> day = new HashMap<>();
            day.put("date", data[0]);
            day.put("revenue", data[1]);
            dailyTrend.add(day);
        }
        revenueBreakdown.put("dailyTrend", dailyTrend);

        return revenueBreakdown;
    }

    private List<Map<String, Object>> getDailySales(LocalDateTime start, LocalDateTime end) {
        List<Object[]> dailyData = orderRepository.getDailySales(start, end);
        return dailyData.stream()
                .map(data -> {
                    Map<String, Object> day = new HashMap<>();
                    day.put("date", data[0]);
                    day.put("orderCount", data[1]);
                    day.put("revenue", data[2]);
                    return day;
                })
                .toList();
    }

    private List<Map<String, Object>> getWeeklySales(LocalDateTime start, LocalDateTime end) {
        List<Object[]> weeklyData = orderRepository.getWeeklySales(start, end);
        return weeklyData.stream()
                .map(data -> {
                    Map<String, Object> week = new HashMap<>();
                    week.put("year", data[0]);
                    week.put("week", data[1]);
                    week.put("weekStart", data[2]);
                    week.put("orderCount", data[3]);
                    week.put("revenue", data[4]);
                    return week;
                })
                .toList();
    }

    private List<Map<String, Object>> getMonthlySales(LocalDateTime start, LocalDateTime end) {
        List<Object[]> monthlyData = orderRepository.getMonthlySales(start, end);
        return monthlyData.stream()
                .map(data -> {
                    Map<String, Object> month = new HashMap<>();
                    month.put("year", data[0]);
                    month.put("month", data[1]);
                    month.put("orderCount", data[2]);
                    month.put("revenue", data[3]);
                    return month;
                })
                .toList();
    }

    private List<Map<String, Object>> getYearlySales(LocalDateTime start, LocalDateTime end) {
        List<Object[]> yearlyData = orderRepository.getYearlySales(start, end);
        return yearlyData.stream()
                .map(data -> {
                    Map<String, Object> year = new HashMap<>();
                    year.put("year", data[0]);
                    year.put("orderCount", data[1]);
                    year.put("revenue", data[2]);
                    return year;
                })
                .toList();
    }
}