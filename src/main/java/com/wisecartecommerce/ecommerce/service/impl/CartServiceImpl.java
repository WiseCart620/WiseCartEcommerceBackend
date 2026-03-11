package com.wisecartecommerce.ecommerce.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.wisecartecommerce.ecommerce.Dto.Request.CartCheckRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartItemRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartItemUpdateRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartMergeRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CartRequest;
import com.wisecartecommerce.ecommerce.Dto.Request.CouponApplyRequest;
import com.wisecartecommerce.ecommerce.Dto.Response.CartResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashShippingRateResponse;
import com.wisecartecommerce.ecommerce.entity.Address;
import com.wisecartecommerce.ecommerce.entity.Cart;
import com.wisecartecommerce.ecommerce.entity.CartItem;
import com.wisecartecommerce.ecommerce.entity.Coupon;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.ProductAddOn;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import com.wisecartecommerce.ecommerce.entity.SavedCart;
import com.wisecartecommerce.ecommerce.entity.SavedCartItem;
import com.wisecartecommerce.ecommerce.entity.User;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.exception.ResourceNotFoundException;
import com.wisecartecommerce.ecommerce.repository.AddressRepository;
import com.wisecartecommerce.ecommerce.repository.CartItemRepository;
import com.wisecartecommerce.ecommerce.repository.CartRepository;
import com.wisecartecommerce.ecommerce.repository.CouponRepository;
import com.wisecartecommerce.ecommerce.repository.CouponUsageRepository;
import com.wisecartecommerce.ecommerce.repository.ProductAddOnRepository;
import com.wisecartecommerce.ecommerce.repository.ProductRepository;
import com.wisecartecommerce.ecommerce.repository.ProductVariationRepository;
import com.wisecartecommerce.ecommerce.repository.SavedCartRepository;
import com.wisecartecommerce.ecommerce.service.CartService;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import com.wisecartecommerce.ecommerce.util.ShippingWeightCalculator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductAddOnRepository productAddOnRepository;
    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;
    private final SavedCartRepository savedCartRepository;
    private final ProductVariationRepository productVariationRepository;
    private final AddressRepository addressRepository;
    private final FlashExpressShippingService flashShippingService;
    private final ShippingWeightCalculator weightCalculator;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Override
    @Transactional
    public CartResponse getCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseGet(() -> createNewCart(user));
        return mapToCartResponse(cart);
    }

    @Override
    @Transactional
    public CartResponse addToCart(CartItemRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createNewCart(user));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        if (!product.isActive()) {
            throw new CustomException("Product is not available");
        }

        BigDecimal itemPrice;
        BigDecimal itemOriginalPrice;
        ProductVariation variation = null;

        if (request.getVariationId() != null) {
            variation = productVariationRepository.findById(request.getVariationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Variation not found"));

            if (!variation.isActive()) {
                throw new CustomException("Product variation is not available");
            }
            if (variation.getStockQuantity() < request.getQuantity()) {
                throw new CustomException("Insufficient stock. Available: " + variation.getStockQuantity());
            }

            itemPrice = variation.getDiscountedPrice() != null ? variation.getDiscountedPrice() : variation.getPrice();
            itemOriginalPrice = variation.getPrice();
        } else {
            if (product.getStockQuantity() < request.getQuantity()) {
                throw new CustomException("Insufficient stock. Available: " + product.getStockQuantity());
            }
            itemPrice = product.getDiscountedPrice() != null ? product.getDiscountedPrice() : product.getPrice();
            itemOriginalPrice = product.getPrice();
        }

        // For regular (non-addon) items, merge with existing cart item if present
        if (request.getAddonProductAddOnId() == null) {
            CartItem existingItem = request.getVariationId() != null
                    ? getItemByProductAndVariation(cart, request.getProductId(), request.getVariationId())
                    : cart.getItemByProductId(request.getProductId());

            if (existingItem != null) {
                int newQuantity = existingItem.getQuantity() + request.getQuantity();
                if (newQuantity > 100) {
                    throw new CustomException("Maximum quantity per item is 100");
                }
                existingItem.setQuantity(newQuantity);
                existingItem.setPrice(itemPrice);
                existingItem.setOriginalPrice(itemOriginalPrice);
            } else {
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(request.getQuantity())
                        .price(itemPrice)
                        .originalPrice(itemOriginalPrice)
                        .variation(variation)
                        .build();
                cart.getItems().add(newItem);
            }
        } else {
            // ── Add-on item: always add as a separate line item ────────────────
            ProductAddOn addOn = productAddOnRepository.findById(request.getAddonProductAddOnId())
                    .orElseThrow(() -> new ResourceNotFoundException("Add-on not found"));

            BigDecimal addonPrice;
            ProductVariation addonVariation = null;

            if (request.getAddonVariationId() != null) {
                addonVariation = productVariationRepository.findById(request.getAddonVariationId())
                        .orElseThrow(() -> new ResourceNotFoundException("Add-on variation not found"));
                addonPrice = addonVariation.getDiscountedPrice() != null
                        ? addonVariation.getDiscountedPrice()
                        : addonVariation.getPrice();
            } else {
                Product ap = addOn.getAddOnProduct();
                BigDecimal orig = ap.getDiscountedPrice() != null ? ap.getDiscountedPrice() : ap.getPrice();
                addonPrice = (addOn.getSpecialPrice() != null && addOn.getSpecialPrice().compareTo(orig) < 0)
                        ? addOn.getSpecialPrice()
                        : orig;
            }

            CartItem addonItem = CartItem.builder()
                    .cart(cart)
                    .product(addOn.getAddOnProduct())
                    .variation(addonVariation)
                    .quantity(1)
                    .price(addonPrice)
                    .originalPrice(addonPrice)
                    .isAddon(true)
                    .addonProduct(addOn.getAddOnProduct())
                    .addonProductAddOn(addOn)
                    .addonVariation(addonVariation)
                    .addonPrice(addonPrice)
                    .build();
            cart.getItems().add(addonItem);
        }

        cart.calculateTotals();
        Cart savedCart = cartRepository.save(cart);

        log.info("Added product {} to cart for user {}", request.getProductId(), user.getEmail());
        return mapToCartResponse(savedCart);
    }

    // Returns an existing non-addon cart item matching both product and variation
    private CartItem getItemByProductAndVariation(Cart cart, Long productId, Long variationId) {
        return cart.getItems().stream()
                .filter(item -> !item.isAddon()
                        && item.getProduct().getId().equals(productId)
                        && !item.getSavedForLater()
                        && item.getVariation() != null
                        && item.getVariation().getId().equals(variationId))
                .findFirst()
                .orElse(null);
    }

    @Override
    @Transactional
    public CartResponse updateCartItem(Long itemId, CartItemUpdateRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        Integer quantity = request.getQuantity();

        if (quantity <= 0) {
            cart.getItems().remove(cartItem);
            cartItemRepository.delete(cartItem);
        } else {
            if (quantity > 100) {
                throw new CustomException("Maximum quantity per item is 100");
            }

            int availableStock;
            if (cartItem.getVariation() != null) {
                availableStock = cartItem.getVariation().getStockQuantity() != null
                        ? cartItem.getVariation().getStockQuantity()
                        : 0;
            } else {
                availableStock = cartItem.getProduct().getStockQuantity();
            }
            if (availableStock < quantity) {
                throw new CustomException("Insufficient stock. Available: " + availableStock);
            }

            cartItem.setQuantity(quantity);

            if (request.getGiftWrap() != null)
                cartItem.setGiftWrap(request.getGiftWrap());
            if (request.getGiftMessage() != null)
                cartItem.setGiftMessage(request.getGiftMessage());
            if (request.getNotes() != null)
                cartItem.setNotes(request.getNotes());
        }

        cart.calculateTotals();
        Cart savedCart = cartRepository.save(cart);

        log.info("Updated cart item {} quantity to {} for user {}", itemId, quantity, user.getEmail());
        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse removeFromCart(Long itemId) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        CartItem cartItem = cart.getItems().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);
        cart.calculateTotals();
        Cart savedCart = cartRepository.save(cart);

        log.info("Removed cart item {} for user {}", itemId, user.getEmail());
        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse batchUpdateCart(CartRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (Boolean.TRUE.equals(request.getClearExistingItems())) {
            cartItemRepository.deleteByCartId(cart.getId());
            cart.getItems().clear();
        }

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (CartRequest.CartItemRequest itemRequest : request.getItems()) {
                Product product = productRepository.findById(itemRequest.getProductId())
                        .orElseThrow(() -> new ResourceNotFoundException(
                                "Product not found with id: " + itemRequest.getProductId()));

                if (!product.isActive()) {
                    throw new CustomException("Product " + product.getName() + " is not available");
                }

                if (product.getStockQuantity() < itemRequest.getQuantity()) {
                    throw new CustomException("Insufficient stock for " + product.getName() + ". Available: "
                            + product.getStockQuantity());
                }

                CartItem existingItem = cart.getItemByProductId(itemRequest.getProductId());

                if (existingItem != null) {
                    existingItem.setQuantity(itemRequest.getQuantity());
                    existingItem.setNotes(itemRequest.getNotes());
                    existingItem.setGiftWrap(itemRequest.getGiftWrap());
                    existingItem.setGiftMessage(itemRequest.getGiftMessage());
                } else {
                    CartItem newItem = CartItem.builder()
                            .cart(cart)
                            .product(product)
                            .quantity(itemRequest.getQuantity())
                            .price(product.getDiscountedPrice() != null ? product.getDiscountedPrice()
                                    : product.getPrice())
                            .originalPrice(product.getPrice())
                            .notes(itemRequest.getNotes())
                            .giftWrap(itemRequest.getGiftWrap())
                            .giftMessage(itemRequest.getGiftMessage())
                            .build();
                    cart.getItems().add(newItem);
                }
            }
        }

        if (request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty()) {
            applyCouponToCart(cart, request.getCouponCode());
        }

        cart.calculateTotals();
        Cart savedCart = cartRepository.save(cart);
        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional
    public void clearCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setTotal(BigDecimal.ZERO);
        cart.setDiscountAmount(BigDecimal.ZERO);
        cart.setCouponCode(null);
        cart.setCouponDiscountAmount(BigDecimal.ZERO);
        cartRepository.save(cart);

        log.info("Cart cleared for user {}", user.getEmail());
    }

    @Override
    @Transactional
    public CartResponse applyCoupon(CouponApplyRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        String couponCode = request.getCouponCode();
        if (couponCode == null || couponCode.trim().isEmpty()) {
            throw new CustomException("Coupon code is required");
        }

        Coupon coupon = validateCouponFully(couponCode.trim().toUpperCase(), cart, user);

        if (Boolean.TRUE.equals(request.getValidateOnly())) {
            log.info("Validated coupon {} for user {}", couponCode, user.getEmail());
            return mapToCartResponse(cart);
        }

        applyCouponToCart(cart, coupon);
        Cart savedCart = cartRepository.save(cart);

        log.info("Applied coupon {} to cart for user {}", couponCode, user.getEmail());
        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional
    public CartResponse removeCoupon() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.clearCoupon();
        Cart savedCart = cartRepository.save(cart);

        log.info("Removed coupon from cart for user {}", user.getEmail());
        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse validateCart(CartCheckRequest request) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        Map<String, String> warnings = new HashMap<>();
        Map<String, String> errors = new HashMap<>();
        boolean isValid = true;

        if (Boolean.TRUE.equals(request.getCheckStock())) {
            for (CartItem item : cart.getItems()) {
                Product product = item.getProduct();
                if (!product.isActive()) {
                    errors.put("product_" + product.getId(), product.getName() + " is no longer available");
                    isValid = false;
                } else if (product.getStockQuantity() < item.getQuantity()) {
                    if (product.getStockQuantity() == 0) {
                        errors.put("stock_" + product.getId(), product.getName() + " is out of stock");
                        isValid = false;
                    } else {
                        warnings.put("stock_" + product.getId(),
                                product.getName() + " has limited stock. Only " + product.getStockQuantity()
                                        + " available");
                    }
                }
            }
        }

        if (Boolean.TRUE.equals(request.getCheckPrices())) {
            for (CartItem item : cart.getItems()) {
                Product product = item.getProduct();
                BigDecimal currentPrice = product.getDiscountedPrice() != null ? product.getDiscountedPrice()
                        : product.getPrice();
                if (item.getPrice().compareTo(currentPrice) != 0) {
                    warnings.put("price_" + product.getId(),
                            product.getName() + " price has changed from " + item.getPrice() + " to " + currentPrice);
                }
            }
        }

        if (Boolean.TRUE.equals(request.getCheckCoupon()) && cart.getCouponCode() != null) {
            try {
                validateCouponFully(cart.getCouponCode(), cart, user);
            } catch (CustomException e) {
                warnings.put("coupon", e.getMessage());
            }
        }

        CartResponse response = mapToCartResponse(cart);
        response.setWarnings(warnings);
        response.setErrors(errors);
        response.setIsValidForCheckout(isValid);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Integer getCartItemCount() {
        User user = getCurrentUser();
        return cartRepository.findByUserId(user.getId())
                .map(Cart::getItemCount)
                .orElse(0);
    }

    @Override
    @Transactional
    public CartResponse mergeCart(CartMergeRequest request) {
        User user = getCurrentUser();
        Cart userCart = cartRepository.findByUserId(user.getId())
                .orElseGet(() -> createNewCart(user));

        String guestCartId = request.getGuestCartId() != null ? request.getGuestCartId() : request.getGuestSessionId();
        Cart guestCart = cartRepository.findBySessionId(guestCartId).orElse(null);

        if (guestCart != null && !guestCart.getItems().isEmpty()) {
            if (Boolean.TRUE.equals(request.getReplaceExisting())) {
                cartItemRepository.deleteByCartId(userCart.getId());
                userCart.getItems().clear();
            }

            for (CartItem guestItem : guestCart.getItems()) {
                CartItem existingItem = userCart.getItemByProductId(guestItem.getProduct().getId());
                if (existingItem != null) {
                    if (Boolean.TRUE.equals(request.getMergeConflicts())) {
                        int newQuantity = existingItem.getQuantity() + guestItem.getQuantity();
                        existingItem.setQuantity(Math.min(newQuantity, 100));
                    }
                } else {
                    CartItem newItem = CartItem.builder()
                            .cart(userCart)
                            .product(guestItem.getProduct())
                            .quantity(guestItem.getQuantity())
                            .price(guestItem.getPrice())
                            .originalPrice(guestItem.getOriginalPrice())
                            .build();
                    userCart.getItems().add(newItem);
                }
            }

            cartItemRepository.deleteByCartId(guestCart.getId());
            cartRepository.delete(guestCart);
        }

        userCart.calculateTotals();
        Cart savedCart = cartRepository.save(userCart);
        return mapToCartResponse(savedCart);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> estimateShipping(Long addressId, String country, String postalCode) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        Map<String, Object> result = new HashMap<>();
        BigDecimal freeThreshold = new BigDecimal("2500");

        result.put("freeShippingThreshold", freeThreshold);
        result.put("subtotal", cart.getSubtotal());

        if (cart.getSubtotal().compareTo(freeThreshold) >= 0) {
            result.put("isEligibleForFreeShipping", true);
            result.put("standardShipping", BigDecimal.ZERO);
            result.put("onTimeShipping", BigDecimal.ZERO);
            result.put("message", "Your order qualifies for free shipping!");
            return result;
        }

        result.put("isEligibleForFreeShipping", false);

        String province = null, city = null, postal = postalCode;

        if (addressId != null) {
            Address address = addressRepository.findById(addressId).orElse(null);
            if (address != null) {
                if (address.getUser() != null && !address.getUser().getId().equals(user.getId())) {
                    throw new CustomException("Address does not belong to current user");
                }
                province = address.getState();
                city = address.getCity();
                postal = address.getPostalCode();
            }
        }

        if (province == null || city == null || postal == null) {
            result.put("standardShipping", new BigDecimal("150.00"));
            result.put("onTimeShipping", new BigDecimal("300.00"));
            result.put("note", "Enter your full address to see exact shipping rates");
            result.put("estimatedDeliveryDays", Map.of("standard", "3-5", "onTime", "1-2"));
            return result;
        }

        int totalWeightGrams = weightCalculator.calculateCartWeightGrams(cart.getItems());
        result.put("totalWeightGrams", totalWeightGrams);

        try {
            FlashShippingRateResponse standard = flashShippingService.estimateRateManual(
                    province, city, postal, totalWeightGrams, 1);

            BigDecimal standardFee = standard.getShippingFee();
            if (standard.isUpCountry() && standard.getUpCountryFee() != null)
                standardFee = standardFee.add(standard.getUpCountryFee());

            result.put("standardShipping", standardFee);
            result.put("upCountry", standard.isUpCountry());
            result.put("upCountryFee", standard.getUpCountryFee());
            result.put("pricePolicyText", standard.getPricePolicyText());
            result.put("expressLabel", standard.getExpressLabel());

            try {
                FlashShippingRateResponse onTime = flashShippingService.estimateRateManual(
                        province, city, postal, totalWeightGrams, 2);
                BigDecimal onTimeFee = onTime.getShippingFee();
                if (onTime.isUpCountry() && onTime.getUpCountryFee() != null)
                    onTimeFee = onTimeFee.add(onTime.getUpCountryFee());
                result.put("onTimeShipping", onTimeFee);
            } catch (Exception e) {
                result.put("onTimeShipping",
                        standardFee.multiply(new BigDecimal("1.8")).setScale(2, RoundingMode.HALF_UP));
                log.debug("On-time rate unavailable for {}/{}: {}", city, province, e.getMessage());
            }

            result.put("estimatedDeliveryDays", Map.of("standard", "3-5", "onTime", "1-2"));

        } catch (Exception e) {
            log.warn("Flash Express estimate failed for {}/{}/{}: {}", city, province, postal, e.getMessage());
            result.put("standardShipping", new BigDecimal("150.00"));
            result.put("onTimeShipping", new BigDecimal("300.00"));
            result.put("note", "Live rates temporarily unavailable. Estimated rates shown.");
        }

        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> estimateTax(Long addressId) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        BigDecimal taxRate = new BigDecimal("0.08");
        BigDecimal taxAmount = cart.getSubtotal().multiply(taxRate);

        Map<String, Object> taxEstimate = new HashMap<>();
        taxEstimate.put("taxRate", taxRate);
        taxEstimate.put("taxAmount", taxAmount);
        taxEstimate.put("taxableAmount", cart.getSubtotal());
        return taxEstimate;
    }

    @Override
    @Transactional
    public void saveCart(String cartName) {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserIdWithItems(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        if (savedCartRepository.existsByUserIdAndName(user.getId(), cartName)) {
            throw new CustomException("A saved cart with this name already exists");
        }

        SavedCart savedCart = SavedCart.builder().user(user).name(cartName).build();
        List<SavedCartItem> savedItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            SavedCartItem savedItem = SavedCartItem.builder()
                    .savedCart(savedCart)
                    .product(item.getProduct())
                    .quantity(item.getQuantity())
                    .price(item.getPrice())
                    .build();
            savedItems.add(savedItem);
        }
        savedCart.setItems(savedItems);
        savedCartRepository.save(savedCart);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getSavedCarts() {
        User user = getCurrentUser();
        return savedCartRepository.findByUserId(user.getId()).stream()
                .map(savedCart -> {
                    Map<String, Object> cartMap = new HashMap<>();
                    cartMap.put("id", savedCart.getId());
                    cartMap.put("name", savedCart.getName());
                    cartMap.put("itemCount", savedCart.getItems().size());
                    cartMap.put("savedAt", savedCart.getSavedAt());
                    return cartMap;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CartResponse restoreCart(Long savedCartId) {
        User user = getCurrentUser();
        SavedCart savedCart = savedCartRepository.findByIdAndUserIdWithItems(savedCartId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Saved cart not found"));

        Cart cart = cartRepository.findByUserId(user.getId()).orElseGet(() -> createNewCart(user));
        cartItemRepository.deleteByCartId(cart.getId());
        cart.getItems().clear();

        for (SavedCartItem savedItem : savedCart.getItems()) {
            Product product = savedItem.getProduct();
            if (product.isActive() && product.getStockQuantity() > 0) {
                int quantity = Math.min(savedItem.getQuantity(), product.getStockQuantity());
                CartItem newItem = CartItem.builder()
                        .cart(cart)
                        .product(product)
                        .quantity(quantity)
                        .price(product.getDiscountedPrice() != null ? product.getDiscountedPrice() : product.getPrice())
                        .originalPrice(product.getPrice())
                        .build();
                cart.getItems().add(newItem);
            }
        }

        cart.calculateTotals();
        return mapToCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional
    public CartResponse recalculateCart() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        cart.getItems().forEach(item -> {
            Product product = item.getProduct();
            item.setPrice(product.getDiscountedPrice() != null ? product.getDiscountedPrice() : product.getPrice());
            item.setOriginalPrice(product.getPrice());
        });

        cart.calculateTotals();
        return mapToCartResponse(cartRepository.save(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCartSummary() {
        User user = getCurrentUser();
        Cart cart = cartRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
        return mapToCartResponse(cart);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Cart createNewCart(User user) {
        Cart cart = Cart.builder()
                .user(user)
                .subtotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)
                .discountAmount(BigDecimal.ZERO)
                .couponDiscountAmount(BigDecimal.ZERO)
                .build();
        return cartRepository.save(cart);
    }

    private Coupon validateCouponFully(String couponCode, Cart cart, User user) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(couponCode)
                .orElseThrow(() -> new CustomException("Coupon not found or inactive: " + couponCode));

        if (!coupon.getIsActive()) {
            throw new CustomException("Coupon is not active");
        }
        if (coupon.getStartDate() != null && coupon.getStartDate().isAfter(LocalDateTime.now())) {
            throw new CustomException("Coupon is not yet valid");
        }
        if (coupon.getExpirationDate() != null && coupon.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new CustomException("Coupon has expired");
        }
        if (coupon.getMaxUsageCount() != null && coupon.getCurrentUsageCount() >= coupon.getMaxUsageCount()) {
            throw new CustomException("Coupon usage limit reached");
        }
        if (coupon.getMaxUsagePerUser() != null) {
            Integer userUsageCount = couponUsageRepository.countByUserIdAndCouponId(user.getId(), coupon.getId());
            if (userUsageCount >= coupon.getMaxUsagePerUser()) {
                throw new CustomException("You have reached the usage limit for this coupon");
            }
        }
        if (coupon.getMinimumPurchaseAmount() != null &&
                cart.getSubtotal().compareTo(coupon.getMinimumPurchaseAmount()) < 0) {
            throw new CustomException(
                    "Minimum purchase amount of " + coupon.getMinimumPurchaseAmount() + " required for this coupon");
        }
        if (coupon.getApplicableProducts() != null && !coupon.getApplicableProducts().isEmpty()) {
            boolean hasApplicableProduct = cart.getItems().stream()
                    .anyMatch(item -> coupon.getApplicableProducts().contains(item.getProduct().getId()));
            if (!hasApplicableProduct) {
                throw new CustomException("This coupon is not applicable to items in your cart");
            }
        }
        if (coupon.getApplicableCategories() != null && !coupon.getApplicableCategories().isEmpty()) {
            boolean hasApplicableCategory = cart.getItems().stream()
                    .anyMatch(item -> item.getProduct().getCategory() != null &&
                            coupon.getApplicableCategories().contains(item.getProduct().getCategory().getId()));
            if (!hasApplicableCategory) {
                throw new CustomException("This coupon is not applicable to product categories in your cart");
            }
        }
        return coupon;
    }

    private void applyCouponToCart(Cart cart, String couponCode) {
        Coupon coupon = couponRepository.findByCodeAndIsActiveTrue(couponCode)
                .orElseThrow(() -> new CustomException("Coupon not found"));
        applyCouponToCart(cart, coupon);
    }

    private void applyCouponToCart(Cart cart, Coupon coupon) {
        BigDecimal discountAmount = BigDecimal.ZERO;

        switch (coupon.getType()) {
            case PERCENTAGE:
                discountAmount = cart.getSubtotal()
                        .multiply(coupon.getDiscountValue().divide(BigDecimal.valueOf(100)));
                if (coupon.getMaximumDiscountAmount() != null
                        && discountAmount.compareTo(coupon.getMaximumDiscountAmount()) > 0) {
                    discountAmount = coupon.getMaximumDiscountAmount();
                }
                break;
            case FIXED_AMOUNT:
                discountAmount = coupon.getDiscountValue();
                if (discountAmount.compareTo(cart.getSubtotal()) > 0) {
                    discountAmount = cart.getSubtotal();
                }
                break;
            case FREE_SHIPPING:
                discountAmount = BigDecimal.ZERO;
                break;
        }

        cart.setCouponCode(coupon.getCode());
        cart.setCouponDiscountAmount(discountAmount);
        cart.calculateTotals();
    }

    private CartResponse mapToCartResponse(Cart cart) {
        List<CartResponse.CartItemResponse> itemResponses = cart.getItems().stream()
                .map(this::mapToCartItemResponse)
                .collect(Collectors.toList());

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser().getId())
                .items(itemResponses)
                .itemCount(cart.getItemCount())
                .uniqueItemCount(cart.getUniqueItemCount())
                .subtotal(cart.getSubtotal())
                .discountAmount(cart.getDiscountAmount())
                .couponCode(cart.getCouponCode())
                .shippingAmount(cart.getShippingAmount())
                .taxAmount(cart.getTaxAmount())
                .total(cart.getTotal())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartResponse.CartItemResponse mapToCartItemResponse(CartItem cartItem) {
        Product product = cartItem.getProduct();

        String imageUrl = product.getImageUrl();
        boolean inStock = product.isInStock();
        int stockQuantity = product.getStockQuantity();
        String variationName = null;

        if (cartItem.getVariation() != null) {
            ProductVariation variation = cartItem.getVariation();
            if (variation.getImageUrl() != null && !variation.getImageUrl().isBlank()) {
                imageUrl = variation.getImageUrl();
            }
            inStock = variation.isInStock();
            stockQuantity = variation.getStockQuantity() != null ? variation.getStockQuantity() : 0;
            variationName = variation.getName();
        }

        // Add-on label: use the add-on variation name if present, else the add-on product name
        String addonVariationName = null;
        if (cartItem.isAddon() && cartItem.getAddonVariation() != null) {
            addonVariationName = cartItem.getAddonVariation().getName();
        }

        return CartResponse.CartItemResponse.builder()
                .id(cartItem.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productImage(imageUrl)
                .price(cartItem.getPrice())
                .originalPrice(cartItem.getOriginalPrice())
                .quantity(cartItem.getQuantity())
                .subtotal(cartItem.getSubtotal())
                .inStock(inStock)
                .stockQuantity(stockQuantity)
                .variationName(variationName)
                .isAddon(cartItem.isAddon())
                .addonProductId(cartItem.isAddon() && cartItem.getAddonProduct() != null
                        ? cartItem.getAddonProduct().getId() : null)
                .addonVariationName(addonVariationName)
                .addonPrice(cartItem.getAddonPrice())
                .giftWrap(cartItem.getGiftWrap())
                .giftMessage(cartItem.getGiftMessage())
                .notes(cartItem.getNotes())
                .addedAt(cartItem.getCreatedAt())
                .updatedAt(cartItem.getUpdatedAt())
                .build();
    }
}