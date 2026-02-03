package com.dms.liteflow.infrastructure.saga.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Saga 步骤执行实体（MyBatis）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecutionEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 步骤ID
     */
    private String stepId;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 状态
     */
    private String status;

    /**
     * 输入数据（JSON字符串）
     */
    private String inputData;

    /**
     * 输出数据（JSON字符串）
     */
    private String outputData;

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
    private LocalDateTime executedAt;

    /**
     * 补偿时间
     */
    private LocalDateTime compensatedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
