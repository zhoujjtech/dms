package com.dms.liteflow.api.vo;

import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组件 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentVO {
    private Long id;
    private Long tenantId;
    private String componentId;
    private String componentName;
    private String description;
    private String componentType;
    private String content;
    private String status;
    private Integer currentVersion;

    public static ComponentVO fromDomain(RuleComponent component) {
        return ComponentVO.builder()
                .id(component.getId())
                .tenantId(component.getTenantId().getValue())
                .componentId(component.getComponentId().getValue())
                .componentName(component.getComponentName())
                .description(component.getDescription())
                .componentType(component.getComponentType().getCode())
                .content(component.getContent())
                .status(component.getStatus().getCode())
                .currentVersion(0)
                .build();
    }
}
