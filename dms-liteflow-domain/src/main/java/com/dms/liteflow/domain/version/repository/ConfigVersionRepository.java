package com.dms.liteflow.domain.version.repository;

import com.dms.liteflow.domain.version.aggregate.ConfigVersion;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * 配置版本仓储接口
 */
public interface ConfigVersionRepository {

    /**
     * 保存配置版本
     *
     * @param version 配置版本聚合根
     * @return 保存后的版本
     */
    ConfigVersion save(ConfigVersion version);

    /**
     * 根据ID查找
     *
     * @param id 版本ID
     * @return 版本 Optional
     */
    Optional<ConfigVersion> findById(Long id);

    /**
     * 根据租户、配置类型和配置ID查找所有版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @return 版本列表
     */
    List<ConfigVersion> findByTenantIdAndConfigTypeAndConfigId(
            TenantId tenantId,
            String configType,
            Long configId
    );

    /**
     * 根据租户、配置类型、配置ID和版本号查找
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @param version    版本号
     * @return 版本 Optional
     */
    Optional<ConfigVersion> findByTenantIdAndConfigTypeAndConfigIdAndVersion(
            TenantId tenantId,
            String configType,
            Long configId,
            Integer version
    );

    /**
     * 查找配置的最新版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @return 最新版本 Optional
     */
    Optional<ConfigVersion> findLatestVersion(
            TenantId tenantId,
            String configType,
            Long configId
    );

    /**
     * 统计配置的版本数量
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @return 版本数量
     */
    long countVersions(
            TenantId tenantId,
            String configType,
            Long configId
    );

    /**
     * 删除版本
     *
     * @param id 版本ID
     */
    void deleteById(Long id);
}
