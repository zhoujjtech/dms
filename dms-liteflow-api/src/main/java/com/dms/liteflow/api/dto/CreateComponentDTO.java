package com.dms.liteflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建组件 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateComponentDTO {
    private Long tenantId;
    private String componentId;
    private String componentName;
    private String description;
    private String componentType;
    private String content;
}
