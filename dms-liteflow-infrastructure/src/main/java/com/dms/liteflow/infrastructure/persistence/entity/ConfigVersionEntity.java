package com.dms.liteflow.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 配置版本实体（MyBatis）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigVersionEntity {

    private Long id;
    private Long tenantId;
    private String configType;
    private Long configId;
    private Integer version;
    private String content;
    private String status;
    private String createdBy;
    private LocalDateTime createdAt;
}
