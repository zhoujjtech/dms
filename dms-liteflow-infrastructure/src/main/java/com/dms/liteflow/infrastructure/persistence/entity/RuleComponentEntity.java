package com.dms.liteflow.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规则组件实体（MyBatis）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleComponentEntity {

    private Long id;
    private Long tenantId;
    private String componentId;
    private String componentName;
    private String description;
    private String componentType;
    private String content; // 对应数据库的 component_code 字段
    private String status;

    // LiteFlow SQL 插件所需字段
    private String applicationName;
    private Integer scriptEnable;
    private String language;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
