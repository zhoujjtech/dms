package com.dms.liteflow.domain.shared.kernel.valueobject;

import lombok.Getter;

/**
 * 组件类型枚举
 */
@Getter
public enum ComponentType {

    /**
     * 业务组件
     */
    BUSINESS("BUSINESS", "业务组件"),

    /**
     * 条件组件
     */
    CONDITION("CONDITION", "条件组件"),

    /**
     * 循环组件
     */
    LOOP("LOOP", "循环组件");

    private final String code;
    private final String description;

    ComponentType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ComponentType fromCode(String code) {
        for (ComponentType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown component type: " + code);
    }
}
