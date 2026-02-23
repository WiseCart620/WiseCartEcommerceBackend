package com.wisecartecommerce.ecommerce.service.impl;

import com.wisecartecommerce.ecommerce.Dto.Response.FlashShippingRateResponse;
import com.wisecartecommerce.ecommerce.config.FlashExpressProperties;
import com.wisecartecommerce.ecommerce.entity.Address;
import com.wisecartecommerce.ecommerce.exception.CustomException;
import com.wisecartecommerce.ecommerce.service.FlashExpressShippingService;
import com.wisecartecommerce.ecommerce.util.FlashExpressClient;
import com.wisecartecommerce.ecommerce.util.SignUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlashExpressShippingServiceImpl implements FlashExpressShippingService {

    private final FlashExpressProperties props;
    private final FlashExpressClient client;

    private static final BigDecimal CENTS = BigDecimal.valueOf(100);
    private static final BigDecimal FALLBACK_SHIPPING_FEE = new BigDecimal("150.00");
    private static final BigDecimal FALLBACK_UPCOUNTRY_FEE = new BigDecimal("50.00");

    @Override
    public FlashShippingRateResponse estimateRate(Address dstAddress, int weightGrams, int expressCategory) {
        return estimateRateManual(
                dstAddress.getState(), // state = province in PH
                dstAddress.getCity(),
                dstAddress.getPostalCode(),
                weightGrams,
                expressCategory);
    }

    @Override
    public FlashShippingRateResponse estimateRateManual(
            String dstProvince, String dstCity, String dstPostalCode,
            int weightGrams, int expressCategory) {

        // Check if Flash Express is configured
        if (!isFlashExpressConfigured()) {
            log.warn("Flash Express not configured. Using fallback shipping fee: ₱{}", FALLBACK_SHIPPING_FEE);
            return createFallbackResponse(expressCategory, dstPostalCode);
        }

        try {
            Map<String, String> params = new HashMap<>();
            params.put("mchId", props.getMchId());
            params.put("nonceStr", String.valueOf(System.currentTimeMillis()));

            params.put("srcProvinceName", props.getSrcProvinceName());
            params.put("srcCityName", props.getSrcCityName());
            params.put("srcPostalCode", props.getSrcPostalCode());

            params.put("dstProvinceName", dstProvince);
            params.put("dstCityName", dstCity);
            params.put("dstPostalCode", dstPostalCode);
            params.put("weight", String.valueOf(weightGrams));
            params.put("expressCategory", String.valueOf(expressCategory));

            String sign = SignUtils.generateSign(params, props.getSecretKey());
            params.put("sign", sign);

            Map<String, Object> response = client.post(props.estimateRateUrl(), params);
            return parseRateResponse(response, expressCategory);

        } catch (Exception e) {
            log.error("Flash Express API call failed: {}. Using fallback shipping fee: ₱{}",
                    e.getMessage(), FALLBACK_SHIPPING_FEE);
            return createFallbackResponse(expressCategory, dstPostalCode);
        }
    }

    private FlashShippingRateResponse parseRateResponse(Map<String, Object> body, int expressCategory) {
        if (body == null)
            throw new CustomException("Empty response from Flash Express");

        Object codeObj = body.get("code");
        int code = codeObj instanceof Number ? ((Number) codeObj).intValue() : 0;

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
        int pricePolicy = policyObj instanceof Number ? ((Number) policyObj).intValue() : 1;

        String pricePolicyText = pricePolicy == 2 ? "By dimensions" : "By weight";
        String expressLabel = switch (expressCategory) {
            case 2 -> "On-Time Delivery";
            case 4 -> "Bulky Delivery";
            default -> "Standard Delivery";
        };

        log.info("Flash Express rate: ₱{} ({})", shippingFee, expressLabel);

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

    /**
     * Check if Flash Express is properly configured
     */
    private boolean isFlashExpressConfigured() {
        return props.getMchId() != null &&
                !props.getMchId().isEmpty() &&
                !props.getMchId().contains("YOUR_") &&
                !props.getMchId().contains("AAXXXX") &&
                props.getSecretKey() != null &&
                !props.getSecretKey().isEmpty() &&
                !props.getSecretKey().contains("YOUR_") &&
                props.getBaseUrl() != null;
    }

    /**
     * Create a fallback response when Flash Express is unavailable
     */
    private FlashShippingRateResponse createFallbackResponse(int expressCategory, String dstPostalCode) {
        // Simple logic for remote areas (you can customize this)
        boolean isUpCountry = dstPostalCode != null &&
                (dstPostalCode.startsWith("4") ||
                        dstPostalCode.startsWith("5") ||
                        dstPostalCode.startsWith("9"));

        BigDecimal shippingFee = FALLBACK_SHIPPING_FEE;
        BigDecimal upCountryFee = isUpCountry ? FALLBACK_UPCOUNTRY_FEE : BigDecimal.ZERO;

        if (isUpCountry) {
            shippingFee = shippingFee.add(upCountryFee);
        }

        String expressLabel = switch (expressCategory) {
            case 2 -> "On-Time Delivery (Fallback)";
            case 4 -> "Bulky Delivery (Fallback)";
            default -> "Standard Delivery (Fallback)";
        };

        log.info("Using fallback shipping: ₱{} (upcountry: {})", shippingFee, isUpCountry);

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
}