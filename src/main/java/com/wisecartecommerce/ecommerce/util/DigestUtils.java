package com.wisecartecommerce.ecommerce.util;

import java.security.MessageDigest;

public class DigestUtils {
    private static final String SHA256_ALGORITHM_NAME = "SHA-256";
    private static final char[] HEX_CHARS = {'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f'};

    public static String sha256HexDigest(String source) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(SHA256_ALGORITHM_NAME);
            messageDigest.update(source.getBytes("UTF-8"));
            return new String(encodeHex(messageDigest.digest()));
        } catch (Exception e) {
            throw new RuntimeException("SHA256 failed", e);
        }
    }

    private static char[] encodeHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < chars.length; i = i + 2) {
            byte b = bytes[i / 2];
            chars[i]     = HEX_CHARS[(b >>> 0x4) & 0xf];
            chars[i + 1] = HEX_CHARS[b & 0xf];
        }
        return chars;
    }
}