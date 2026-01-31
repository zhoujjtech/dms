package com.dms.liteflow.domain.ruleconfig.repository;

import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * 规则组件仓储接口
 * <p>
 * 领域层定义，基础设施层实现
 * </p>
 */
public interface RuleComponentRepository {

    /**
     * 保存规则组件
     *
     * @param component 规则组件聚合根
     * @return 保存后的组件
     */
    RuleComponent save(RuleComponent component);

    /**
     * 根据组件ID查找
     *
     * @param componentId 组件ID
     * @return 组件 Optional
     */
    Optional<RuleComponent> findByComponentId(ComponentId componentId);

    /**
     * 根据租户ID查找所有组件
     *
     * @param tenantId 租户ID
     * @return 组件列表
     */
    List<RuleComponent> findByTenantId(TenantId tenantId);

    /**
     * 根据租户ID和状态查找组件
     *
     * @param tenantId 租户ID
     * @param status   状态
     * @return 组件列表
     */
    List<RuleComponent> findByTenantIdAndStatus(TenantId tenantId, ComponentStatus status);

    /**
     * 根据租户ID和组件类型查找组件
     *
     * @param tenantId      租户ID
     * @param componentType 组件类型
     * @return 组件列表
     */
    List<RuleComponent> findByTenantIdAndType(TenantId tenantId, String componentType);

    /**
     * 检查组件ID是否存在
     *
     * @param componentId 组件ID
     * @return 是否存在
     */
    boolean existsByComponentId(ComponentId componentId);

    /**
     * 删除组件
     *
     * @param componentId 组件ID
     */
    void deleteByComponentId(ComponentId componentId);

    /**
     * 统计租户下的组件数量
     *
     * @param tenantId 租户ID
     * @return 组件数量
     */
    long countByTenantId(TenantId tenantId);
}
