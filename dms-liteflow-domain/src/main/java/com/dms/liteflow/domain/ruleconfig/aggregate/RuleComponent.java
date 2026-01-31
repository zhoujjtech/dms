package com.dms.liteflow.domain.ruleconfig.aggregate;

import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentType;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 规则组件聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleComponent {

    /**
     * 主键ID（数据库生成）
     */
    private Long id;

    /**
     * 租户ID
     */
    private TenantId tenantId;

    /**
     * 组件ID
     */
    private ComponentId componentId;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 组件描述
     */
    private String description;

    /**
     * 组件类型
     */
    private ComponentType componentType;

    /**
     * 组件内容
     */
    private String content;

    /**
     * 状态
     */
    private ComponentStatus status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 发布组件
     */
    public void publish() {
        if (this.status == ComponentStatus.PUBLISHED) {
            throw new IllegalStateException("Component is already published");
        }
        this.status = ComponentStatus.PUBLISHED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 归档组件
     */
    public void archive() {
        if (this.status == ComponentStatus.ARCHIVED) {
            throw new IllegalStateException("Component is already archived");
        }
        this.status = ComponentStatus.ARCHIVED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新组件内容
     */
    public void updateContent(String newContent) {
        if (this.status == ComponentStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot update published component. Create a new version instead.");
        }
        this.content = newContent;
        this.updatedAt = LocalDateTime.now();
    }
}
