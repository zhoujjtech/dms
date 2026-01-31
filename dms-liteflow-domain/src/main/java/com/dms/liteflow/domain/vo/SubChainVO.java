package com.dms.liteflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 子流程 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubChainVO {
    private Long id;
    private Long tenantId;
    private String subChainName;
    private String chainCode;
    private String description;
    private Long parentChainId;
    private String status;
    private Integer currentVersion;
    private String createdAt;
    private String updatedAt;
}
