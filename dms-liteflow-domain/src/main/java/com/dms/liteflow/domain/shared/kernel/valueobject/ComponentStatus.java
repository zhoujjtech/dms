package com.dms.liteflow.domain.shared.kernel.valueobject;

import lombok.Getter;

/**
 * 配置状态枚举
 */
@Getter
public enum ComponentStatus {

    /**
     * 草稿
     */
    DRAFT("DRAFT", "草稿"),

    /**
     * 已发布
     */
    PUBLISHED("PUBLISHED", "已发布"),

    /**
     * 已归档
     */
    ARCHIVED("ARCHIVED", "已归档"),

    /**
     * 已启用
     */
    ENABLED("ENABLED", "已启用"),

    /**
     * 已禁用
     */
    DISABLED("DISABLED", "已禁用");

    private final String code;
    private final String description;

    ComponentStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public static ComponentStatus fromCode(String code) {
        for (ComponentStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + code);
    }
}
