package com.dms.liteflow.domain.shared.kernel.valueobject;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Objects;

/**
 * 租户ID值对象
 * <p>
 * 共享内核的一部分，在所有限界上下文中使用
 * </p>
 */
@Getter
@EqualsAndHashCode
public final class TenantId {

    private final Long value;

    private TenantId(Long value) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException("Tenant ID must be positive");
        }
        this.value = value;
    }

    public static TenantId of(Long value) {
        return new TenantId(value);
    }

    public Long getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "TenantId{" + value + "}";
    }
}
