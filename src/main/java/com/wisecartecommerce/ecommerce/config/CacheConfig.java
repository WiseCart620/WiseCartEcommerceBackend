package com.wisecartecommerce.ecommerce.config;

import java.util.concurrent.TimeUnit;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.github.benmanes.caffeine.cache.Caffeine;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager(
            // existing product caches
            "products", "activeProducts", "featuredProducts",
            "newArrivals", "topSelling", "categories",
            "categoryTree", "homepageSections",
            // review caches (TTLs handled per-cache via individual builders below)
            "reviews", "reviewSummary", "recentReviews"
        );
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(500));
        return manager;
    }
}