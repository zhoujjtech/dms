package com.dms.liteflow.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建子流程 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSubChainDTO {
    private Long tenantId;
    private String subChainName;
    private String chainCode;
    private String description;
    private Long parentChainId;
}
