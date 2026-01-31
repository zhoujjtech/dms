package com.dms.liteflow.domain.testing.repository;

import com.dms.liteflow.domain.testing.aggregate.TestCase;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

/**
 * 测试用例仓储接口
 */
public interface TestCaseRepository {

    /**
     * 保存测试用例
     *
     * @param testCase 测试用例聚合根
     * @return 保存后的测试用例
     */
    TestCase save(TestCase testCase);

    /**
     * 根据ID查找
     *
     * @param id 测试用例ID
     * @return 测试用例 Optional
     */
    Optional<TestCase> findById(Long id);

    /**
     * 根据租户ID查找所有测试用例
     *
     * @param tenantId 租户ID
     * @return 测试用例列表
     */
    List<TestCase> findByTenantId(TenantId tenantId);

    /**
     * 根据租户、配置类型和配置ID查找测试用例
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @return 测试用例列表
     */
    List<TestCase> findByTenantIdAndConfigTypeAndConfigId(
            TenantId tenantId,
            String configType,
            Long configId
    );

    /**
     * 删除测试用例
     *
     * @param id 测试用例ID
     */
    void deleteById(Long id);
}
