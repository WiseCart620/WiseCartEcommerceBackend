package com.wisecartecommerce.ecommerce.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.wisecartecommerce.ecommerce.Dto.Response.FlashNotifyResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashOrderResult;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashShippingRateResponse;
import com.wisecartecommerce.ecommerce.Dto.Response.FlashTrackingResponse;
import com.wisecartecommerce.ecommerce.entity.Address;
import com.wisecartecommerce.ecommerce.entity.FlashExpressSettings;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.repository.OrderRepository;
import com.wisecartecommerce.ecommerce.service.FlashExpressSettingsService;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import com.wisecartecommerce.ecommerce.util.FlashExpressClient;
import com.wisecartecommerce.ecommerce.util.FlashExpressSignatureUtil;
import com.wisecartecommerce.ecommerce.util.OrderStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashExpressShippingServiceImpl implements FlashExpressShippingService {

    private final FlashExpressClient client;
    private final FlashExpressSettingsService settingsService;
    private final OrderRepository orderRepository;

    private static final BigDecimal CENTS = new BigDecimal("100");
    private static final BigDecimal FALLBACK_SHIPPING_FEE = new BigDecimal("150.00");
    private static final BigDecimal FALLBACK_UPCOUNTRY_FEE = new BigDecimal("50.00");

    private static final int DIMENSION_DEFAULT_LENGTH = 20;
    private static final int DIMENSION_DEFAULT_WIDTH = 15;
    private static final int DIMENSION_DEFAULT_HEIGHT = 10;
    private static final int MIN_DIMENSION = 1;

    private static final int EXPRESS_CATEGORY_STANDARD = 1;
    private static final int EXPRESS_CATEGORY_OTD = 2;
    private static final int EXPRESS_CATEGORY_BULKY = 4;

    private static final String ARTICLE_CATEGORY_GENERAL = "2";

    private static final Set<Integer> VALID_EXPRESS_CATEGORIES = Set.of(
            EXPRESS_CATEGORY_STANDARD,
            EXPRESS_CATEGORY_OTD,
            EXPRESS_CATEGORY_BULKY
    );

    private void validateExpressCategory(int expressCategory) {
        if (!VALID_EXPRESS_CATEGORIES.contains(expressCategory)) {
            throw new IllegalArgumentException("Invalid express category: " + expressCategory
                    + ". Valid values are: " + EXPRESS_CATEGORY_STANDARD + " (Standard), "
                    + EXPRESS_CATEGORY_OTD + " (On-Time Delivery), "
                    + EXPRESS_CATEGORY_BULKY + " (Bulky Delivery)");
        }
    }

    private FlashExpressSettings getSettings() {
        return settingsService.getSettings();
    }

    @Override
    public FlashShippingRateResponse estimateRate(Address dstAddress, int weightGrams, int expressCategory) {
        validateExpressCategory(expressCategory);
        return estimateRateManual(
                dstAddress.getState(),
                dstAddress.getCity(),
                dstAddress.getPostalCode(),
                weightGrams,
                expressCategory);
    }

    @Override
    public FlashShippingRateResponse estimateRateManual(
            String dstProvince,
            String dstCity,
            String dstPostalCode,
            int weightGrams,
            int expressCategory) {
        validateExpressCategory(expressCategory);
        if (!isFlashExpressConfigured()) {
            log.warn("Flash Express not configured. Using fallback shipping fee: ₱{}", FALLBACK_SHIPPING_FEE);
            return createFallbackResponse(expressCategory, dstPostalCode);
        }

        try {
            FlashExpressSettings settings = getSettings();
            Map<String, String> params = new HashMap<>();
            params.put("mchId", settings.getMchId());
            params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
            params.put("srcProvinceName", settings.getSrcProvinceName());
            params.put("srcCityName", settings.getSrcCityName());
            params.put("srcPostalCode", padPostalCode(settings.getSrcPostalCode()));
            params.put("dstProvinceName", dstProvince);
            params.put("dstCityName", dstCity);
            params.put("dstPostalCode", padPostalCode(dstPostalCode));
            params.put("weight", String.valueOf(weightGrams));
            params.put("expressCategory", String.valueOf(expressCategory));
            params.put("sign", FlashExpressSignatureUtil.generateSign(params, settings.getSecretKey()));

            Map<String, Object> response = client.post(settings.getBaseUrl() + "/open/v1/orders/estimate_rate", params);
            return parseRateResponse(response, expressCategory);

        } catch (Exception e) {
            log.error("Flash Express API call failed: {}", e.getMessage(), e);
            return createFallbackResponse(expressCategory, dstPostalCode);
        }
    }

    @Override
    public FlashOrderResult createOrder(Order order, Address shippingAddress,
            int weightGrams, int expressCategory) {

        if (!isFlashExpressConfigured()) {
            log.warn("Flash Express not configured — skipping order creation");
            return null;
        }

        FlashExpressSettings settings = getSettings();
        boolean isCod = isCodPayment(order.getPaymentMethod());

        logCodDetailsIfApplicable(order, isCod);

        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", settings.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());

        String outTradeNo = order.getId() != null ? order.getId().toString() : order.getOrderNumber();
        params.put("outTradeNo", outTradeNo);

        // Sender information
        params.put("srcName", settings.getSrcName());
        params.put("srcPhone", settings.getSrcPhone());
        params.put("srcDetailAddress", settings.getSrcDetailAddress());
        params.put("srcProvinceName", settings.getSrcProvinceName());
        params.put("srcCityName", settings.getSrcCityName());
        params.put("srcPostalCode", settings.getSrcPostalCode());

        // Recipient information
        params.put("dstName", formatRecipientName(shippingAddress));
        params.put("dstPhone", shippingAddress.getPhone());
        params.put("dstProvinceName", shippingAddress.getState());
        params.put("dstCityName", shippingAddress.getCity());
        params.put("dstPostalCode", shippingAddress.getPostalCode());
        params.put("dstDetailAddress", shippingAddress.getAddressLine1());

        // Parcel information
        params.put("articleCategory", ARTICLE_CATEGORY_GENERAL);
        params.put("expressCategory", String.valueOf(expressCategory));

        int[] dimensions = calculateDimensions(order);
        params.put("weight", String.valueOf(weightGrams));
        params.put("length", String.valueOf(dimensions[0]));
        params.put("width", String.valueOf(dimensions[1]));
        params.put("height", String.valueOf(dimensions[2]));
        params.put("insured", "0");

        // COD configuration
        configureCodPayment(params, order, isCod);

        logRequestDetails(params);

        params.put("sign", FlashExpressSignatureUtil.generateSign(params, settings.getSecretKey()));

        try {
            Map<String, Object> response = client.post(settings.getBaseUrl() + "/open/v3/orders", params);
            return parseOrderCreationResponse(response, isCod);
        } catch (Exception e) {
            log.error("Flash Express order creation exception: {}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public byte[] printLabel(String pno) {
        if (!isFlashExpressConfigured()) {
            throw new CustomException("Flash Express is not configured");
        }

        FlashExpressSettings settings = getSettings();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", settings.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, settings.getSecretKey()));

        String url = settings.getBaseUrl() + "/open/v1/orders/" + pno + "/pre_print";
        log.info("Downloading Flash Express label for PNO={}", pno);

        byte[] pdfBytes = client.postForBytes(url, params);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new CustomException("Empty label response from Flash Express for PNO: " + pno);
        }

        return pdfBytes;
    }

    @Override
    public FlashNotifyResponse notifyCourier(int estimateParcelNumber, String remark) {
        if (!isFlashExpressConfigured()) {
            throw new CustomException("Flash Express is not configured");
        }

        FlashExpressSettings settings = getSettings();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", settings.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("srcName", settings.getSrcName());
        params.put("srcPhone", settings.getSrcPhone());
        params.put("srcProvinceName", settings.getSrcProvinceName());
        params.put("srcCityName", settings.getSrcCityName());
        params.put("srcPostalCode", settings.getSrcPostalCode());
        params.put("srcDetailAddress", settings.getSrcDetailAddress());
        params.put("estimateParcelNumber", String.valueOf(estimateParcelNumber));

        if (remark != null && !remark.isBlank()) {
            params.put("remark", remark);
        }

        params.put("sign", FlashExpressSignatureUtil.generateSign(params, settings.getSecretKey()));

        try {
            Map<String, Object> response = client.post(settings.getBaseUrl() + "/open/v1/notify", params);
            return parseNotifyResponse(response);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flash Express notify exception: {}", e.getMessage(), e);
            throw new CustomException("Flash Express notify failed: " + e.getMessage());
        }
    }

    @Override
    public FlashTrackingResponse trackOrder(String pno) {
        if (!isFlashExpressConfigured()) {
            throw new CustomException("Flash Express is not configured");
        }

        FlashExpressSettings settings = getSettings();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", settings.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, settings.getSecretKey()));

        String url = settings.getBaseUrl() + "/open/v1/orders/" + pno + "/routes";

        try {
            Map<String, Object> response = client.post(url, params);
            return parseTrackingResponse(response, pno);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flash tracking exception for PNO={}: {}", pno, e.getMessage(), e);
            throw new CustomException("Flash tracking failed: " + e.getMessage());
        }
    }

    @Override
    public void cancelOrder(String pno) {
        if (!isFlashExpressConfigured()) {
            throw new CustomException("Flash Express is not configured");
        }

        FlashExpressSettings settings = getSettings();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", settings.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, settings.getSecretKey()));

        try {
            Map<String, Object> response = client.post(
                    settings.getBaseUrl() + "/open/v1/orders/" + pno + "/cancel", params);

            Object codeObj = response.get("code");
            int code = codeObj instanceof Number n ? n.intValue() : 0;

            if (code == 1) {
                log.info("Flash Express order PNO={} cancelled successfully", pno);
            } else {
                String message = (String) response.get("message");
                log.warn("Flash Express cancel failed for PNO={}: code={}, message={}", pno, code, message);
                throw new CustomException("Flash Express cancellation failed: " + message);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flash Express cancel exception for PNO={}: {}", pno, e.getMessage(), e);
            throw new CustomException("Failed to cancel Flash Express order: " + e.getMessage());
        }
    }

    @Override
    public int getPendingParcelCount() {
        Long count = orderRepository.countReadyForPickup(
                List.of(OrderStatus.PENDING, OrderStatus.PROCESSING)
        );
        return count != null ? count.intValue() : 0;
    }

    // ==================== PRIVATE HELPER METHODS ====================
    private boolean isCodPayment(String paymentMethod) {
        return "COD".equalsIgnoreCase(paymentMethod)
                || "CASH_ON_DELIVERY".equalsIgnoreCase(paymentMethod);
    }

    private void logCodDetailsIfApplicable(Order order, boolean isCod) {
        if (isCod && order.getFinalAmount() != null) {
            log.debug("Order {} finalAmount raw value: '{}', scale: {}, precision: {}",
                    order.getOrderNumber(),
                    order.getFinalAmount().toPlainString(),
                    order.getFinalAmount().scale(),
                    order.getFinalAmount().precision());
        }
    }

    private String formatRecipientName(Address shippingAddress) {
        return shippingAddress.getFirstName() + " " + shippingAddress.getLastName();
    }

    private void configureCodPayment(Map<String, String> params, Order order, boolean isCod) {
        if (isCod && order.getFinalAmount() != null && order.getFinalAmount().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal roundedAmount = order.getFinalAmount().setScale(0, RoundingMode.HALF_UP);
            String codAmountCents = roundedAmount.multiply(CENTS).toBigInteger().toString();

            log.info("COD enabled - Amount: ₱{} ({} cents)", roundedAmount.toPlainString(), codAmountCents);

            params.put("codEnabled", "1");
            params.put("codAmount", codAmountCents);
        } else {
            params.put("codEnabled", "0");
            if (isCod) {
                log.warn("COD requested but amount is zero or null for order {}", order.getOrderNumber());
            }
        }
    }

    private void logRequestDetails(Map<String, String> params) {
        log.info("=== Flash Express Order Creation Request ===");
        params.forEach((key, value) -> {
            if ("sign".equals(key)) {
                return;
            }
            if ("codAmount".equals(key)) {
                log.info("  {} = '{}' (length: {}, numeric: {})",
                        key, value, value.length(), value.matches("\\d+"));
            } else if ("codEnabled".equals(key)) {
                log.info("  {} = '{}'", key, value);
            } else {
                log.debug("  {} = '{}'", key, value);
            }
        });
        log.info("=== End Request ===");
    }

    @SuppressWarnings("unchecked")
    private FlashOrderResult parseOrderCreationResponse(Map<String, Object> response, boolean isCod) {
        Object codeObj = response.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : 0;

        if (code == 1) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            log.info("Flash order created successfully: PNO={}, COD={}", data.get("pno"), isCod);
            return FlashOrderResult.builder()
                    .pno((String) data.get("pno"))
                    .sortCode((String) data.get("sortCode"))
                    .dstStoreName((String) data.get("dstStoreName"))
                    .upCountryCharge(Boolean.TRUE.equals(data.get("upcountryCharge")))
                    .build();
        } else {
            log.error("Flash Express order creation failed: code={}, message={}, full={}",
                    response.get("code"), response.get("message"), response);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private FlashNotifyResponse parseNotifyResponse(Map<String, Object> response) {
        Object codeObj = response.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : 0;

        switch (code) {
            case 1 -> {
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                Object ticketId = data.get("ticketPickupId");
                Long ticketPickupId = ticketId instanceof Number n ? n.longValue() : null;
                log.info("Flash courier notified. TicketPickupId={}", ticketPickupId);
                return FlashNotifyResponse.builder()
                        .ticketPickupId(ticketPickupId)
                        .staffInfoName((String) data.get("staffInfoName"))
                        .staffInfoPhone((String) data.get("staffInfoPhone"))
                        .timeoutAtText((String) data.get("timeoutAtText"))
                        .ticketMessage((String) data.get("ticketMessage"))
                        .upCountryNote((String) data.get("upCountryNote"))
                        .build();
            }
            case 1010 -> {
                log.info("Active pickup ticket already exists (code 1010)");
                return FlashNotifyResponse.builder()
                        .ticketMessage("A pickup has already been scheduled. The courier is on the way.")
                        .build();
            }
            default -> {
                String message = (String) response.get("message");
                log.error("Flash notify courier failed: code={}, message={}", code, message);
                throw new CustomException("Flash Express notify failed: " + message);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private FlashTrackingResponse parseTrackingResponse(Map<String, Object> response, String pno) {
        Object codeObj = response.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : 0;

        if (code == 1) {
            Map<String, Object> data = (Map<String, Object>) response.get("data");

            Integer state = getIntegerFromMap(data, "state");
            Long stateChangeAt = getLongFromMap(data, "stateChangeAt");

            List<Map<String, Object>> rawRoutes = (data.get("routes") instanceof List)
                    ? (List<Map<String, Object>>) data.get("routes")
                    : Collections.emptyList();

            List<FlashTrackingResponse.RouteEntry> routes = rawRoutes.stream()
                    .map(this::parseRouteEntry)
                    .collect(Collectors.toList());

            return FlashTrackingResponse.builder()
                    .pno((String) data.get("pno"))
                    .origPno((String) data.get("origPno"))
                    .returnedPno((String) data.get("returnedPno"))
                    .state(state)
                    .stateText((String) data.get("stateText"))
                    .stateChangeAt(stateChangeAt)
                    .routes(routes)
                    .build();
        } else {
            String message = (String) response.get("message");
            log.error("Flash tracking failed for PNO={}: code={}, message={}", pno, response.get("code"), message);
            throw new CustomException("Flash tracking failed: " + message);
        }
    }

    private FlashTrackingResponse.RouteEntry parseRouteEntry(Map<String, Object> route) {
        Long routedAt = getLongFromMap(route, "routedAt");
        Integer routeState = getIntegerFromMap(route, "state");

        return FlashTrackingResponse.RouteEntry.builder()
                .routedAt(routedAt)
                .routeAction((String) route.get("routeAction"))
                .message((String) route.get("message"))
                .state(routeState)
                .build();
    }

    private Integer getIntegerFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.intValue() : null;
    }

    private Long getLongFromMap(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value instanceof Number n ? n.longValue() : null;
    }

    private int[] calculateDimensions(Order order) {
        int maxLength = 0;
        int maxWidth = 0;
        int totalHeight = 0;

        for (var item : order.getItems()) {
            ProductVariation variation = item.getVariation();
            Product product = item.getProduct();

            int length = DIMENSION_DEFAULT_LENGTH;
            int width = DIMENSION_DEFAULT_WIDTH;
            int height = DIMENSION_DEFAULT_HEIGHT;

            if (variation != null) {
                length = variation.getLengthCm() != null ? variation.getLengthCm().intValue() : length;
                width = variation.getWidthCm() != null ? variation.getWidthCm().intValue() : width;
                height = variation.getHeightCm() != null ? variation.getHeightCm().intValue() : height;
            }

            if (length == 0 && product.getLengthCm() != null) {
                length = product.getLengthCm().intValue();
            }
            if (width == 0 && product.getWidthCm() != null) {
                width = product.getWidthCm().intValue();
            }
            if (height == 0 && product.getHeightCm() != null) {
                height = product.getHeightCm().intValue();
            }

            maxLength = Math.max(maxLength, Math.max(length, MIN_DIMENSION));
            maxWidth = Math.max(maxWidth, Math.max(width, MIN_DIMENSION));
            totalHeight += Math.max(height, MIN_DIMENSION) * item.getQuantity();
        }

        return new int[]{
            Math.max(maxLength, MIN_DIMENSION),
            Math.max(maxWidth, MIN_DIMENSION),
            Math.max(totalHeight, MIN_DIMENSION)
        };
    }

    private FlashShippingRateResponse parseRateResponse(Map<String, Object> body, int expressCategory) {
        if (body == null) {
            throw new CustomException("Empty response from Flash Express");
        }

        Object codeObj = body.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : 0;

        if (code != 1) {
            String message = (String) body.get("message");
            log.error("Flash Express rate error: {}", message);
            throw new CustomException("Shipping rate error: " + message);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data == null) {
            throw new CustomException("No data in Flash Express response");
        }

        BigDecimal shippingFee = centsToPHP(data.get("estimatePrice"));
        BigDecimal upCountryFee = centsToPHP(data.get("upCountryAmount"));
        BigDecimal codTransferFee = centsToPHP(data.get("codTransferFee"));
        Boolean upCountry = (Boolean) data.get("upCountry");

        Object policyObj = data.get("pricePolicy");
        int pricePolicy = policyObj instanceof Number n ? n.intValue() : 1;
        String pricePolicyText = pricePolicy == 2 ? "By dimensions" : "By weight";

        return FlashShippingRateResponse.builder()
                .shippingFee(shippingFee)
                .upCountryFee(upCountryFee)
                .codTransferFee(codTransferFee)
                .upCountry(Boolean.TRUE.equals(upCountry))
                .pricePolicyText(pricePolicyText)
                .expressCategory(expressCategory)
                .expressLabel(getExpressLabel(expressCategory))
                .build();
    }

    private String getExpressLabel(int expressCategory) {
        return switch (expressCategory) {
            case EXPRESS_CATEGORY_OTD ->
                "On-Time Delivery";
            case EXPRESS_CATEGORY_BULKY ->
                "Bulky Delivery";
            case EXPRESS_CATEGORY_STANDARD ->
                "Standard Delivery";
            default ->
                "Standard Delivery";
        };
    }

    private BigDecimal centsToPHP(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(value.toString()).divide(CENTS, 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            log.warn("Failed to convert cents to PHP: {}", value, e);
            return BigDecimal.ZERO;
        }
    }

    private boolean isFlashExpressConfigured() {
        FlashExpressSettings settings = getSettings();
        String mchId = settings.getMchId();
        String secretKey = settings.getSecretKey();
        String baseUrl = settings.getBaseUrl();

        return mchId != null
                && !mchId.isEmpty()
                && !mchId.contains("YOUR_")
                && !mchId.contains("AAXXXX")
                && secretKey != null
                && !secretKey.isEmpty()
                && baseUrl != null;
    }

    private FlashShippingRateResponse createFallbackResponse(int expressCategory, String dstPostalCode) {
        boolean isUpCountry = isUpCountryLocation(dstPostalCode);

        BigDecimal shippingFee = FALLBACK_SHIPPING_FEE;
        BigDecimal upCountryFee = isUpCountry ? FALLBACK_UPCOUNTRY_FEE : BigDecimal.ZERO;

        if (isUpCountry) {
            shippingFee = shippingFee.add(upCountryFee);
        }

        return FlashShippingRateResponse.builder()
                .shippingFee(shippingFee)
                .upCountryFee(upCountryFee)
                .codTransferFee(BigDecimal.ZERO)
                .upCountry(isUpCountry)
                .pricePolicyText("Fallback rate (Flash Express unavailable)")
                .expressCategory(expressCategory)
                .expressLabel(getExpressLabel(expressCategory) + " (Fallback)")
                .build();
    }

    private boolean isUpCountryLocation(String postalCode) {
        return postalCode != null
                && (postalCode.startsWith("4")
                || postalCode.startsWith("5")
                || postalCode.startsWith("9"));
    }

    private String padPostalCode(String postalCode) {
        if (postalCode == null || postalCode.trim().isEmpty()) {
            return postalCode;
        }
        String trimmed = postalCode.trim();
        return trimmed.length() < 5
                ? String.format("%05d", Integer.parseInt(trimmed))
                : trimmed;
    }
}
