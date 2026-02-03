package com.dms.liteflow.domain.saga.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Saga 补偿日志值对象
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationLog {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 步骤ID
     */
    private String stepId;

    /**
     * 补偿组件名称
     */
    private String compensateComponent;

    /**
     * 补偿状态：SUCCESS/FAILED/SKIPPED
     */
    private String status;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 补偿时间
     */
    private LocalDateTime compensatedAt;

    /**
     * 操作人（如果是手动触发）
     */
    private String operator;

    /**
     * 操作类型：AUTO/MANUAL
     */
    @Builder.Default
    private String operationType = "AUTO";

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equalsIgnoreCase(this.status);
    }

    /**
     * 检查是否失败
     */
    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(this.status);
    }

    /**
     * 检查是否为手动操作
     */
    public boolean isManual() {
        return "MANUAL".equalsIgnoreCase(this.operationType);
    }

    /**
     * 创建成功日志
     */
    public static CompensationLog success(String executionId, String stepId, String compensateComponent) {
        return CompensationLog.builder()
                .executionId(executionId)
                .stepId(stepId)
                .compensateComponent(compensateComponent)
                .status("SUCCESS")
                .compensatedAt(LocalDateTime.now())
                .operationType("AUTO")
                .build();
    }

    /**
     * 创建失败日志
     */
    public static CompensationLog failure(String executionId, String stepId, String compensateComponent, String errorMessage) {
        return CompensationLog.builder()
                .executionId(executionId)
                .stepId(stepId)
                .compensateComponent(compensateComponent)
                .status("FAILED")
                .errorMessage(errorMessage)
                .compensatedAt(LocalDateTime.now())
                .operationType("AUTO")
                .build();
    }

    /**
     * 创建手动操作日志
     */
    public static CompensationLog manual(String executionId, String stepId, String compensateComponent, String operator) {
        return CompensationLog.builder()
                .executionId(executionId)
                .stepId(stepId)
                .compensateComponent(compensateComponent)
                .status("SUCCESS")
                .compensatedAt(LocalDateTime.now())
                .operator(operator)
                .operationType("MANUAL")
                .build();
    }
}
