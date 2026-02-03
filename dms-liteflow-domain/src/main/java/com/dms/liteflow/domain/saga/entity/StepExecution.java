package com.dms.liteflow.domain.saga.entity;

import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.saga.valueobject.StepStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Saga 步骤执行实体
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StepExecution {

    /**
     * 主键ID（数据库自增）
     */
    private Long id;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 步骤ID
     */
    private StepId stepId;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 步骤状态
     */
    private StepStatus status;

    /**
     * 输入数据（JSON）
     */
    private Map<String, Object> inputData;

    /**
     * 输出数据（JSON）
     */
    private Map<String, Object> outputData;

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

    /**
     * 开始执行
     */
    public void start() {
        if (this.status != null && this.status != StepStatus.RUNNING) {
            throw new IllegalStateException("Step already started or completed");
        }
        this.status = StepStatus.RUNNING;
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 完成执行
     */
    public void complete(Map<String, Object> outputData) {
        if (this.status != StepStatus.RUNNING) {
            throw new IllegalStateException("Step is not running");
        }
        this.status = StepStatus.COMPLETED;
        this.outputData = outputData != null ? new HashMap<>(outputData) : new HashMap<>();
    }

    /**
     * 执行失败
     */
    public void fail(String errorCode, String errorMessage, String stackTrace) {
        if (this.status != StepStatus.RUNNING) {
            throw new IllegalStateException("Step is not running");
        }
        this.status = StepStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
    }

    /**
     * 跳过步骤
     */
    public void skip() {
        if (this.status != StepStatus.RUNNING) {
            throw new IllegalStateException("Step is not running");
        }
        this.status = StepStatus.SKIPPED;
    }

    /**
     * 补偿完成
     */
    public void compensate() {
        if (this.compensatedAt != null) {
            throw new IllegalStateException("Step already compensated");
        }
        this.compensatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否已补偿
     */
    public boolean isCompensated() {
        return this.compensatedAt != null;
    }

    /**
     * 检查是否需要补偿
     */
    public boolean needsCompensation() {
        return this.needsCompensation != null && this.needsCompensation && !isCompensated();
    }

    /**
     * 获取执行时长（毫秒）
     */
    public Long getExecutionDurationMs() {
        if (this.executedAt == null) {
            return 0L;
        }
        LocalDateTime end = this.compensatedAt != null ? this.compensatedAt : LocalDateTime.now();
        return java.time.Duration.between(this.executedAt, end).toMillis();
    }

    /**
     * 创建新的 StepExecution 实例
     */
    public static StepExecution create(String executionId, StepId stepId, String componentName,
                                        Map<String, Object> inputData, String compensateComponent, Boolean needsCompensation) {
        return StepExecution.builder()
                .executionId(executionId)
                .stepId(stepId)
                .componentName(componentName)
                .status(StepStatus.RUNNING)
                .inputData(inputData != null ? new HashMap<>(inputData) : new HashMap<>())
                .compensateComponent(compensateComponent)
                .needsCompensation(needsCompensation)
                .executedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
    }
}
