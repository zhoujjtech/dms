package com.dms.liteflow.infrastructure.persistence.mapper;

import com.dms.liteflow.infrastructure.persistence.entity.TenantEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 租户 Mapper
 */
public interface TenantMapper {

    /**
     * 插入租户
     */
    int insert(TenantEntity entity);

    /**
     * 根据ID更新
     */
    int updateById(TenantEntity entity);

    /**
     * 根据ID查询
     */
    TenantEntity selectById(@Param("id") Long id);

    /**
     * 根据租户编码查询
     */
    TenantEntity selectByTenantCode(@Param("tenantCode") String tenantCode);

    /**
     * 查询所有租户
     */
    List<TenantEntity> selectAll();

    /**
     * 检查租户编码是否存在
     */
    int countByTenantCode(@Param("tenantCode") String tenantCode);

    /**
     * 软删除
     */
    int softDeleteById(@Param("id") Long id);
}
