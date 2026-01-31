package com.dms.liteflow.domain.flowexec.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 子流程实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowSubChain {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 子流程名称
     */
    private String subChainName;

    /**
     * EL表达式
     */
    private String chainCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 父流程链ID
     */
    private Long parentChainId;

    /**
     * 状态
     */
    private String status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 激活子流程
     */
    public void activate() {
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 停用子流程
     */
    public void deactivate() {
        this.status = "INACTIVE";
        this.updatedAt = LocalDateTime.now();
    }
}
