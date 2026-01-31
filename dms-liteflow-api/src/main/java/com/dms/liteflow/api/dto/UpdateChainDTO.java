package com.dms.liteflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 更新流程链 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChainDTO {
    private String chainCode;
    private String description;
}
