package com.wisecartecommerce.ecommerce.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class FlashExpressSignatureUtil {

    /**
     * Generates SHA256 signature per Flash Express spec:
     * 1. Sort params by ASCII order (TreeMap)
     * 2. Skip blank/null values
     * 3. Concatenate as key=value&key=value...&key={apiKey}
     * 4. SHA256 → uppercase
     */
    public static String generateSign(Map<String, String> params, String apiKey) {
        // TreeMap sorts keys by ASCII (natural order)
        Map<String, String> sorted = new TreeMap<>(params);

        String stringA = sorted.entrySet().stream()
                .filter(e -> e.getValue() != null && !e.getValue().isBlank())
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        String stringSignTemp = stringA + "&key=" + apiKey;

        return sha256(stringSignTemp).toUpperCase();
    }

    public static String generateNonce() {
        return String.valueOf(System.currentTimeMillis());
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA256 failed", e);
        }
    }
}