package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.Dto.Request.GuestOrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.OrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashShippingRateResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.entity.*;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.*;
import com.wisecartecommerce.ecommerce.service.EmailService;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import com.wisecartecommerce.ecommerce.service.OrderService;
import com.wisecartecommerce.ecommerce.util.OrderStatus;
import com.wisecartecommerce.ecommerce.util.ShippingWeightCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    // ── Repositories ───────────────────────────────────────────────────────────
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ProductVariationRepository productVariationRepository;

    // ── Services ───────────────────────────────────────────────────────────────
    private final EmailService emailService;
    private final FlashExpressShippingService flashShippingService;

    // ── Utilities ──────────────────────────────────────────────────────────────
    private final ShippingWeightCalculator weightCalculator;

    // ── Constants ──────────────────────────────────────────────────────────────
    /** Orders at or above this amount ship free. */
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2500");
    /** Philippines VAT. */
    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
    /** Flat fallback when Flash Express API is unreachable. */
    private static final BigDecimal FALLBACK_SHIPPING = new BigDecimal("150.00");

    // ══════════════════════════════════════════════════════════════════════════
    // Authenticated order creation
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = getCurrentUser();

        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));

        if (cart.getItems().isEmpty())
            throw new CustomException("Cannot create order with an empty cart");

        // ── Resolve addresses ──────────────────────────────────────────────────
        Address shippingAddress = resolveAddress(
                request.getShippingAddressId(), request.getShippingAddress(), user);
        Address billingAddress = (request.getBillingAddressId() != null || request.getBillingAddress() != null)
                ? resolveAddress(request.getBillingAddressId(), request.getBillingAddress(), user)
                : shippingAddress;

        // ── Validate stock + build line items ─────────────────────────────────
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> stockUpdates = new HashMap<>(); // For products
        Map<Long, Integer> variationStockUpdates = new HashMap<>(); // For variations

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            ProductVariation variation = cartItem.getVariation(); // Get the variation

            if (!product.isActive())
                throw new CustomException("Product '" + product.getName() + "' is no longer available");

            // Check variation stock if this cart item has a variation
            if (variation != null) {
                if (!variation.isActive())
                    throw new CustomException("Variation '" + variation.getName() + "' is no longer available");
                if (variation.getStockQuantity() < cartItem.getQuantity())
                    throw new CustomException("Insufficient stock for '" + product.getName() +
                            " (" + variation.getName() + ")'" + ". Available: " + variation.getStockQuantity());

                // Track variation stock update
                variationStockUpdates.put(variation.getId(),
                        variation.getStockQuantity() - cartItem.getQuantity());
            } else {
                // Fall back to product stock if no variation (for backward compatibility)
                if (product.getStockQuantity() < cartItem.getQuantity())
                    throw new CustomException("Insufficient stock for '" + product.getName()
                            + "'. Available: " + product.getStockQuantity());

                // Track product stock update
                stockUpdates.put(product.getId(), product.getStockQuantity() - cartItem.getQuantity());
            }

            // Create order item with variation info
            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .variation(variation) // You'll need to add this field to OrderItem entity
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .subtotal(cartItem.getSubtotal())
                    .build();

            orderItems.add(orderItem);
            product.setSoldCount(product.getSoldCount() + cartItem.getQuantity());
        }

        // ── Shipping via Flash Express ─────────────────────────────────────────
        BigDecimal shippingAmount = resolveShippingFee(
                request.getShippingFee(),
                request.getExpressCategory(),
                shippingAddress,
                cart.getSubtotal(),
                cart.getItems());

        // ── Tax (12% VAT) ──────────────────────────────────────────────────────
        BigDecimal taxAmount = cart.getSubtotal()
                .multiply(VAT_RATE)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        // ── Build + persist order ──────────────────────────────────────────────
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .paymentMethod(request.getPaymentMethod())
                .couponCode(cart.getCouponCode())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .totalAmount(cart.getSubtotal())
                .discountAmount(cart.getDiscountAmount() != null ? cart.getDiscountAmount() : BigDecimal.ZERO)
                .shippingAmount(shippingAmount)
                .taxAmount(taxAmount)
                .items(new ArrayList<>())
                .build();

        order.calculateTotals();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);

        // Update product stock (for items without variations)
        for (Map.Entry<Long, Integer> entry : stockUpdates.entrySet()) {
            productRepository.findById(entry.getKey()).ifPresent(p -> {
                p.setStockQuantity(entry.getValue());
                productRepository.save(p);
            });
        }

        // Update variation stock
        for (Map.Entry<Long, Integer> entry : variationStockUpdates.entrySet()) {
            productVariationRepository.findById(entry.getKey()).ifPresent(v -> {
                v.setStockQuantity(entry.getValue());
                productVariationRepository.save(v);
            });
        }

        // Clear cart
        cart.getItems().clear();
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setTotal(BigDecimal.ZERO);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setCouponCode(null);
        cart.setCouponDiscountAmount(BigDecimal.ZERO);
        cartRepository.save(cart);

        emailService.sendOrderConfirmationEmail(saved);
        log.info("Order created: {} | user: {} | shipping: ₱{}", saved.getOrderNumber(), user.getEmail(),
                shippingAmount);

        return mapToOrderResponse(saved);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Guest order creation
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional
    public OrderResponse createGuestOrder(GuestOrderRequest request) {

        // Save shipping address
        Address shippingAddress = addressRepository.save(Address.builder()
                .firstName(request.getGuestFirstName())
                .lastName(request.getGuestLastName())
                .phone(request.getGuestPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .addressType("SHIPPING")
                .isDefault(false)
                .build());

        // Build line items
        List<OrderItem> orderItems = new ArrayList<>();
        int totalWeightGrams = 0;

        for (GuestOrderRequest.GuestOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            if (!product.isActive())
                throw new CustomException("Product '" + product.getName() + "' is no longer available");
            if (product.getStockQuantity() < itemReq.getQuantity())
                throw new CustomException("Insufficient stock for '" + product.getName() + "'");

            BigDecimal price = product.getDiscountedPrice();
            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .quantity(itemReq.getQuantity())
                    .price(price)
                    .subtotal(subtotal)
                    .build());

            totalWeightGrams += product.getWeightGrams() * itemReq.getQuantity();

            product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
            product.setSoldCount(product.getSoldCount() + itemReq.getQuantity());
            productRepository.save(product);
        }

        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Shipping
        BigDecimal shippingAmount;
        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            shippingAmount = BigDecimal.ZERO;
        } else if (request.getShippingFee() != null && request.getShippingFee().compareTo(BigDecimal.ZERO) > 0) {
            shippingAmount = request.getShippingFee();
        } else {
            try {
                int cat = request.getExpressCategory() != null ? request.getExpressCategory() : 1;
                int weight = Math.max(totalWeightGrams, ShippingWeightCalculator.DEFAULT_ITEM_WEIGHT_GRAMS);
                FlashShippingRateResponse rate = flashShippingService.estimateRateManual(
                        request.getState(), request.getCity(), request.getPostalCode(), weight, cat);
                shippingAmount = rate.getShippingFee();
                if (rate.isUpCountry() && rate.getUpCountryFee() != null)
                    shippingAmount = shippingAmount.add(rate.getUpCountryFee());
            } catch (Exception e) {
                log.warn("Flash shipping failed for guest order: {}", e.getMessage());
                shippingAmount = FALLBACK_SHIPPING;
            }
        }

        BigDecimal taxAmount = subtotal.multiply(VAT_RATE).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal finalAmount = subtotal.add(shippingAmount).add(taxAmount);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .guestEmail(request.getGuestEmail())
                .guestFirstName(request.getGuestFirstName())
                .guestLastName(request.getGuestLastName())
                .guestPhone(request.getGuestPhone())
                .shippingAddress(shippingAddress)
                .paymentMethod(request.getPaymentMethod())
                .couponCode(request.getCouponCode())
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .totalAmount(subtotal)
                .discountAmount(BigDecimal.ZERO)
                .shippingAmount(shippingAmount)
                .taxAmount(taxAmount)
                .finalAmount(finalAmount)
                .items(new ArrayList<>())
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order saved = orderRepository.save(order);
        log.info("Guest order created: {} | {} | shipping: ₱{}", saved.getOrderNumber(), request.getGuestEmail(),
                shippingAmount);
        return mapToGuestOrderResponse(saved);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Shipping resolution logic
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Priority:
     * 1. Free shipping if subtotal >= ₱2,500
     * 2. Client-provided pre-calculated fee (from /shipping/estimate call)
     * 3. Live Flash Express API call using cart weight + destination
     * 4. ₱150 flat-rate fallback if Flash API is unavailable
     */
    private BigDecimal resolveShippingFee(
            BigDecimal clientFee,
            Integer expressCategory,
            Address destination,
            BigDecimal subtotal,
            List<CartItem> cartItems) {

        if (subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
            log.info("Free shipping applied (subtotal ₱{})", subtotal);
            return BigDecimal.ZERO;
        }

        if (clientFee != null && clientFee.compareTo(BigDecimal.ZERO) > 0) {
            log.info("Using client-provided shipping fee: ₱{}", clientFee);
            return clientFee;
        }

        try {
            int weightGrams = weightCalculator.calculateCartWeightGrams(cartItems);
            int category = expressCategory != null ? expressCategory : 1;

            FlashShippingRateResponse rate = flashShippingService.estimateRate(
                    destination, weightGrams, category);

            BigDecimal fee = rate.getShippingFee();
            if (rate.isUpCountry() && rate.getUpCountryFee() != null) {
                fee = fee.add(rate.getUpCountryFee());
            }

            log.info("Flash Express: ₱{} ({}g, cat {})", fee, weightGrams, category);
            return fee;

        } catch (Exception e) {
            log.warn("Flash Express unavailable, using fallback: {}", e.getMessage());
            return FALLBACK_SHIPPING;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Read / status / cancel operations (unchanged from original)
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        return mapToOrderResponse(orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id)));
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getUserOrderById(Long id) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId()))
            throw new CustomException("You can only view your own orders");
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse trackOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
        User user = getCurrentUser();
        if (!order.getUser().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN"))
            throw new CustomException("You can only track your own orders");
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse trackGuestOrder(String orderNumber, String email) {
        Order order = orderRepository.findByOrderNumberAndGuestEmail(orderNumber, email)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToGuestOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getUserOrders(Pageable pageable) {
        return orderRepository.findByUserId(getCurrentUser().getId(), pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable, OrderStatus status,
            LocalDate startDate, LocalDate endDate, String customerEmail) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        return orderRepository.findOrdersWithFilters(status, start, end, customerEmail, pageable)
                .map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getTodayOrders(Pageable pageable) {
        return orderRepository.findTodayOrders(pageable).map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomer(Long userId, Pageable pageable) {
        return orderRepository.findByUserId(userId, pageable).map(this::mapToOrderResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getRecentOrders(int limit) {
        return orderRepository.findAll(PageRequest.of(0, limit, Sort.by("createdAt").descending()))
                .stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getUserRecentOrders(int limit) {
        return orderRepository.findRecentOrdersByUserId(
                getCurrentUser().getId(),
                PageRequest.of(0, limit, Sort.by("createdAt").descending()))
                .stream().map(this::mapToOrderResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(Long id, OrderStatus status, String notes) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        OrderStatus prev = order.getStatus();
        order.setStatus(status);

        if (notes != null && !notes.isBlank())
            order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + notes : notes);

        switch (status) {
            case SHIPPED -> {
                order.setShippingCarrier("Flash Express");
                order.setTrackingNumber("FE" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
                order.setEstimatedDelivery(LocalDateTime.now().plusDays(3));
            }
            case DELIVERED -> order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                restoreStock(order);
            }
        }

        Order updated = orderRepository.save(order);
        if (prev != status)
            emailService.sendOrderStatusUpdateEmail(updated);
        log.info("Order {} status: {} → {}", order.getOrderNumber(), prev, status);
        return mapToOrderResponse(updated);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING)
            throw new CustomException("Cannot cancel order in status: " + order.getStatus());
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        restoreStock(order);
        return mapToOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse cancelUserOrder(Long id) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId()))
            throw new CustomException("You can only cancel your own orders");
        if (order.getStatus() != OrderStatus.PENDING)
            throw new CustomException("Order cannot be cancelled after processing has started");
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        restoreStock(order);
        return mapToOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderResponse requestReturn(Long id, String reason) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId()))
            throw new CustomException("You can only request returns for your own orders");
        if (order.getStatus() != OrderStatus.DELIVERED)
            throw new CustomException("Returns can only be requested for delivered orders");
        if (order.getDeliveredAt().isBefore(LocalDateTime.now().minusDays(30)))
            throw new CustomException("Return window (30 days) has expired");
        order.setStatus(OrderStatus.RETURNED);
        order.setNotes(order.getNotes() != null
                ? order.getNotes() + "\nReturn requested: " + reason
                : "Return requested: " + reason);
        return mapToOrderResponse(orderRepository.save(order));
    }

    @Override
    @Transactional(readOnly = true)
    public Object getOrderStats(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime end = endDate != null ? endDate.plusDays(1).atStartOfDay() : null;
        Map<String, Object> stats = new HashMap<>();
        BigDecimal revenue = orderRepository.getTotalRevenue(start, end);
        stats.put("totalRevenue", revenue != null ? revenue : BigDecimal.ZERO);
        Map<String, Long> counts = new HashMap<>();
        for (OrderStatus s : OrderStatus.values())
            counts.put(s.name(), Optional.ofNullable(orderRepository.countByStatus(s)).orElse(0L));
        stats.put("statusCounts", counts);
        stats.put("todayOrders", Optional.ofNullable(orderRepository.countTodayOrders()).orElse(0L));
        long total = orderRepository.count();
        stats.put("averageOrderValue", total > 0 && revenue != null
                ? revenue.divide(BigDecimal.valueOf(total), 2, BigDecimal.ROUND_HALF_UP)
                : BigDecimal.ZERO);
        return stats;
    }

    @Override
    @Transactional(readOnly = true)
    public Object getUserOrderCountsByStatus() {
        User user = getCurrentUser();
        Map<String, Long> counts = new HashMap<>();
        for (OrderStatus s : OrderStatus.values())
            counts.put(s.name(), user.getOrders().stream().filter(o -> o.getStatus() == s).count());
        return counts;
    }

    @Override
    @Transactional
    public OrderResponse createReview(Long orderId, String review, Integer rating) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        log.info("Review {} stars created for order {}", rating, order.getOrderNumber());
        return mapToOrderResponse(order);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Private helpers
    // ══════════════════════════════════════════════════════════════════════════

    private String generateOrderNumber() {
        String ts = String.valueOf(System.currentTimeMillis());
        String rand = String.format("%04d", (int) (Math.random() * 10000));
        return "ORD" + ts.substring(ts.length() - 8) + rand;
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getItems()) {
            Product p = item.getProduct();
            p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
            p.setSoldCount(Math.max(0, p.getSoldCount() - item.getQuantity()));
            productRepository.save(p);
        }
    }

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private Address resolveAddress(Long addressId, OrderRequest.AddressData data, User user) {
        if (addressId != null) {
            Address a = addressRepository.findById(addressId)
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
            if (!a.getUser().getId().equals(user.getId()))
                throw new CustomException("Address does not belong to current user");
            return a;
        }
        if (data != null) {
            return addressRepository.save(Address.builder()
                    .user(user)
                    .firstName(data.getFirstName())
                    .lastName(data.getLastName())
                    .addressLine1(data.getAddressLine1())
                    .addressLine2(data.getAddressLine2())
                    .city(data.getCity())
                    .state(data.getState())
                    .postalCode(data.getPostalCode())
                    .country(data.getCountry())
                    .phone(data.getPhone())
                    .companyName(data.getCompanyName())
                    .addressType("SHIPPING")
                    .isDefault(false)
                    .build());
        }
        throw new CustomException("Shipping address is required");
    }

    // ── Response mapping ───────────────────────────────────────────────────────

    private OrderResponse mapToOrderResponse(Order order) {
        boolean isGuest = order.getUser() == null;
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userId(isGuest ? null : order.getUser().getId())
                .userEmail(isGuest ? order.getGuestEmail() : order.getUser().getEmail())
                .userName(isGuest
                        ? order.getGuestFirstName() + " " + order.getGuestLastName()
                        : order.getUser().getFirstName() + " " + order.getUser().getLastName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingAmount(order.getShippingAmount())
                .taxAmount(order.getTaxAmount())
                .finalAmount(order.getFinalAmount())
                .shippingAddress(mapToAddressResponse(order.getShippingAddress()))
                .billingAddress(mapToAddressResponse(order.getBillingAddress()))
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .couponCode(order.getCouponCode())
                .trackingNumber(order.getTrackingNumber())
                .shippingCarrier(order.getShippingCarrier())
                .estimatedDelivery(order.getEstimatedDelivery())
                .deliveredAt(order.getDeliveredAt())
                .items(order.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList()))
                .payments(order.getPayments().stream().map(this::mapToPaymentResponse).collect(Collectors.toList()))
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private OrderResponse mapToGuestOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .userEmail(order.getGuestEmail())
                .userName(order.getGuestFirstName() + " " + order.getGuestLastName())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .shippingAmount(order.getShippingAmount())
                .taxAmount(order.getTaxAmount())
                .finalAmount(order.getFinalAmount())
                .shippingAddress(mapToAddressResponse(order.getShippingAddress()))
                .paymentMethod(order.getPaymentMethod())
                .paymentStatus(order.getPaymentStatus())
                .couponCode(order.getCouponCode())
                .items(order.getItems().stream().map(this::mapToItemResponse).collect(Collectors.toList()))
                .payments(new ArrayList<>())
                .notes(order.getNotes())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }


    private OrderResponse.OrderItemResponse mapToItemResponse(OrderItem item) {
        Product product = item.getProduct();
        String imageUrl = product.getImageUrl();
        String variationName = null;

        if (item.getVariation() != null) {
            ProductVariation variation = item.getVariation();
            if (variation.getImageUrl() != null && !variation.getImageUrl().isBlank()) {
                imageUrl = variation.getImageUrl();
            }
            variationName = variation.getName();
        }

        return OrderResponse.OrderItemResponse.builder()
                .id(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(imageUrl)
                .variationName(variationName)
                .price(item.getPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    private OrderResponse.PaymentResponse mapToPaymentResponse(Payment payment) {
        return OrderResponse.PaymentResponse.builder()
                .id(payment.getId())
                .transactionId(payment.getTransactionId())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .paymentGateway(payment.getPaymentGateway())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }

    private OrderResponse.AddressResponse mapToAddressResponse(Address address) {
        if (address == null)
            return null;
        return OrderResponse.AddressResponse.builder()
                .id(address.getId())
                .firstName(address.getFirstName())
                .lastName(address.getLastName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .postalCode(address.getPostalCode())
                .country(address.getCountry())
                .companyName(address.getCompanyName())
                .build();
    }
}