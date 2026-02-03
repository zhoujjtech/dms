package com.dms.liteflow.domain.saga.valueobject;

/**
 * Saga 失败处理动作类型
 *
 * @author DMS
 * @since 2026-02-03
 */
public enum ActionType {
    /**
     * 重试
     */
    RETRY,

    /**
     * 自动补偿
     */
    AUTO_COMPENSATE,

    /**
     * 人工介入
     */
    MANUAL,

    /**
     * 跳过
     */
    SKIP
}
