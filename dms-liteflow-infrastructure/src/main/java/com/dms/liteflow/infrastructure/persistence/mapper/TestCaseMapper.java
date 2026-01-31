package com.dms.liteflow.infrastructure.persistence.mapper;

import com.dms.liteflow.infrastructure.persistence.entity.TestCaseEntity;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 测试用例 Mapper 接口
 */
public interface TestCaseMapper {

    /**
     * 插入测试用例
     */
    int insert(TestCaseEntity entity);

    /**
     * 根据ID更新
     */
    int updateById(TestCaseEntity entity);

    /**
     * 根据ID查询
     */
    TestCaseEntity selectById(Long id);

    /**
     * 根据租户ID查询所有测试用例
     */
    List<TestCaseEntity> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据租户、配置类型和配置ID查询测试用例
     */
    List<TestCaseEntity> selectByTenantIdAndConfigTypeAndConfigId(
            @Param("tenantId") Long tenantId,
            @Param("configType") String configType,
            @Param("configId") Long configId
    );

    /**
     * 根据ID删除
     */
    int deleteById(Long id);
}
