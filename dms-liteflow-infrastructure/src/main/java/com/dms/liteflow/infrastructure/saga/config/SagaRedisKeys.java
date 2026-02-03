package com.dms.liteflow.infrastructure.saga.config;

/**
 * Saga Redis Key 命名规范
 *
 * @author DMS
 * @since 2026-02-03
 */
public class SagaRedisKeys {

    /**
     * Saga 执行实例 Key
     * <p>格式: saga:execution:{tenantId}:{executionId}</p>
     * <p>TTL: 24小时</p>
     */
    public static final String EXECUTION_KEY = "saga:execution:%d:%s";

    /**
     * Saga 执行栈 Key
     * <p>格式: saga:execution:{tenantId}:{executionId}:stack</p>
     * <p>TTL: 24小时</p>
     */
    public static final String EXECUTION_STACK_KEY = "saga:execution:%d:%s:stack";

    /**
     * Saga 分布式锁 Key
     * <p>格式: saga:lock:{executionId}</p>
     * <p>TTL: 30秒</p>
     */
    public static final String LOCK_KEY = "saga:lock:%s";

    /**
     * Saga 查询结果缓存 Key
     * <p>格式: saga:execution:query:{tenantId}:{queryHash}</p>
     * <p>TTL: 5分钟</p>
     */
    public static final String QUERY_CACHE_KEY = "saga:execution:query:%d:%s";

    /**
     * Saga 步骤执行状态 Key
     * <p>格式: saga:step:{tenantId}:{executionId}:{stepId}</p>
     * <p>TTL: 24小时</p>
     */
    public static final String STEP_KEY = "saga:step:%d:%s:%s";

    /**
     * Saga 元数据缓存 Key
     * <p>格式: saga:metadata:{tenantId}:{componentName}</p>
     * <p>TTL: 永久</p>
     */
    public static final String METADATA_KEY = "saga:metadata:%d:%s";

    // TTL 配置（秒）

    /**
     * 执行数据 TTL: 24小时
     */
    public static final long EXECUTION_TTL = 24 * 60 * 60;

    /**
     * 锁 TTL: 30秒
     */
    public static final long LOCK_TTL = 30;

    /**
     * 查询缓存 TTL: 5分钟
     */
    public static final long QUERY_CACHE_TTL = 5 * 60;

    /**
     * 元数据 TTL: 永久（-1）
     */
    public static final long METADATA_TTL = -1;

    private SagaRedisKeys() {
        // 防止实例化
    }

    /**
     * 构建执行实例 Key
     */
    public static String executionKey(Long tenantId, String executionId) {
        return String.format(EXECUTION_KEY, tenantId, executionId);
    }

    /**
     * 构建执行栈 Key
     */
    public static String executionStackKey(Long tenantId, String executionId) {
        return String.format(EXECUTION_STACK_KEY, tenantId, executionId);
    }

    /**
     * 构建锁 Key
     */
    public static String lockKey(String executionId) {
        return String.format(LOCK_KEY, executionId);
    }

    /**
     * 构建步骤执行状态 Key
     */
    public static String stepKey(Long tenantId, String executionId, String stepId) {
        return String.format(STEP_KEY, tenantId, executionId, stepId);
    }

    /**
     * 构建元数据 Key
     */
    public static String metadataKey(Long tenantId, String componentName) {
        return String.format(METADATA_KEY, tenantId, componentName);
    }
}
