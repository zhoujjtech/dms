package com.dms.liteflow.infrastructure.persistence.mapper;

import com.dms.liteflow.infrastructure.persistence.entity.RuleComponentEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 规则组件 Mapper
 */
public interface RuleComponentMapper {

    /**
     * 插入规则组件
     */
    int insert(RuleComponentEntity entity);

    /**
     * 根据ID更新
     */
    int updateById(RuleComponentEntity entity);

    /**
     * 根据ID查询
     */
    RuleComponentEntity selectById(Long id);

    /**
     * 根据组件ID查询
     */
    RuleComponentEntity selectByComponentId(@Param("componentId") String componentId);

    /**
     * 根据租户ID查询所有
     */
    List<RuleComponentEntity> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据租户ID和状态查询
     */
    List<RuleComponentEntity> selectByTenantIdAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") String status
    );

    /**
     * 根据租户ID和组件类型查询
     */
    List<RuleComponentEntity> selectByTenantIdAndType(
            @Param("tenantId") Long tenantId,
            @Param("componentType") String componentType
    );

    /**
     * 检查组件ID是否存在
     */
    int countByComponentId(@Param("componentId") String componentId);

    /**
     * 根据ID删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 统计租户下的组件数量
     */
    long countByTenantId(@Param("tenantId") Long tenantId);
}
