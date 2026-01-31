package com.dms.liteflow.domain.version.aggregate;

import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 配置版本聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfigVersion {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private TenantId tenantId;

    /**
     * 配置类型 (COMPONENT/CHAIN/SUB_CHAIN)
     */
    private String configType;

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 版本号
     */
    private Integer version;

    /**
     * 配置内容
     */
    private String content;

    /**
     * 状态
     */
    private ComponentStatus status;

    /**
     * 创建人
     */
    private String createdBy;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 发布版本
     */
    public void publish() {
        if (this.status == ComponentStatus.PUBLISHED) {
            throw new IllegalStateException("Version is already published");
        }
        this.status = ComponentStatus.PUBLISHED;
    }

    /**
     * 归档版本
     */
    public void archive() {
        if (this.status == ComponentStatus.ARCHIVED) {
            throw new IllegalStateException("Version is already archived");
        }
        this.status = ComponentStatus.ARCHIVED;
    }

    /**
     * 检查是否是草稿状态
     */
    public boolean isDraft() {
        return this.status == ComponentStatus.DRAFT;
    }

    /**
     * 检查是否已发布
     */
    public boolean isPublished() {
        return this.status == ComponentStatus.PUBLISHED;
    }
}
