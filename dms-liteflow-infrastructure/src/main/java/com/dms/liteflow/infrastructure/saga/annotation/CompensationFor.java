package com.dms.liteflow.infrastructure.saga.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 补偿组件注解
 * 标识该组件是某个业务组件的补偿组件
 *
 * @author DMS
 * @since 2026-02-03
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface CompensationFor {

    /**
     * 原始组件名称
     */
    String value();
}
