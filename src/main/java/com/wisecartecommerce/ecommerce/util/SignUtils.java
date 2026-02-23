package com.wisecartecommerce.ecommerce.util;

import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;

public class SignUtils {

    public static String generateSign(Map<String, String> paramMap, String secretKey) {
        Map<String, String> acsMap = sortMapByKey(paramMap);
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : acsMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            // Skip blank values — matches Flash's StringUtils.isNotBlank check
            if (value != null && !value.isBlank()) {
                sb.append(key).append("=").append(value).append("&");
            }
        }
        sb.append("key=").append(secretKey);
        return DigestUtils.sha256HexDigest(sb.toString()).toUpperCase();
    }

    public static boolean checkSign(Map<String, String> dataMap, String secretKey) {
        Map<String, String> tempMap = new TreeMap<>(dataMap);
        if (tempMap.containsKey("sign")) {
            String sign = tempMap.remove("sign");
            String correctSign = generateSign(tempMap, secretKey);
            return correctSign.equals(sign);
        }
        return false;
    }

    private static Map<String, String> sortMapByKey(Map<String, String> map) {
        if (map == null || map.isEmpty()) return new TreeMap<>();
        Map<String, String> sortMap = new TreeMap<>(Comparator.naturalOrder());
        sortMap.putAll(map);
        return sortMap;
    }
}