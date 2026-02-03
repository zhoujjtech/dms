package com.dms.liteflow.domain.saga.valueobject;

/**
 * Saga 步骤状态枚举
 *
 * @author DMS
 * @since 2026-02-03
 */
public enum StepStatus {
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
     * 已跳过
     */
    SKIPPED
}
