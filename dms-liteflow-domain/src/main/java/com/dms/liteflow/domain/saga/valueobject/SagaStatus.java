package com.dms.liteflow.domain.saga.valueobject;

/**
 * Saga 执行状态枚举
 *
 * @author DMS
 * @since 2026-02-03
 */
public enum SagaStatus {
    /**
     * 等待执行
     */
    PENDING,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 执行完成
     */
    COMPLETED,

    /**
     * 执行失败
     */
    FAILED,

    /**
     * 补偿中
     */
    COMPENSATING,

    /**
     * 已补偿
     */
    COMPENSATED,

    /**
     * 需要人工介入
     */
    MANUAL_INTERVENTION
}
