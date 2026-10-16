package com.dayan.food.config;

import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import java.time.Duration;

@Configuration
public class CacheTtlConfig {
    @Bean
    RedisCacheManagerBuilderCustomizer performanceCacheTtl() {
        return builder -> builder
                .withCacheConfiguration("foodDiscoveryCatalog", ttl(60))
                .withCacheConfiguration("foodDiscoveryCounts", ttl(60))
                .withCacheConfiguration("foodDiscoveryMap", ttl(30))
                .withCacheConfiguration("wishlistMatches", ttl(300));
    }

    private RedisCacheConfiguration ttl(long seconds) {
        return RedisCacheConfiguration.defaultCacheConfig().entryTtl(Duration.ofSeconds(seconds));
    }
}
