package com.dms.liteflow.domain.flowexec.repository;

import com.dms.liteflow.domain.flowexec.entity.FlowSubChain;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * 子流程仓储接口
 */
public interface FlowSubChainRepository {

    /**
     * 保存子流程
     *
     * @param subChain 子流程实体
     * @return 保存后的子流程
     */
    FlowSubChain save(FlowSubChain subChain);

    /**
     * 根据ID查找
     *
     * @param id 子流程ID
     * @return 子流程 Optional
     */
    Optional<FlowSubChain> findById(Long id);

    /**
     * 根据租户ID查找所有子流程
     *
     * @param tenantId 租户ID
     * @return 子流程列表
     */
    List<FlowSubChain> findByTenantId(TenantId tenantId);

    /**
     * 根据租户ID和名称查找
     *
     * @param tenantId      租户ID
     * @param subChainName  子流程名称
     * @return 子流程 Optional
     */
    Optional<FlowSubChain> findByTenantIdAndName(TenantId tenantId, String subChainName);

    /**
     * 根据父流程链ID查找子流程
     *
     * @param parentChainId 父流程链ID
     * @return 子流程列表
     */
    List<FlowSubChain> findByParentChainId(Long parentChainId);

    /**
     * 删除子流程
     *
     * @param id 子流程ID
     */
    void deleteById(Long id);
}
