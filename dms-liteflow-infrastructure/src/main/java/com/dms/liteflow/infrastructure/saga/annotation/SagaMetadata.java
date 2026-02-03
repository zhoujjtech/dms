package com.dms.liteflow.infrastructure.saga.annotation;

import com.dms.liteflow.domain.saga.valueobject.ActionType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Saga 组件元数据注解
 * 声明组件的 Saga 补偿相关配置
 *
 * @author DMS
 * @since 2026-02-03
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface SagaMetadata {

    /**
     * 补偿组件名称
     */
    String compensateComponent() default "";

    /**
     * 是否需要补偿
     */
    boolean needsCompensation() default false;

    /**
     * 默认失败策略
     */
    ActionType defaultFailureStrategy() default ActionType.AUTO_COMPENSATE;

    /**
     * 超时时间（毫秒）
     */
    long timeoutMs() default 30000;

    /**
     * 失败规则列表
     */
    FailureRule[] failureRules() default {};

    /**
     * 失败规则定义
     */
    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface FailureRule {
        /**
         * 错误条件（错误码）
         */
        String condition();

        /**
         * 处理动作
         */
        ActionType action();

        /**
         * 重试次数（当 action = RETRY 时）
         */
        int retryCount() default 3;
    }
}
