package com.dms.liteflow.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 流程链实体（MyBatis）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowChainEntity {

    private Long id;
    private Long tenantId;
    private String chainName;
    private String chainCode;
    private String description;
    private String configType;
    private String status;
    private Integer currentVersion;
    private Boolean transactional;
    private Integer transactionTimeout;
    private String transactionPropagation;

    // LiteFlow SQL 插件所需字段
    private String applicationName;
    private Integer chainEnable;
    private String namespace;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
