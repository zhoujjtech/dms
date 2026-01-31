package com.dms.liteflow.domain.flowexec.repository;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * 流程链仓储接口
 * <p>
 * 领域层定义，基础设施层实现
 * </p>
 */
public interface FlowChainRepository {

    /**
     * 保存流程链
     *
     * @param chain 流程链聚合根
     * @return 保存后的流程链
     */
    FlowChain save(FlowChain chain);

    /**
     * 根据ID查找
     *
     * @param chainId 流程链ID
     * @return 流程链 Optional
     */
    Optional<FlowChain> findById(ChainId chainId);

    /**
     * 根据租户ID查找所有流程链
     *
     * @param tenantId 租户ID
     * @return 流程链列表
     */
    List<FlowChain> findByTenantId(TenantId tenantId);

    /**
     * 根据租户ID和状态查找流程链
     *
     * @param tenantId 租户ID
     * @param status   状态
     * @return 流程链列表
     */
    List<FlowChain> findByTenantIdAndStatus(TenantId tenantId, ComponentStatus status);

    /**
     * 根据租户ID和名称查找流程链
     *
     * @param tenantId  租户ID
     * @param chainName 流程链名称
     * @return 流程链 Optional
     */
    Optional<FlowChain> findByTenantIdAndName(TenantId tenantId, String chainName);

    /**
     * 检查流程链名称是否存在
     *
     * @param tenantId  租户ID
     * @param chainName 流程链名称
     * @return 是否存在
     */
    boolean existsByTenantIdAndName(TenantId tenantId, String chainName);

    /**
     * 删除流程链（软删除）
     *
     * @param chainId 流程链ID
     */
    void deleteById(ChainId chainId);

    /**
     * 统计租户下的流程链数量
     *
     * @param tenantId 租户ID
     * @return 流程链数量
     */
    long countByTenantId(TenantId tenantId);
}
