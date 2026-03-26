package com.wisecartecommerce.ecommerce.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.GuestOrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.OrderRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.CustomerTrackingResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashOrderResult;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashShippingRateResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashTrackingResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.OrderResponse;
import com.wisecartecommerce.ecommerce.entity.Address;
import com.wisecartecommerce.ecommerce.entity.Cart;
import com.wisecartecommerce.ecommerce.entity.CartItem;
import com.wisecartecommerce.ecommerce.entity.Coupon;
import com.wisecartecommerce.ecommerce.entity.CouponUsage;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.OrderItem;
import com.wisecartecommerce.ecommerce.entity.Payment;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.RateLimitException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.AddressRepository;
import com.wisecartecommerce.ecommerce.repository.CartRepository;
import com.wisecartecommerce.ecommerce.repository.CouponRepository;
import com.wisecartecommerce.ecommerce.repository.CouponUsageRepository;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.repository.ProductVariationRepository;
import com.wisecartecommerce.ecommerce.service.EmailService;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import com.wisecartecommerce.ecommerce.service.NotificationService;
import com.wisecartecommerce.ecommerce.service.OrderService;
import com.wisecartecommerce.ecommerce.service.RateLimitService;
import com.wisecartecommerce.ecommerce.util.CouponValidationResult;
import com.wisecartecommerce.ecommerce.util.CouponValidator;
import com.wisecartecommerce.ecommerce.util.OrderStatus;
import com.wisecartecommerce.ecommerce.util.ShippingWeightCalculator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    // ── Repositories ───────────────────────────────────────────────────────────
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final AddressRepository addressRepository;
    private final ProductVariationRepository productVariationRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    // ── Services ───────────────────────────────────────────────────────────────
    private final EmailService emailService;
    private final FlashExpressShippingService flashShippingService;
    private final NotificationService notificationService;

    // ── Utilities ──────────────────────────────────────────────────────────────
    private final ShippingWeightCalculator weightCalculator;
    private final CouponValidator couponValidator;
    private final RateLimitService rateLimitService;

    // ── Constants ──────────────────────────────────────────────────────────────
    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("2500");
    private static final BigDecimal VAT_RATE = new BigDecimal("0.12");
    private static final BigDecimal FALLBACK_SHIPPING = new BigDecimal("150.00");

    // ══════════════════════════════════════════════════════════════════════════
    // Authenticated order creation
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        User user = getCurrentUser();
        if (!rateLimitService.tryConsume(rateLimitService.orderBucket(user.getId().toString()))) {
            throw new RateLimitException("You are placing orders too quickly. Please wait.");
        }
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new CustomException("Cannot create order with an empty cart");
        }

        // ── Resolve addresses ──────────────────────────────────────────────────
        Address shippingAddress = resolveAddress(
                request.getShippingAddressId(), request.getShippingAddress(), user);
        Address billingAddress = (request.getBillingAddressId() != null || request.getBillingAddress() != null)
                ? resolveAddress(request.getBillingAddressId(), request.getBillingAddress(), user)
                : shippingAddress;

        // ── Validate stock + build line items ─────────────────────────────────
        List<OrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> stockUpdates = new HashMap<>();
        Map<Long, Integer> variationStockUpdates = new HashMap<>();

        for (CartItem cartItem : cart.getItems()) {
            Product product = cartItem.getProduct();
            ProductVariation variation = cartItem.getVariation();

            if (!product.isActive()) {
                throw new CustomException("Product '" + product.getName() + "' is no longer available");
            }

            if (variation != null) {
                if (!variation.isActive()) {
                    throw new CustomException("Variation '" + variation.getName() + "' is no longer available");
                }
                if (variation.getStockQuantity() < cartItem.getQuantity()) {
                    throw new CustomException("Insufficient stock for '" + product.getName()
                            + " (" + variation.getName() + ")'. Available: " + variation.getStockQuantity());
                }
                variationStockUpdates.put(variation.getId(),
                        variation.getStockQuantity() - cartItem.getQuantity());
            } else {
                if (product.getStockQuantity() < cartItem.getQuantity()) {
                    throw new CustomException("Insufficient stock for '" + product.getName()
                            + "'. Available: " + product.getStockQuantity());
                }
                stockUpdates.put(product.getId(), product.getStockQuantity() - cartItem.getQuantity());
            }

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .variation(variation)
                    .quantity(cartItem.getQuantity())
                    .price(cartItem.getPrice())
                    .subtotal(cartItem.getSubtotal())
                    .isAddon(cartItem.isAddon())
                    .addonProduct(cartItem.getAddonProduct())
                    .addonProductAddOn(cartItem.getAddonProductAddOn())
                    .addonVariation(cartItem.getAddonVariation())
                    .addonPrice(cartItem.getAddonPrice())
                    .build();

            orderItems.add(orderItem);
            product.setSoldCount(product.getSoldCount() + cartItem.getQuantity());
        }

        BigDecimal subtotal = cart.getSubtotal();

        // ── Coupon ────────────────────────────────────────────────────────────
        String couponCode = cart.getCouponCode();
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean couponFreeShipping = false;
        CouponValidationResult couponResult = null;

        if (couponCode != null && !couponCode.isBlank()) {
            try {
                couponResult = couponValidator.validate(couponCode, subtotal, user.getId());
                discountAmount = couponResult.getDiscountAmount();
                couponFreeShipping = couponResult.isFreeShipping();
                log.info("Coupon '{}' applied: ₱{} discount", couponCode, discountAmount);
            } catch (CustomException e) {
                // Coupon became invalid between cart-apply and checkout — just skip it
                log.warn("Coupon '{}' invalid at checkout: {}", couponCode, e.getMessage());
                couponCode = null;
            }
        }

        // ── Shipping ──────────────────────────────────────────────────────────
        BigDecimal shippingAmount;
        if (couponFreeShipping) {
            shippingAmount = BigDecimal.ZERO;
            log.info("Free shipping applied via coupon '{}'", couponCode);
        } else {
            shippingAmount = resolveShippingFee(
                    request.getShippingFee(),
                    request.getExpressCategory(),
                    shippingAddress,
                    subtotal,
                    cart.getItems());
        }

        // ── Tax (12% VAT on subtotal after discount) ───────────────────────────
        BigDecimal taxableAmount = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);
        BigDecimal taxAmount = taxableAmount
                .multiply(VAT_RATE)
                .setScale(2, BigDecimal.ROUND_HALF_UP);

        BigDecimal finalAmount = taxableAmount.add(shippingAmount).add(taxAmount);

        // ── Build + persist order ──────────────────────────────────────────────
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .user(user)
                .shippingAddress(shippingAddress)
                .billingAddress(billingAddress)
                .paymentMethod(request.getPaymentMethod())
                .couponCode(couponCode)
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .totalAmount(subtotal)
                .discountAmount(discountAmount)
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
        orderRepository.flush();

        int weightGrams = weightCalculator.calculateCartWeightGrams(cart.getItems());
        int category = request.getExpressCategory() != null ? request.getExpressCategory() : 1;
        assignFlashOrderNumber(saved, shippingAddress, weightGrams, category);
        log.info("Final order number after Flash assignment: {}", saved.getOrderNumber());
        saved = orderRepository.save(saved);

        // Update stock
        for (Map.Entry<Long, Integer> entry : stockUpdates.entrySet()) {
            productRepository.findById(entry.getKey()).ifPresent(p -> {
                p.setStockQuantity(entry.getValue());
                productRepository.save(p);
            });
        }
        for (Map.Entry<Long, Integer> entry : variationStockUpdates.entrySet()) {
            productVariationRepository.findById(entry.getKey()).ifPresent(v -> {
                v.setStockQuantity(entry.getValue());
                productVariationRepository.save(v);
            });
        }

        // ── Record coupon usage + increment counter ────────────────────────────
        if (couponResult != null) {
            recordCouponUsage(couponResult.getCoupon(), user, null, saved);
        }

        boolean isMaya = "maya".equalsIgnoreCase(request.getPaymentMethod());

        if (!isMaya) {
            cart.getItems().clear();
            cart.setSubtotal(BigDecimal.ZERO);
            cart.setTotal(BigDecimal.ZERO);
            cart.setDiscountAmount(BigDecimal.ZERO);
            cart.setCouponCode(null);
            cart.setCouponDiscountAmount(BigDecimal.ZERO);
            cartRepository.save(cart);

            emailService.sendOrderConfirmationEmail(saved);
            notificationService.createNotification(
                    user,
                    "Order Placed Successfully",
                    "Your order #" + saved.getOrderNumber() + " has been placed and is being processed.",
                    "ORDER",
                    saved.getId(),
                    "ORDER");
            notificationService.createAdminNotification(
                    "New Order Received",
                    "Order #" + saved.getOrderNumber() + " was placed by " + user.getEmail() + ".",
                    "ORDER",
                    saved.getId(),
                    "ORDER");
        }
        log.info("Order created: {} | user: {} | discount: ₱{} | shipping: ₱{}",
                saved.getOrderNumber(), user.getEmail(), discountAmount, shippingAmount);

        return mapToOrderResponse(saved);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Guest order creation
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    @Transactional
    @SuppressWarnings("null")
    public OrderResponse createGuestOrder(GuestOrderRequest request) {
        if (!rateLimitService.tryConsume(rateLimitService.orderBucket(request.getGuestEmail()))) {
            throw new RateLimitException("Too many orders from this email. Please wait.");
        }

        // ── Save shipping address ─────────────────────────────────────────────
        Address shippingAddress = addressRepository.save(Address.builder()
                .firstName(request.getGuestFirstName())
                .lastName(request.getGuestLastName())
                .phone(request.getGuestPhone() != null ? request.getGuestPhone() : request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .addressType("SHIPPING")
                .isDefault(false)
                .build());

        // ── Build line items ──────────────────────────────────────────────────
        List<OrderItem> orderItems = new ArrayList<>();
        int totalWeightGrams = 0;

        for (GuestOrderRequest.GuestOrderItemRequest itemReq : request.getItems()) {
            Product product = productRepository.findById(itemReq.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + itemReq.getProductId()));

            if (!product.isActive()) {
                throw new CustomException("Product '" + product.getName() + "' is no longer available");
            }

            BigDecimal price;
            ProductVariation variation = null;

            if (itemReq.getVariationId() != null) {
                variation = productVariationRepository.findById(itemReq.getVariationId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                        "Variation not found: " + itemReq.getVariationId()));

                if (!variation.isActive()) {
                    throw new CustomException("Variation '" + variation.getName() + "' is no longer available");
                }
                if (variation.getStockQuantity() < itemReq.getQuantity()) {
                    throw new CustomException("Insufficient stock for '" + product.getName()
                            + " (" + variation.getName() + ")'. Available: " + variation.getStockQuantity());
                }

                variation.setStockQuantity(variation.getStockQuantity() - itemReq.getQuantity());
                productVariationRepository.save(variation);
                price = (variation.getPrice() != null) ? variation.getPrice() : product.getDiscountedPrice();
            } else {
                if (product.getStockQuantity() < itemReq.getQuantity()) {
                    throw new CustomException("Insufficient stock for '" + product.getName()
                            + "'. Available: " + product.getStockQuantity());
                }
                product.setStockQuantity(product.getStockQuantity() - itemReq.getQuantity());
                price = product.getDiscountedPrice();
            }

            BigDecimal itemSubtotal = price.multiply(BigDecimal.valueOf(itemReq.getQuantity()));

            orderItems.add(OrderItem.builder()
                    .product(product)
                    .variation(variation)
                    .quantity(itemReq.getQuantity())
                    .price(price)
                    .subtotal(itemSubtotal)
                    .build());

            totalWeightGrams += product.getWeightGrams() * itemReq.getQuantity();
            product.setSoldCount(product.getSoldCount() + itemReq.getQuantity());
            productRepository.save(product);
        }

        BigDecimal subtotal = orderItems.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        String couponCode = request.getCouponCode();
        BigDecimal discountAmount = BigDecimal.ZERO;
        boolean couponFreeShipping = false;
        CouponValidationResult couponResult = null;

        if (couponCode != null && !couponCode.isBlank()) {
            try {
                couponResult = couponValidator.validate(couponCode, subtotal, null); // null = guest
                discountAmount = couponResult.getDiscountAmount();
                couponFreeShipping = couponResult.isFreeShipping();
                log.info("Guest coupon '{}' applied: ₱{} discount", couponCode, discountAmount);
            } catch (CustomException e) {
                throw new CustomException("Coupon error: " + e.getMessage());
            }
        }

        // ── Shipping ──────────────────────────────────────────────────────────
        BigDecimal shippingAmount;
        BigDecimal taxableSubtotal = subtotal.subtract(discountAmount).max(BigDecimal.ZERO);

        if (couponFreeShipping) {
            shippingAmount = BigDecimal.ZERO;
        } else if (taxableSubtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0) {
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
                if (rate.isUpCountry() && rate.getUpCountryFee() != null) {
                    shippingAmount = shippingAmount.add(rate.getUpCountryFee());
                }
            } catch (Exception e) {
                log.warn("Flash shipping failed for guest order: {}", e.getMessage());
                shippingAmount = FALLBACK_SHIPPING;
            }
        }

        BigDecimal taxAmount = taxableSubtotal.multiply(VAT_RATE).setScale(2, BigDecimal.ROUND_HALF_UP);
        BigDecimal finalAmount = taxableSubtotal.add(shippingAmount).add(taxAmount);

        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .guestEmail(request.getGuestEmail())
                .guestFirstName(request.getGuestFirstName())
                .guestLastName(request.getGuestLastName())
                .guestPhone(request.getGuestPhone())
                .shippingAddress(shippingAddress)
                .paymentMethod(request.getPaymentMethod())
                .couponCode(couponCode)
                .notes(request.getNotes())
                .status(OrderStatus.PENDING)
                .totalAmount(subtotal)
                .discountAmount(discountAmount)
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
        orderRepository.flush();

        int category = request.getExpressCategory() != null ? request.getExpressCategory() : 1;
        int weight = Math.max(totalWeightGrams, ShippingWeightCalculator.DEFAULT_ITEM_WEIGHT_GRAMS);
        assignFlashOrderNumber(saved, shippingAddress, weight, category);
        log.info("Final order number after Flash assignment: {}", saved.getOrderNumber());
        saved = orderRepository.save(saved);

        if (couponResult != null) {
            recordCouponUsage(couponResult.getCoupon(), null, request.getGuestEmail(), saved);
        }

        emailService.sendOrderConfirmationEmail(saved);
        notificationService.createAdminNotification(
                "New Guest Order Received",
                "Guest order #" + saved.getOrderNumber() + " was placed by " + request.getGuestEmail() + ".",
                "ORDER",
                saved.getId(),
                "ORDER");
        log.info("Guest order created: {} | {} | discount: ₱{} | shipping: ₱{}",
                saved.getOrderNumber(), request.getGuestEmail(), discountAmount, shippingAmount);
        return mapToGuestOrderResponse(saved);
    }

    private void recordCouponUsage(Coupon coupon, User user, String guestEmail, Order order) {
        coupon.setCurrentUsageCount(coupon.getCurrentUsageCount() + 1);
        couponRepository.save(coupon);
        if (user != null) {
            CouponUsage usage = CouponUsage.builder()
                    .user(user)
                    .coupon(coupon)
                    .order(order)
                    .build();
            couponUsageRepository.save(usage);
            log.info("Coupon usage recorded: {} | user: {}", coupon.getCode(), user.getEmail());
        } else {
            log.info("Coupon usage recorded (global only): {} | guest: {}", coupon.getCode(), guestEmail);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Shipping resolution
    // ══════════════════════════════════════════════════════════════════════════
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
            FlashShippingRateResponse rate = flashShippingService.estimateRate(destination, weightGrams, category);
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
    // Read / status / cancel operations
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
        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only view your own orders");
        }
        return mapToOrderResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse trackOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));
        User user = getCurrentUser();
        if (!order.getUser().getId().equals(user.getId()) && !user.getRole().name().equals("ADMIN")) {
            throw new CustomException("You can only track your own orders");
        }
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

        if (notes != null && !notes.isBlank()) {
            order.setNotes(order.getNotes() != null ? order.getNotes() + "\n" + notes : notes);
        }

        switch (status) {
            case SHIPPED -> {
                order.setShippingCarrier("Flash Express");
                if (order.getTrackingNumber() == null || order.getTrackingNumber().isBlank()) {
                    order.setTrackingNumber("FE" + UUID.randomUUID().toString().substring(0, 12).toUpperCase());
                }
                order.setEstimatedDelivery(LocalDateTime.now().plusDays(3));
            }
            case DELIVERED ->
                order.setDeliveredAt(LocalDateTime.now());
            case CANCELLED -> {
                order.setCancelledAt(LocalDateTime.now());
                restoreStock(order);
            }
        }

        Order updated = orderRepository.save(order);
        if (prev != status) {
            emailService.sendOrderStatusUpdateEmail(updated);
            if (updated.getUser() != null) {
                String title = "Order Status Updated";
                String message = switch (status) {
                    case PROCESSING ->
                        "Your order #" + updated.getOrderNumber() + " is now being processed.";
                    case SHIPPED ->
                        "Your order #" + updated.getOrderNumber() + " has been shipped via Flash Express.";
                    case DELIVERED ->
                        "Your order #" + updated.getOrderNumber() + " has been delivered. Enjoy!";
                    case CANCELLED ->
                        "Your order #" + updated.getOrderNumber() + " has been cancelled.";
                    default ->
                        "Your order #" + updated.getOrderNumber() + " status changed to " + status.name();
                };
                notificationService.createNotification(
                        updated.getUser(), title, message, "ORDER", updated.getId(), "ORDER");
            }
        }
        log.info("Order {} status: {} → {}", order.getOrderNumber(), prev, status);
        return mapToOrderResponse(updated);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new CustomException("Cannot cancel order in status: " + order.getStatus());
        }
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

        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only cancel your own orders");
        }

        // Allow cancellation while PENDING or PROCESSING (before pickup)
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PROCESSING) {
            throw new CustomException("Order cannot be cancelled once it has been shipped");
        }

        String pno = order.getTrackingNumber();

        // Fallback: old orders may have null trackingNumber but PNO stored in
        // orderNumber
        if ((pno == null || pno.isBlank() || !pno.startsWith("P"))
                && order.getOrderNumber() != null
                && order.getOrderNumber().startsWith("P")) {
            pno = order.getOrderNumber();
            log.warn("trackingNumber null for order id={}, falling back to orderNumber as PNO={}",
                    order.getId(), pno);
        }

        // ── 1. Check Flash state to block if already picked up ───────────────────
        if (pno != null && pno.startsWith("P")) {
            try {
                FlashTrackingResponse flashData = flashShippingService.trackOrder(pno);
                if (flashData != null && flashData.getState() != null && flashData.getState() >= 50) {
                    throw new CustomException(
                            "Your order has already been picked up by the courier and cannot be cancelled.");
                }
            } catch (CustomException e) {
                throw e; // rethrow pickup-blocking errors
            } catch (Exception e) {
                // Flash tracking unavailable — proceed cautiously but log it
                log.warn("Could not verify Flash state for PNO={}, proceeding with cancellation: {}", pno,
                        e.getMessage());
            }
        }

        // ── 2. Cancel on Flash Express first ─────────────────────────────────────
        if (pno != null && pno.startsWith("P")) {
            try {
                flashShippingService.cancelOrder(pno);
                log.info("Flash Express order cancelled for PNO={}, order={}", pno, order.getOrderNumber());
            } catch (CustomException e) {
                // Flash returned a business error (e.g. already picked up on their end)
                String msg = e.getMessage() != null ? e.getMessage() : "";
                if (msg.contains("1015") || msg.toLowerCase().contains("picked up")) {
                    throw new CustomException(
                            "Your order has already been picked up by the courier and cannot be cancelled.");
                }
                // Other Flash errors — log but don't block local cancellation
                log.warn("Flash Express cancel returned error for PNO={}: {}", pno, msg);
            } catch (Exception e) {
                log.warn("Flash Express cancel exception for PNO={}: {}", pno, e.getMessage());
                // Don't block local cancellation if Flash is temporarily unavailable
            }
        }

        // ── 3. Cancel locally ────────────────────────────────────────────────────
        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledAt(LocalDateTime.now());
        restoreStock(order);

        Order saved = orderRepository.save(order);
        log.info("Order {} cancelled by customer {}", order.getOrderNumber(), user.getEmail());
        return mapToOrderResponse(saved);
    }

    @Override
    @Transactional
    public OrderResponse requestReturn(Long id, String reason) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only request returns for your own orders");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new CustomException("Returns can only be requested for delivered orders");
        }
        if (order.getDeliveredAt().isBefore(LocalDateTime.now().minusDays(30))) {
            throw new CustomException("Return window (30 days) has expired");
        }
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
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s.name(), Optional.ofNullable(orderRepository.countByStatus(s)).orElse(0L));
        }
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
        for (OrderStatus s : OrderStatus.values()) {
            counts.put(s.name(), user.getOrders().stream().filter(o -> o.getStatus() == s).count());
        }
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
            if (!a.getUser().getId().equals(user.getId())) {
                throw new CustomException("Address does not belong to current user");
            }
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
                .isAddon(item.isAddon())
                .addonPrice(item.getAddonPrice())
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
        if (address == null) {
            return null;
        }
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

    private void assignFlashOrderNumber(Order saved, Address shippingAddress, int weightGrams, int expressCategory) {
        try {
            FlashOrderResult flashOrder = flashShippingService.createOrder(
                    saved, shippingAddress, weightGrams, expressCategory);

            if (flashOrder != null && flashOrder.getPno() != null) {
                saved.setOrderNumber(flashOrder.getPno());
                saved.setTrackingNumber(flashOrder.getPno());
                saved.setShippingCarrier("Flash Express");
                log.info("Flash order created: PNO={}", flashOrder.getPno());
            } else {
                saved.setOrderNumber(generateOrderNumber());
                log.warn("Flash order creation returned null, using local order number");
            }
        } catch (Exception e) {
            saved.setOrderNumber(generateOrderNumber());
            log.warn("Flash order creation failed, using local order number: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerTrackingResponse getCustomerTracking(String orderNumber) {
        // 1. Load and verify ownership
        User user = getCurrentUser();
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderNumber));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new CustomException("You can only track your own orders");
        }

        String pno = order.getTrackingNumber();

        // 2. Build base response — always returned even without a PNO
        CustomerTrackingResponse.CustomerTrackingResponseBuilder builder = CustomerTrackingResponse.builder()
                .orderNumber(order.getOrderNumber())
                .trackingNumber(pno)
                .shippingCarrier(order.getShippingCarrier())
                .orderStatus(order.getStatus());

        // 3. If no PNO yet (order still being packed), return without Flash data
        if (pno == null || pno.isBlank()) {
            return builder.flashTracking(null).build();
        }

        // 4. Fetch live Flash tracking
        try {
            FlashTrackingResponse flashData = flashShippingService.trackOrder(pno);

            CustomerTrackingResponse.FlashTracking flashTracking = CustomerTrackingResponse.FlashTracking.builder()
                    .pno(flashData.getPno())
                    .origPno(flashData.getOrigPno())
                    .returnedPno(flashData.getReturnedPno())
                    .state(flashData.getState())
                    .stateText(flashData.getStateText())
                    .stateChangeAt(flashData.getStateChangeAt())
                    .routes(flashData.getRoutes())
                    .build();

            builder.flashTracking(flashTracking);

        } catch (Exception e) {
            // Flash API unavailable — still return order info, just no live tracking
            log.warn("Flash tracking unavailable for PNO={}: {}", pno, e.getMessage());
            builder.flashTracking(null);
        }

        return builder.build();
    }
}
