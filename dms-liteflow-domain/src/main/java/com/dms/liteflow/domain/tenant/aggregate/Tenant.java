package com.dms.liteflow.domain.tenant.aggregate;

import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 租户聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tenant {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户编码
     */
    private String tenantCode;

    /**
     * 租户名称
     */
    private String tenantName;

    /**
     * 状态 (ACTIVE/SUSPENDED/DELETED)
     */
    private String status;

    /**
     * 最大流程链数量
     */
    private Integer maxChains;

    /**
     * 最大组件数量
     */
    private Integer maxComponents;

    /**
     * Executor是否已缓存
     */
    private Boolean executorCached;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 删除时间（软删除）
     */
    private LocalDateTime deletedAt;

    /**
     * 激活租户
     */
    public void activate() {
        if ("ACTIVE".equals(this.status)) {
            throw new IllegalStateException("Tenant is already active");
        }
        this.status = "ACTIVE";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 暂停租户
     */
    public void suspend() {
        if ("SUSPENDED".equals(this.status)) {
            throw new IllegalStateException("Tenant is already suspended");
        }
        this.status = "SUSPENDED";
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否激活
     */
    public boolean isActive() {
        return "ACTIVE".equals(this.status);
    }

    /**
     * 检查是否已暂停
     */
    public boolean isSuspended() {
        return "SUSPENDED".equals(this.status);
    }

    /**
     * 检查流程链数量是否超限
     */
    public boolean isChainLimitExceeded(long currentCount) {
        return currentCount >= this.maxChains;
    }

    /**
     * 检查组件数量是否超限
     */
    public boolean isComponentLimitExceeded(long currentCount) {
        return currentCount >= this.maxComponents;
    }

    /**
     * 标记Executor已缓存
     */
    public void markExecutorCached() {
        this.executorCached = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记Executor未缓存
     */
    public void markExecutorNotCached() {
        this.executorCached = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 软删除
     */
    public void softDelete() {
        this.status = "DELETED";
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
