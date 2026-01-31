package com.dms.liteflow.domain.tenant.repository;

import com.dms.liteflow.domain.tenant.aggregate.Tenant;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * 租户仓储接口
 */
public interface TenantRepository {

    /**
     * 保存租户
     *
     * @param tenant 租户聚合根
     * @return 保存后的租户
     */
    Tenant save(Tenant tenant);

    /**
     * 根据ID查找租户
     *
     * @param tenantId 租户ID
     * @return 租户 Optional
     */
    Optional<Tenant> findById(TenantId tenantId);

    /**
     * 根据租户编码查找
     *
     * @param tenantCode 租户编码
     * @return 租户 Optional
     */
    Optional<Tenant> findByTenantCode(String tenantCode);

    /**
     * 查找所有租户
     *
     * @return 租户列表
     */
    List<Tenant> findAll();

    /**
     * 检查租户编码是否存在
     *
     * @param tenantCode 租户编码
     * @return 是否存在
     */
    boolean existsByTenantCode(String tenantCode);

    /**
     * 删除租户（软删除）
     *
     * @param tenantId 租户ID
     */
    void deleteById(TenantId tenantId);
}
