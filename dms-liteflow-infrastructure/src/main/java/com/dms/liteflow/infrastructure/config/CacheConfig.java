package com.dms.liteflow.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置
 * <p>
 * 使用 Caffeine 实现本地缓存
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 缓存管理器
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 初始容量
                .initialCapacity(100)
                // 最大容量
                .maximumSize(1000)
                // 写入后过期时间（5分钟）
                .expireAfterWrite(5, TimeUnit.MINUTES)
                // 记录统计信息
                .recordStats()
        );
        return cacheManager;
    }
}
