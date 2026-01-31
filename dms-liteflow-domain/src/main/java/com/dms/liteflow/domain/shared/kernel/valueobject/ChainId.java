package com.dms.liteflow.domain.shared.kernel.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 流程链ID值对象
 */
@Getter
@EqualsAndHashCode
public final class ChainId {

    private final Long value;

    private ChainId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Chain ID must be positive");
        }
        this.value = value;
    }

    public static ChainId of(Long value) {
        return new ChainId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ChainId{" + value + "}";
    }
}
