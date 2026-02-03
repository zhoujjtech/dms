package com.dms.liteflow.infrastructure.saga.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Saga 配置属性
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Component
@ConfigurationProperties(prefix = "saga")
public class SagaProperties {

    /**
     * 是否启用 Saga 功能
     */
    private Boolean enabled = true;

    /**
     * 默认超时时间（毫秒）
     */
    private Long defaultTimeoutMs = 300000L; // 5分钟

    /**
     * 最大超时时间（毫秒）
     */
    private Long maxTimeoutMs = 600000L; // 10分钟

    /**
     * 补偿重试次数
     */
    private Integer compensationRetryCount = 3;

    /**
     * 补偿重试间隔（毫秒）
     */
    private Long compensationRetryIntervalMs = 1000L;

    /**
     * Redis 故障时是否降级到纯 MySQL 存储
     */
    private Boolean redisFallbackEnabled = true;

    /**
     * 数据清理配置
     */
    private DataCleanup dataCleanup = new DataCleanup();

    /**
     * 告警配置
     */
    private Alert alert = new Alert();

    @Data
    public static class DataCleanup {
        /**
         * Redis 数据保留天数
         */
        private Integer redisRetentionDays = 1;

        /**
         * MySQL 数据归档天数
         */
        private Integer mysqlArchiveDays = 90;
    }

    @Data
    public static class Alert {
        /**
         * 补偿失败率告警阈值（0-1）
         */
        private Double compensationFailureRateThreshold = 0.05;

        /**
         * 人工介入告警阈值（次/小时）
         */
        private Integer manualInterventionThreshold = 10;

        /**
         * 告警通知方式（逗号分隔）
         */
        private String notificationTypes = "email,slack";
    }
}
