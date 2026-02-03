package com.dms.liteflow.infrastructure.saga.redis;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.saga.config.SagaRedisKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Saga Redis 服务
 * 提供 Saga 执行数据的 Redis 缓存操作
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaRedisService {

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /**
     * 保存执行实例到 Redis
     */
    public void saveExecution(Long tenantId, String executionId, SagaExecution sagaExecution) {
        try {
            String key = SagaRedisKeys.executionKey(tenantId, executionId);
            String json = objectMapper.writeValueAsString(sagaExecution);

            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(json);
            bucket.expire(Duration.ofSeconds(SagaRedisKeys.EXECUTION_TTL));

            log.debug("Saved execution to Redis: key={}", key);
        } catch (Exception e) {
            log.error("Failed to save execution to Redis: executionId={}", executionId, e);
            throw new RuntimeException("Failed to save execution to Redis", e);
        }
    }

    /**
     * 从 Redis 获取执行实例
     */
    public SagaExecution getExecution(Long tenantId, String executionId) {
        try {
            String key = SagaRedisKeys.executionKey(tenantId, executionId);
            RBucket<String> bucket = redissonClient.getBucket(key);
            String json = bucket.get();

            if (json == null) {
                log.debug("Execution not found in Redis: key={}", key);
                return null;
            }

            return objectMapper.readValue(json, SagaExecution.class);
        } catch (Exception e) {
            log.error("Failed to get execution from Redis: executionId={}", executionId, e);
            return null;
        }
    }

    /**
     * 删除执行实例
     */
    public void deleteExecution(Long tenantId, String executionId) {
        try {
            String key = SagaRedisKeys.executionKey(tenantId, executionId);
            redissonClient.getBucket(key).delete();
            log.debug("Deleted execution from Redis: key={}", key);
        } catch (Exception e) {
            log.error("Failed to delete execution from Redis: executionId={}", executionId, e);
        }
    }

    /**
     * 保存执行栈到 Redis
     */
    public void saveExecutionStack(Long tenantId, String executionId, List<StepExecution> stack) {
        try {
            String key = SagaRedisKeys.executionStackKey(tenantId, executionId);
            String json = objectMapper.writeValueAsString(stack);

            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(json);
            bucket.expire(Duration.ofSeconds(SagaRedisKeys.EXECUTION_TTL));

            log.debug("Saved execution stack to Redis: key={}, size={}", key, stack.size());
        } catch (Exception e) {
            log.error("Failed to save execution stack to Redis: executionId={}", executionId, e);
            throw new RuntimeException("Failed to save execution stack to Redis", e);
        }
    }

    /**
     * 从 Redis 获取执行栈
     */
    public List<StepExecution> getExecutionStack(Long tenantId, String executionId) {
        try {
            String key = SagaRedisKeys.executionStackKey(tenantId, executionId);
            RBucket<String> bucket = redissonClient.getBucket(key);
            String json = bucket.get();

            if (json == null) {
                log.debug("Execution stack not found in Redis: key={}", key);
                return null;
            }

            return objectMapper.readValue(json,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, StepExecution.class));
        } catch (Exception e) {
            log.error("Failed to get execution stack from Redis: executionId={}", executionId, e);
            return null;
        }
    }

    /**
     * 删除执行栈
     */
    public void deleteExecutionStack(Long tenantId, String executionId) {
        try {
            String key = SagaRedisKeys.executionStackKey(tenantId, executionId);
            redissonClient.getBucket(key).delete();
            log.debug("Deleted execution stack from Redis: key={}", key);
        } catch (Exception e) {
            log.error("Failed to delete execution stack from Redis: executionId={}", executionId, e);
        }
    }

    /**
     * 保存步骤执行状态
     */
    public void saveStep(Long tenantId, String executionId, String stepId, StepExecution stepExecution) {
        try {
            String key = SagaRedisKeys.stepKey(tenantId, executionId, stepId);
            String json = objectMapper.writeValueAsString(stepExecution);

            RBucket<String> bucket = redissonClient.getBucket(key);
            bucket.set(json);
            bucket.expire(Duration.ofSeconds(SagaRedisKeys.EXECUTION_TTL));

            log.debug("Saved step to Redis: key={}", key);
        } catch (Exception e) {
            log.error("Failed to save step to Redis: stepId={}", stepId, e);
            throw new RuntimeException("Failed to save step to Redis", e);
        }
    }

    /**
     * 从 Redis 获取步骤执行状态
     */
    public StepExecution getStep(Long tenantId, String executionId, String stepId) {
        try {
            String key = SagaRedisKeys.stepKey(tenantId, executionId, stepId);
            RBucket<String> bucket = redissonClient.getBucket(key);
            String json = bucket.get();

            if (json == null) {
                return null;
            }

            return objectMapper.readValue(json, StepExecution.class);
        } catch (Exception e) {
            log.error("Failed to get step from Redis: stepId={}", stepId, e);
            return null;
        }
    }

    /**
     * 检查执行实例是否存在
     */
    public boolean existsExecution(Long tenantId, String executionId) {
        try {
            String key = SagaRedisKeys.executionKey(tenantId, executionId);
            return redissonClient.getBucket(key).isExists();
        } catch (Exception e) {
            log.error("Failed to check execution existence in Redis: executionId={}", executionId, e);
            return false;
        }
    }

    /**
     * 设置执行实例的过期时间
     */
    public void expireExecution(Long tenantId, String executionId, long ttl, TimeUnit timeUnit) {
        try {
            String key = SagaRedisKeys.executionKey(tenantId, executionId);
            redissonClient.getBucket(key).expire(Duration.ofMillis(timeUnit.toMillis(ttl)));
            log.debug("Set expiration for execution: key={}, ttl={} {}", key, ttl, timeUnit);
        } catch (Exception e) {
            log.error("Failed to set expiration for execution: executionId={}", executionId, e);
        }
    }

    /**
     * 清理过期的执行数据
     */
    public void cleanupExpiredExecutions(Long tenantId, long beforeTimestamp) {
        try {
            // 由于 Redis 的 keys 命令不建议在生产环境使用，
            // 这里应该使用 scan 命令或者依赖 TTL 自动清理
            log.info("Redis auto-cleanup enabled, relying on TTL for expired executions");
        } catch (Exception e) {
            log.error("Failed to cleanup expired executions", e);
        }
    }

    // ====== Overloaded methods using value objects ======

    /**
     * 删除执行实例（使用值对象）
     */
    public void deleteExecution(SagaExecutionId executionId) {
        deleteExecution(1L, executionId.getValue()); // 使用系统租户ID
    }

    /**
     * 删除执行实例（使用值对象和租户ID）
     */
    public void deleteExecution(TenantId tenantId, SagaExecutionId executionId) {
        deleteExecution(tenantId.getValue(), executionId.getValue());
    }

    /**
     * 获取执行实例（使用值对象）
     */
    public SagaExecution getExecution(TenantId tenantId, SagaExecutionId executionId) {
        return getExecution(tenantId.getValue(), executionId.getValue());
    }

    /**
     * 保存执行实例（使用值对象）
     */
    public void saveExecution(TenantId tenantId, SagaExecutionId executionId, SagaExecution sagaExecution) {
        saveExecution(tenantId.getValue(), executionId.getValue(), sagaExecution);
    }

    /**
     * 检查执行实例是否存在（使用值对象）
     */
    public boolean existsExecution(TenantId tenantId, SagaExecutionId executionId) {
        return existsExecution(tenantId.getValue(), executionId.getValue());
    }
}
