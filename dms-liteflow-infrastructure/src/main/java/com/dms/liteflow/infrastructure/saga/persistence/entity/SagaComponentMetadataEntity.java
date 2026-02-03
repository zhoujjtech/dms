package com.dms.liteflow.infrastructure.saga.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Saga 组件元数据实体（MyBatis）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaComponentMetadataEntity {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 补偿组件名称
     */
    private String compensateComponent;

    /**
     * 是否需要补偿
     */
    private Boolean needsCompensation;

    /**
     * 默认失败策略
     */
    private String defaultFailureStrategy;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeoutMs;

    /**
     * 扩展配置（JSON字符串）
     */
    private String metadata;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
