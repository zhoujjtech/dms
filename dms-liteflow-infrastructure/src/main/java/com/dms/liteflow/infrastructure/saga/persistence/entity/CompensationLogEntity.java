package com.dms.liteflow.infrastructure.saga.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Saga 补偿日志实体（MyBatis）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationLogEntity {

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
     * 补偿组件名称
     */
    private String compensateComponent;

    /**
     * 状态
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
     * 操作人
     */
    private String operator;

    /**
     * 操作类型
     */
    private String operationType;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
