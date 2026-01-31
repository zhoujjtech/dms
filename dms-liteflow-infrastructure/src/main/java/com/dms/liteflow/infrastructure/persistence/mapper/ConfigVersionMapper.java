package com.dms.liteflow.infrastructure.persistence.mapper;

import com.dms.liteflow.infrastructure.persistence.entity.ConfigVersionEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 配置版本 Mapper 接口
 */
public interface ConfigVersionMapper {

    /**
     * 插入配置版本
     */
    int insert(ConfigVersionEntity entity);

    /**
     * 根据ID更新
     */
    int updateById(ConfigVersionEntity entity);

    /**
     * 根据ID查询
     */
    ConfigVersionEntity selectById(Long id);

    /**
     * 根据租户、配置类型和配置ID查询所有版本
     */
    List<ConfigVersionEntity> selectByTenantIdAndConfigTypeAndConfigId(
            @Param("tenantId") Long tenantId,
            @Param("configType") String configType,
            @Param("configId") Long configId
    );

    /**
     * 根据租户、配置类型、配置ID和版本号查询
     */
    ConfigVersionEntity selectByTenantIdAndConfigTypeAndConfigIdAndVersion(
            @Param("tenantId") Long tenantId,
            @Param("configType") String configType,
            @Param("configId") Long configId,
            @Param("version") Integer version
    );

    /**
     * 查找配置的最新版本
     */
    ConfigVersionEntity selectLatestVersion(
            @Param("tenantId") Long tenantId,
            @Param("configType") String configType,
            @Param("configId") Long configId
    );

    /**
     * 统计配置的版本数量
     */
    long countVersions(
            @Param("tenantId") Long tenantId,
            @Param("configType") String configType,
            @Param("configId") Long configId
    );

    /**
     * 根据ID删除
     */
    int deleteById(Long id);
}
