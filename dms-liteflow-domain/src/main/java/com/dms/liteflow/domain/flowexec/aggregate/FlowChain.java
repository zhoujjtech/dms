package com.dms.liteflow.domain.flowexec.aggregate;

import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 流程链聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowChain {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private TenantId tenantId;

    /**
     * 流程链名称
     */
    private String chainName;

    /**
     * EL表达式
     */
    private String chainCode;

    /**
     * 描述
     */
    private String description;

    /**
     * 配置类型
     */
    private String configType;

    /**
     * 状态
     */
    private ComponentStatus status;

    /**
     * 当前版本号
     */
    private Integer currentVersion;

    /**
     * 是否启用流程级事务
     */
    private Boolean transactional;

    /**
     * 事务超时时间(秒)
     */
    private Integer transactionTimeout;

    /**
     * 事务传播行为
     */
    private String transactionPropagation;

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
     * 发布流程链
     */
    public void publish() {
        if (this.status == ComponentStatus.PUBLISHED) {
            throw new IllegalStateException("Flow chain is already published");
        }
        this.status = ComponentStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 归档流程链
     */
    public void archive() {
        if (this.status == ComponentStatus.ARCHIVED) {
            throw new IllegalStateException("Flow chain is already archived");
        }
        this.status = ComponentStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 启用流程链
     */
    public void enable() {
        this.status = ComponentStatus.ENABLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 禁用流程链
     */
    public void disable() {
        this.status = ComponentStatus.DISABLED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新流程链
     */
    public void updateChain(String newChainCode, String newDescription) {
        if (this.status == ComponentStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot update published chain. Create a new version instead.");
        }
        this.chainCode = newChainCode;
        this.description = newDescription;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 软删除
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
