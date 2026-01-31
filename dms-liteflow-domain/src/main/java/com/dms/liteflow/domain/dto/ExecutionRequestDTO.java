package com.dms.liteflow.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程执行请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRequestDTO {
    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 流程链名称
     */
    private String chainName;

    /**
     * 输入数据（JSON格式）
     */
    private String inputData;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeoutMs;

    /**
     * 是否异步执行
     */
    private Boolean async;
}
