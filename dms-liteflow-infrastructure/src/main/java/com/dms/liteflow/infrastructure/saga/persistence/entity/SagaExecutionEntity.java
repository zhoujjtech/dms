package com.dms.liteflow.infrastructure.saga.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Saga 执行实例实体（MyBatis）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecutionEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 流程链名称
     */
    private String chainName;

    /**
     * 状态
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
     * 输入数据（JSON字符串）
     */
    private String inputData;

    /**
     * 输出数据（JSON字符串）
     */
    private String outputData;

    /**
     * 执行栈（JSON字符串）
     */
    private String executionStack;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;
}
