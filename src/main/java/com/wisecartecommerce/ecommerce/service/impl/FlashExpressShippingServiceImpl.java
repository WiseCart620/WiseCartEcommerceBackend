package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.Dto.Response.*;
import com.wisecartecommerce.ecommerce.config.FlashExpressProperties;
import com.wisecartecommerce.ecommerce.entity.Address;
import com.wisecartecommerce.ecommerce.entity.FlashExpressSettings;
import com.wisecartecommerce.ecommerce.entity.Order;
import com.wisecartecommerce.ecommerce.entity.Product;
import com.wisecartecommerce.ecommerce.entity.ProductVariation;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.service.FlashExpressSettingsService;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import com.wisecartecommerce.ecommerce.util.FlashExpressClient;
import com.wisecartecommerce.ecommerce.util.FlashExpressSignatureUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashExpressShippingServiceImpl implements FlashExpressShippingService {

    private final FlashExpressProperties props;
    private final FlashExpressClient client;
    private final FlashExpressSettingsService settingsService;

    private static final BigDecimal CENTS = BigDecimal.valueOf(100);
    private static final BigDecimal FALLBACK_SHIPPING_FEE = new BigDecimal("150.00");
    private static final BigDecimal FALLBACK_UPCOUNTRY_FEE = new BigDecimal("50.00");
    private FlashExpressSettings s() {
        return settingsService.getSettings();
    }



    @Override
    public FlashShippingRateResponse estimateRate(Address dstAddress, int weightGrams, int expressCategory) {
        return estimateRateManual(
                dstAddress.getState(),
                dstAddress.getCity(),
                dstAddress.getPostalCode(),
                weightGrams,
                expressCategory);
    }

    @Override
    public FlashShippingRateResponse estimateRateManual(
            String dstProvince, String dstCity, String dstPostalCode,
            int weightGrams, int expressCategory) {

        if (!isFlashExpressConfigured()) {
            log.warn("Flash Express not configured. Using fallback shipping fee: ₱{}", FALLBACK_SHIPPING_FEE);
            return createFallbackResponse(expressCategory, dstPostalCode);
        }

        try {
            FlashExpressSettings s = s();
            Map<String, String> params = new HashMap<>();
            params.put("mchId", s.getMchId());
            params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
            params.put("srcProvinceName", s.getSrcProvinceName());
            params.put("srcCityName", s.getSrcCityName());
            params.put("srcPostalCode", padPostalCode(s.getSrcPostalCode()));
            params.put("dstProvinceName", dstProvince);
            params.put("dstCityName", dstCity);
            params.put("dstPostalCode", padPostalCode(dstPostalCode));
            params.put("weight", String.valueOf(weightGrams));
            params.put("expressCategory", String.valueOf(expressCategory));
            params.put("sign", FlashExpressSignatureUtil.generateSign(params, s.getSecretKey()));

            Map<String, Object> response = client.post(s.getBaseUrl() + "/open/v1/orders/estimate_rate", params);
            return parseRateResponse(response, expressCategory);

        } catch (Exception e) {
            log.error("Flash Express API call failed: {}. Using fallback: ₱{}", e.getMessage(), FALLBACK_SHIPPING_FEE);
            return createFallbackResponse(expressCategory, dstPostalCode);
        }
    }

    // ─── Create Order ─────────────────────────────────────────────────────────

    @Override
    public FlashOrderResult createOrder(Order order, Address shippingAddress,
            int weightGrams, int expressCategory) {
        if (!isFlashExpressConfigured()) {
            log.warn("Flash Express not configured — skipping order creation");
            return null;
        }

        FlashExpressSettings s = s();
        boolean isCod = isCod(order.getPaymentMethod());

        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", s.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("outTradeNo", order.getId().toString());

        params.put("srcName", s.getSrcName());
        params.put("srcPhone", s.getSrcPhone());
        params.put("srcDetailAddress", s.getSrcDetailAddress());
        params.put("srcProvinceName", s.getSrcProvinceName());
        params.put("srcCityName", s.getSrcCityName());
        params.put("srcPostalCode", s.getSrcPostalCode());

        params.put("dstName", shippingAddress.getFirstName() + " " + shippingAddress.getLastName());
        params.put("dstPhone", shippingAddress.getPhone());
        params.put("dstProvinceName", shippingAddress.getState());
        params.put("dstCityName", shippingAddress.getCity());
        params.put("dstPostalCode", shippingAddress.getPostalCode());
        params.put("dstDetailAddress", shippingAddress.getAddressLine1());

        params.put("articleCategory", "2");
        params.put("expressCategory", String.valueOf(expressCategory));

        int[] dims = calculateDimensions(order);
        params.put("weight", String.valueOf(weightGrams));
        params.put("length", String.valueOf(dims[0]));
        params.put("width", String.valueOf(dims[1]));
        params.put("height", String.valueOf(dims[2]));
        params.put("insured", "0");

        if (isCod && order.getFinalAmount() != null) {
            long codAmountCents = order.getFinalAmount()
                    .multiply(CENTS)
                    .setScale(0, RoundingMode.CEILING)
                    .longValue();
            params.put("codEnabled", "1");
            params.put("codAmount", Long.toString(codAmountCents));
            log.info("COD enabled for order {}: ₱{} = {} cents",
                    order.getOrderNumber(), order.getFinalAmount(), codAmountCents);
        } else {
            params.put("codEnabled", "0");
        }

        params.put("sign", FlashExpressSignatureUtil.generateSign(params, s.getSecretKey()));

        try {
            Map<String, Object> response = client.post(s.getBaseUrl() + "/open/v3/orders", params);

            Object codeObj = response.get("code");
            int code = codeObj instanceof Number n ? n.intValue() : 0;

            if (code == 1) {
                @SuppressWarnings("unchecked")
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
        } catch (Exception e) {
            log.error("Flash Express order creation exception: {}", e.getMessage());
            return null;
        }
    }

    // ─── Print Label ──────────────────────────────────────────────────────────

    @Override
    public byte[] printLabel(String pno) {
        if (!isFlashExpressConfigured()) {
            throw new CustomException("Flash Express is not configured");
        }

        FlashExpressSettings s = s();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", s.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, s.getSecretKey()));

        String url = s.getBaseUrl() + "/open/v1/orders/" + pno + "/pre_print";
        log.info("Downloading Flash Express label for PNO={}", pno);

        byte[] pdfBytes = client.postForBytes(url, params);

        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new CustomException("Empty label response from Flash Express for PNO: " + pno);
        }

        return pdfBytes;
    }

    // ─── Notify Courier ───────────────────────────────────────────────────────

    @Override
    public FlashNotifyResponse notifyCourier(int estimateParcelNumber, String remark) {
        if (!isFlashExpressConfigured()) {
            throw new CustomException("Flash Express is not configured");
        }

        FlashExpressSettings s = s();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", s.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("srcName", s.getSrcName());
        params.put("srcPhone", s.getSrcPhone());
        params.put("srcProvinceName", s.getSrcProvinceName());
        params.put("srcCityName", s.getSrcCityName());
        params.put("srcPostalCode", s.getSrcPostalCode());
        params.put("srcDetailAddress", s.getSrcDetailAddress());
        params.put("estimateParcelNumber", String.valueOf(estimateParcelNumber));
        if (remark != null && !remark.isBlank()) {
            params.put("remark", remark);
        }
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, s.getSecretKey()));

        try {
            Map<String, Object> response = client.post(s.getBaseUrl() + "/open/v1/notify", params);

            Object codeObj = response.get("code");
            int code = codeObj instanceof Number n ? n.intValue() : 0;

            if (code == 1) {
                @SuppressWarnings("unchecked")
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

            } else if (code == 1010) {
                log.info(
                        "Flash notify: active pickup ticket already exists (code 1010) — returning existing ticket indicator");
                return FlashNotifyResponse.builder()
                        .ticketMessage("A pickup has already been scheduled. The courier is on the way.")
                        .build();

            } else {
                String message = (String) response.get("message");
                log.error("Flash notify courier failed: code={}, message={}", code, message);
                throw new CustomException("Flash Express notify failed: " + message);
            }

        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flash Express notify exception: {}", e.getMessage());
            throw new CustomException("Flash Express notify failed: " + e.getMessage());
        }
    }

    // ─── Track Order ──────────────────────────────────────────────────────────

    @Override
    public FlashTrackingResponse trackOrder(String pno) {
        if (!isFlashExpressConfigured()) {
            throw new CustomException("Flash Express is not configured");
        }

        FlashExpressSettings s = s();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", s.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, s.getSecretKey()));

        String url = s.getBaseUrl() + "/open/v1/orders/" + pno + "/routes";

        try {
            Map<String, Object> response = client.post(url, params);

            Object codeObj = response.get("code");
            int code = codeObj instanceof Number n ? n.intValue() : 0;

            if (code == 1) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");

                Object stateObj = data.get("state");
                Integer state = stateObj instanceof Number n ? n.intValue() : null;

                Object stateChangeObj = data.get("stateChangeAt");
                Long stateChangeAt = stateChangeObj instanceof Number n ? n.longValue() : null;

                Object routesObj = data.get("routes");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> rawRoutes = (routesObj instanceof List)
                        ? (List<Map<String, Object>>) routesObj
                        : Collections.emptyList();

                List<FlashTrackingResponse.RouteEntry> routes = rawRoutes.stream()
                        .map(r -> {
                            Object routedAtObj = r.get("routedAt");
                            Long routedAt = routedAtObj instanceof Number n ? n.longValue() : null;
                            Object routeStateObj = r.get("state");
                            Integer routeState = routeStateObj instanceof Number n ? n.intValue() : null;
                            return FlashTrackingResponse.RouteEntry.builder()
                                    .routedAt(routedAt)
                                    .routeAction((String) r.get("routeAction"))
                                    .message((String) r.get("message"))
                                    .state(routeState)
                                    .build();
                        })
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
                log.error("Flash tracking failed: code={}, message={}", response.get("code"), message);
                throw new CustomException("Flash tracking failed: " + message);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flash tracking exception for PNO={}: {}", pno, e.getMessage());
            throw new CustomException("Flash tracking failed: " + e.getMessage());
        }
    }


    // ─── Cancel Order ─────────────────────────────────────────────────────────

    @Override
    public void cancelOrder(String pno) {
        if (!isFlashExpressConfigured()) {
            throw new CustomException("Flash Express is not configured");
        }

        FlashExpressSettings s = s();
        Map<String, String> params = new LinkedHashMap<>();
        params.put("mchId", s.getMchId());
        params.put("nonceStr", FlashExpressSignatureUtil.generateNonce());
        params.put("sign", FlashExpressSignatureUtil.generateSign(params, s.getSecretKey()));

        try {
            Map<String, Object> response = client.post(
                    s.getBaseUrl() + "/open/v1/orders/" + pno + "/cancel", params);

            Object codeObj = response.get("code");
            int code = codeObj instanceof Number n ? n.intValue() : 0;

            if (code == 1) {
                log.info("Flash Express order PNO={} cancelled successfully", pno);
            } else {
                String message = (String) response.get("message");
                log.warn("Flash Express cancel non-success PNO={}: code={}, message={}", pno, code, message);
                throw new CustomException("Flash Express cancellation failed: " + message);
            }
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Flash Express cancel exception for PNO={}: {}", pno, e.getMessage());
            throw new CustomException("Failed to cancel Flash Express order: " + e.getMessage());
        }
    }

    // ─── Private Helpers ──────────────────────────────────────────────────────

    private boolean isCod(String paymentMethod) {
        return "COD".equalsIgnoreCase(paymentMethod) ||
                "CASH_ON_DELIVERY".equalsIgnoreCase(paymentMethod);
    }

    private int[] calculateDimensions(Order order) {
        int maxLength = 0, maxWidth = 0, totalHeight = 0;

        for (var item : order.getItems()) {
            ProductVariation variation = item.getVariation();
            Product product = item.getProduct();

            int l = 0, w = 0, h = 0;

            if (variation != null) {
                l = variation.getLengthCm() != null ? variation.getLengthCm().intValue() : 0;
                w = variation.getWidthCm() != null ? variation.getWidthCm().intValue() : 0;
                h = variation.getHeightCm() != null ? variation.getHeightCm().intValue() : 0;
            }

            if (l == 0)
                l = product.getLengthCm() != null ? product.getLengthCm().intValue() : 20;
            if (w == 0)
                w = product.getWidthCm() != null ? product.getWidthCm().intValue() : 15;
            if (h == 0)
                h = product.getHeightCm() != null ? product.getHeightCm().intValue() : 10;

            maxLength = Math.max(maxLength, l);
            maxWidth = Math.max(maxWidth, w);
            totalHeight += h * item.getQuantity();
        }

        return new int[] {
                Math.max(maxLength, 1),
                Math.max(maxWidth, 1),
                Math.max(totalHeight, 1)
        };
    }

    private FlashShippingRateResponse parseRateResponse(Map<String, Object> body, int expressCategory) {
        if (body == null)
            throw new CustomException("Empty response from Flash Express");

        Object codeObj = body.get("code");
        int code = codeObj instanceof Number n ? n.intValue() : 0;

        if (code != 1) {
            String message = (String) body.get("message");
            log.error("Flash Express rate error: {}", message);
            throw new CustomException("Shipping rate error: " + message);
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data == null)
            throw new CustomException("No data in Flash Express response");

        BigDecimal shippingFee = centsToPHP(data.get("estimatePrice"));
        BigDecimal upCountryFee = centsToPHP(data.get("upCountryAmount"));
        BigDecimal codTransferFee = centsToPHP(data.get("codTransferFee"));
        Boolean upCountry = (Boolean) data.get("upCountry");
        Object policyObj = data.get("pricePolicy");
        int pricePolicy = policyObj instanceof Number n ? n.intValue() : 1;

        String pricePolicyText = pricePolicy == 2 ? "By dimensions" : "By weight";
        String expressLabel = switch (expressCategory) {
            case 2 -> "On-Time Delivery";
            case 4 -> "Bulky Delivery";
            default -> "Standard Delivery";
        };

        return FlashShippingRateResponse.builder()
                .shippingFee(shippingFee)
                .upCountryFee(upCountryFee)
                .codTransferFee(codTransferFee)
                .upCountry(Boolean.TRUE.equals(upCountry))
                .pricePolicyText(pricePolicyText)
                .expressCategory(expressCategory)
                .expressLabel(expressLabel)
                .build();
    }

    private BigDecimal centsToPHP(Object value) {
        if (value == null)
            return BigDecimal.ZERO;
        try {
            return new BigDecimal(value.toString()).divide(CENTS, 2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }

    private boolean isFlashExpressConfigured() {
        FlashExpressSettings s = s();
        return s.getMchId() != null &&
                !s.getMchId().isEmpty() &&
                !s.getMchId().contains("YOUR_") &&
                !s.getMchId().contains("AAXXXX") &&
                s.getSecretKey() != null &&
                !s.getSecretKey().isEmpty() &&
                s.getBaseUrl() != null;
    }

    private FlashShippingRateResponse createFallbackResponse(int expressCategory, String dstPostalCode) {
        boolean isUpCountry = dstPostalCode != null &&
                (dstPostalCode.startsWith("4") ||
                        dstPostalCode.startsWith("5") ||
                        dstPostalCode.startsWith("9"));

        BigDecimal shippingFee = FALLBACK_SHIPPING_FEE;
        BigDecimal upCountryFee = isUpCountry ? FALLBACK_UPCOUNTRY_FEE : BigDecimal.ZERO;
        if (isUpCountry)
            shippingFee = shippingFee.add(upCountryFee);

        String expressLabel = switch (expressCategory) {
            case 2 -> "On-Time Delivery (Fallback)";
            case 4 -> "Bulky Delivery (Fallback)";
            default -> "Standard Delivery (Fallback)";
        };

        return FlashShippingRateResponse.builder()
                .shippingFee(shippingFee)
                .upCountryFee(upCountryFee)
                .codTransferFee(BigDecimal.ZERO)
                .upCountry(isUpCountry)
                .pricePolicyText("Fallback rate (Flash Express unavailable)")
                .expressCategory(expressCategory)
                .expressLabel(expressLabel)
                .build();
    }

    private String padPostalCode(String postalCode) {
        if (postalCode == null)
            return postalCode;
        return postalCode.length() < 5
                ? String.format("%05d", Integer.parseInt(postalCode.trim()))
                : postalCode;
    }
}