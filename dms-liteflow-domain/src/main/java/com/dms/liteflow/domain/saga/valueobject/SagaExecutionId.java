package com.dms.liteflow.domain.saga.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.UUID;

/**
 * Saga 执行 ID 值对象
 *
 * @author DMS
 * @since 2026-02-03
 */
@Getter
@EqualsAndHashCode
public class SagaExecutionId {

    private final String value;

    private SagaExecutionId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("SagaExecutionId cannot be null or empty");
        }
        this.value = value;
    }

    /**
     * 生成新的执行 ID
     */
    public static SagaExecutionId generate() {
        return new SagaExecutionId("exec-" + UUID.randomUUID().toString().replace("-", ""));
    }

    /**
     * 从已有值创建
     */
    public static SagaExecutionId of(String value) {
        return new SagaExecutionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
