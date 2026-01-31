package com.dms.liteflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程执行状态 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStatusVO {
    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 进度百分比
     */
    private Integer progress;

    /**
     * 当前执行步骤
     */
    private String currentStep;

    /**
     * 已执行步骤数
     */
    private Integer completedSteps;

    /**
     * 总步骤数
     */
    private Integer totalSteps;

    /**
     * 开始时间
     */
    private String startTime;

    /**
     * 预计剩余时间（毫秒）
     */
    private Long estimatedRemainingTimeMs;
}
