package com.dms.liteflow.infrastructure.interceptor;

import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

/**
 * 租户上下文
 * <p>
 * 使用 ThreadLocal 存储当前请求的租户信息
 * </p>
 */
public class TenantContext {

    private static final ThreadLocal<TenantId> TENANT_ID = new ThreadLocal<>();

    /**
     * 设置租户ID
     */
    public static void setTenantId(TenantId tenantId) {
        TENANT_ID.set(tenantId);
    }

    /**
     * 获取租户ID
     */
    public static TenantId getTenantId() {
        return TENANT_ID.get();
    }

    /**
     * 清除租户ID
     */
    public static void clear() {
        TENANT_ID.remove();
    }
}
