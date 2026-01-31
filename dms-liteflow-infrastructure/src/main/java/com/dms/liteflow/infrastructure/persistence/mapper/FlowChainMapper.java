package com.dms.liteflow.infrastructure.persistence.mapper;

import com.dms.liteflow.infrastructure.persistence.entity.FlowChainEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 流程链 Mapper
 */
public interface FlowChainMapper {

    /**
     * 插入流程链
     */
    int insert(FlowChainEntity entity);

    /**
     * 根据ID更新
     */
    int updateById(FlowChainEntity entity);

    /**
     * 根据ID查询
     */
    FlowChainEntity selectById(@Param("id") Long id);

    /**
     * 根据租户ID查询所有
     */
    List<FlowChainEntity> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据租户ID和状态查询
     */
    List<FlowChainEntity> selectByTenantIdAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") String status
    );

    /**
     * 根据租户ID和名称查询
     */
    FlowChainEntity selectByTenantIdAndName(
            @Param("tenantId") Long tenantId,
            @Param("chainName") String chainName
    );

    /**
     * 检查名称是否存在
     */
    int countByTenantIdAndName(
            @Param("tenantId") Long tenantId,
            @Param("chainName") String chainName
    );

    /**
     * 软删除（更新 deleted_at）
     */
    int softDeleteById(@Param("id") Long id);

    /**
     * 统计租户下的流程链数量
     */
    long countByTenantId(@Param("tenantId") Long tenantId);
}
