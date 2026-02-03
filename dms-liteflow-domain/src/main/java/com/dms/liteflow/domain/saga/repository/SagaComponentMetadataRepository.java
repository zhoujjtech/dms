package com.dms.liteflow.domain.saga.repository;

import com.dms.liteflow.domain.saga.valueobject.SagaComponentMetadata;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * Saga 组件元数据 Repository 接口
 *
 * @author DMS
 * @since 2026-02-03
 */
public interface SagaComponentMetadataRepository {

    /**
     * 保存或更新组件元数据
     */
    SagaComponentMetadata save(SagaComponentMetadata metadata);

    /**
     * 批量保存
     */
    List<SagaComponentMetadata> saveAll(List<SagaComponentMetadata> metadataList);

    /**
     * 根据租户ID 和组件名称查找
     */
    Optional<SagaComponentMetadata> findByTenantIdAndComponentName(TenantId tenantId, String componentName);

    /**
     * 根据租户ID 查找所有组件元数据
     */
    List<SagaComponentMetadata> findByTenantId(TenantId tenantId);

    /**
     * 查找所有组件元数据
     */
    List<SagaComponentMetadata> findAll();

    /**
     * 根据组件名称查找（所有租户）
     */
    List<SagaComponentMetadata> findByComponentName(String componentName);

    /**
     * 删除组件元数据
     */
    void delete(SagaComponentMetadata metadata);

    /**
     * 根据租户ID 删除所有组件元数据
     */
    void deleteByTenantId(TenantId tenantId);
}
