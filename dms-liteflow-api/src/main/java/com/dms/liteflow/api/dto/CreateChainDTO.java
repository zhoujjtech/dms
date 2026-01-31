package com.dms.liteflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建流程链 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChainDTO {
    private Long tenantId;
    private String chainName;
    private String chainCode;
    private String description;
    private String configType;
    private Boolean transactional;
    private Integer transactionTimeout;
    private String transactionPropagation;
}
