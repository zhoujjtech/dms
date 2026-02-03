package com.dms.liteflow.application.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Saga 执行请求
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecutionRequest {

    /**
     * 租户ID
     */
    private Long tenantId;

    /**
     * 流程链名称
     */
    private String chainName;

    /**
     * 输入数据
     */
    private Map<String, Object> inputData;

    /**
     * 是否启用 Saga 模式
     */
    @Builder.Default
    private Boolean sagaMode = true;

    /**
     * 超时时间（毫秒）
     */
    private Long timeoutMs;
}
