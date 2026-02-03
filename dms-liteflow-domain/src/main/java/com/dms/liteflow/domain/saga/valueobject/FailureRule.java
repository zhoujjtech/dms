package com.dms.liteflow.domain.saga.valueobject;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Saga 失败处理规则值对象
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FailureRule {

    /**
     * 错误条件（错误码或错误类型）
     * 例如：PAYMENT_TIMEOUT, INSUFFICIENT_FUNDS, RISK_CHECK_FAILED
     */
    private String condition;

    /**
     * 处理动作
     */
    private ActionType action;

    /**
     * 重试次数（当 action = RETRY 时）
     */
    @Builder.Default
    private Integer retryCount = 3;

    /**
     * 是否匹配错误
     */
    public boolean matches(String errorCode) {
        if (this.condition == null || errorCode == null) {
            return false;
        }
        // 支持通配符匹配
        if (this.condition.equals("*")) {
            return true;
        }
        return this.condition.equalsIgnoreCase(errorCode);
    }
}
