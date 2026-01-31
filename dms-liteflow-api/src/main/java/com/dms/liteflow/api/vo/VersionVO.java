package com.dms.liteflow.api.vo;

import com.dms.liteflow.domain.version.aggregate.ConfigVersion;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 版本 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VersionVO {
    private Long id;
    private Long tenantId;
    private String configType;
    private Long configId;
    private Integer version;
    private String content;
    private String status;
    private String createdBy;
    private String createdAt;

    public static VersionVO fromDomain(ConfigVersion version) {
        return VersionVO.builder()
                .id(version.getId())
                .tenantId(version.getTenantId().getValue())
                .configType(version.getConfigType())
                .configId(version.getConfigId())
                .version(version.getVersion())
                .content(version.getContent())
                .status(version.getStatus().getCode())
                .createdBy(version.getCreatedBy())
                .createdAt(version.getCreatedAt() != null ? version.getCreatedAt().toString() : null)
                .build();
    }
}
