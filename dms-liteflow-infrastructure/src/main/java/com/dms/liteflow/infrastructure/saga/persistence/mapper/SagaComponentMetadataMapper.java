package com.dms.liteflow.infrastructure.saga.persistence.mapper;

import com.dms.liteflow.infrastructure.saga.persistence.entity.SagaComponentMetadataEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Saga 组件元数据 Mapper
 *
 * @author DMS
 * @since 2026-02-03
 */
@Mapper
public interface SagaComponentMetadataMapper {

    /**
     * 插入
     */
    int insert(SagaComponentMetadataEntity entity);

    /**
     * 批量插入
     */
    int insertBatch(@Param("list") List<SagaComponentMetadataEntity> list);

    /**
     * 根据 ID 查询
     */
    SagaComponentMetadataEntity selectById(@Param("id") Long id);

    /**
     * 根据租户ID 和组件名称查找
     */
    SagaComponentMetadataEntity selectByTenantIdAndComponentName(
            @Param("tenantId") Long tenantId,
            @Param("componentName") String componentName
    );

    /**
     * 根据租户ID 查找所有组件元数据
     */
    List<SagaComponentMetadataEntity> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 查找所有组件元数据
     */
    List<SagaComponentMetadataEntity> selectAll();

    /**
     * 根据组件名称查找（所有租户）
     */
    List<SagaComponentMetadataEntity> selectByComponentName(@Param("componentName") String componentName);

    /**
     * 更新
     */
    int update(SagaComponentMetadataEntity entity);

    /**
     * 根据租户ID 和组件名称更新
     */
    int updateByTenantIdAndComponentName(SagaComponentMetadataEntity entity);

    /**
     * 根据 ID 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据租户ID 和组件名称删除
     */
    int deleteByTenantIdAndComponentName(
            @Param("tenantId") Long tenantId,
            @Param("componentName") String componentName
    );

    /**
     * 根据租户ID 删除所有组件元数据
     */
    int deleteByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 检查组件元数据是否存在
     */
    boolean existsByTenantIdAndComponentName(
            @Param("tenantId") Long tenantId,
            @Param("componentName") String componentName
    );
}
