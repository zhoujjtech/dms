package com.dms.liteflow.domain.shared.kernel.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 组件ID值对象
 */
@Getter
@EqualsAndHashCode
public final class ComponentId {

    private final String value;

    private ComponentId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Component ID cannot be empty");
        }
        this.value = value.trim();
    }

    public static ComponentId of(String value) {
        return new ComponentId(value);
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "ComponentId{" + value + "}";
    }
}
