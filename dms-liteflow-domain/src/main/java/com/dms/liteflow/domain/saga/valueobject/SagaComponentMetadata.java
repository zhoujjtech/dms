package com.dms.liteflow.domain.saga.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Saga 组件元数据值对象
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaComponentMetadata {

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 补偿组件名称
     */
    private String compensateComponent;

    /**
     * 是否需要补偿
     */
    private Boolean needsCompensation;

    /**
     * 默认失败策略
     */
    private ActionType defaultFailureStrategy;

    /**
     * 超时时间（毫秒）
     */
    private Integer timeoutMs;

    /**
     * 扩展配置
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 检查是否需要补偿
     */
    public boolean needsCompensation() {
        return this.needsCompensation != null && this.needsCompensation;
    }

    /**
     * 获取超时时间（默认 30 秒）
     */
    public int getTimeoutMs() {
        return this.timeoutMs != null ? this.timeoutMs : 30000;
    }
}
