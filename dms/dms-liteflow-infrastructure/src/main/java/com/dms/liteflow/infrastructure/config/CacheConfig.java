package com.dms.liteflow.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 缓存配置
 * <p>
 * 使用 Redis 实现分布式缓存，支持多实例部署
 * </p>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Redis 缓存管理器
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 配置 JSON 序列化
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        // 配置缓存序列化
        RedisSerializationContext serializationContext = RedisSerializationContext.newSerializationContext()
                .keySerializer(new StringRedisSerializer())
                .valueSerializer(new GenericJackson2JsonRedisSerializer(objectMapper));

        // 配置缓存默认设置
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))  // 5分钟过期
                .disableCachingNullValues()       // 不缓存 null 值
                .serializeValuesWith(serializationContext)
                .prefixCacheNameWith("dms:cache");  // 统一前缀

        // 返回 RedisCacheManager
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .transactionAware()  // 支持事务
                .build();
    }
}
