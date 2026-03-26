package com.wisecartecommerce.ecommerce.service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class RateLimitService {

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(100_000)
            .build();

    // 5 attempts per minute
    public Bucket loginBucket(String ip) {
        return buckets.get("login:" + ip, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1))))
                .build());
    }

    // 3 attempts per hour per IP
    public Bucket forgotPasswordBucket(String ip) {
        return buckets.get("forgot:" + ip, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1))))
                .build());
    }

    // 10 per minute for coupon probing
    public Bucket couponBucket(String userId) {
        return buckets.get("coupon:" + userId, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1))))
                .build());
    }

    // 5 orders per hour per user
    public Bucket orderBucket(String userId) {
        return buckets.get("order:" + userId, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1))))
                .build());
    }

    // 3 contact submissions per hour per IP
    public Bucket contactBucket(String ip) {
        return buckets.get("contact:" + ip, k -> Bucket.builder()
                .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1))))
                .build());
    }

    public boolean tryConsume(Bucket bucket) {
        return bucket.tryConsume(1);
    }
}