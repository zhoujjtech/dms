package com.dms.liteflow.domain.saga.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Saga 步骤 ID 值对象
 *
 * @author DMS
 * @since 2026-02-03
 */
@Getter
@EqualsAndHashCode
public class StepId {

    private final String value;

    private StepId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("StepId cannot be null or empty");
        }
        this.value = value;
    }

    /**
     * 生成新的步骤 ID
     */
    public static StepId generate() {
        return new StepId("step-" + System.currentTimeMillis());
    }

    /**
     * 从已有值创建
     */
    public static StepId of(String value) {
        return new StepId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
