package com.dms.liteflow.api.saga.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 步骤执行详情 VO
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionDetailVO {

    /**
     * 步骤ID
     */
    private String stepId;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 输入数据
     */
    private Object inputData;

    /**
     * 输出数据
     */
    private Object outputData;

    /**
     * 补偿组件名称
     */
    private String compensateComponent;

    /**
     * 是否需要补偿
     */
    private Boolean needsCompensation;

    /**
     * 错误码
     */
    private String errorCode;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 异常堆栈
     */
    private String stackTrace;

    /**
     * 执行时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime executedAt;

    /**
     * 补偿时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime compensatedAt;

    /**
     * 执行时长（毫秒）
     */
    private Long executionDurationMs;

    /**
     * 从领域模型转换
     */
    public static StepExecutionDetailVO fromDomain(com.dms.liteflow.domain.saga.entity.StepExecution step) {
        return StepExecutionDetailVO.builder()
                .stepId(step.getStepId().getValue())
                .componentName(step.getComponentName())
                .status(step.getStatus().name())
                .inputData(step.getInputData())
                .outputData(step.getOutputData())
                .compensateComponent(step.getCompensateComponent())
                .needsCompensation(step.getNeedsCompensation())
                .errorCode(step.getErrorCode())
                .errorMessage(step.getErrorMessage())
                .stackTrace(step.getStackTrace())
                .executedAt(step.getExecutedAt())
                .compensatedAt(step.getCompensatedAt())
                .executionDurationMs(step.getExecutionDurationMs())
                .build();
    }
}
