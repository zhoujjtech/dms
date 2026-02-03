package com.dms.liteflow.application.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Saga 执行状态
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecutionStatus {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 流程链名称
     */
    private String chainName;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 当前步骤索引
     */
    private Integer currentStepIndex;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 结束时间
     */
    private String endTime;

    /**
     * 错误消息
     */
    private String errorMessage;
}
