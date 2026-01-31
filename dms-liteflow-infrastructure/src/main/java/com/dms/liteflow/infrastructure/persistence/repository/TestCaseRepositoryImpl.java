package com.dms.liteflow.infrastructure.persistence.repository;

import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.domain.testing.aggregate.TestCase;
import com.dms.liteflow.domain.testing.repository.TestCaseRepository;
import com.dms.liteflow.infrastructure.persistence.entity.TestCaseEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.TestCaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 测试用例仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TestCaseRepositoryImpl implements TestCaseRepository {

    private final TestCaseMapper testCaseMapper;

    @Override
    public TestCase save(TestCase testCase) {
        TestCaseEntity entity = toEntity(testCase);
        if (entity.getId() == null) {
            testCaseMapper.insert(entity);
        } else {
            testCaseMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<TestCase> findById(Long id) {
        TestCaseEntity entity = testCaseMapper.selectById(id);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<TestCase> findByTenantId(TenantId tenantId) {
        List<TestCaseEntity> entities = testCaseMapper.selectByTenantId(tenantId.getValue());
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public List<TestCase> findByTenantIdAndConfigTypeAndConfigId(
            TenantId tenantId,
            String configType,
            Long configId
    ) {
        List<TestCaseEntity> entities = testCaseMapper.selectByTenantIdAndConfigTypeAndConfigId(
                tenantId.getValue(),
                configType,
                configId
        );
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(Long id) {
        testCaseMapper.deleteById(id);
    }

    /**
     * 领域对象转实体
     */
    private TestCaseEntity toEntity(TestCase domain) {
        return TestCaseEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId().getValue())
                .configType(domain.getConfigType())
                .configId(domain.getConfigId())
                .name(domain.getName())
                .inputData(domain.getInputData())
                .expectedResult(domain.getExpectedResult())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * 实体转领域对象
     */
    private TestCase toDomain(TestCaseEntity entity) {
        return TestCase.builder()
                .id(entity.getId())
                .tenantId(TenantId.of(entity.getTenantId()))
                .configType(entity.getConfigType())
                .configId(entity.getConfigId())
                .name(entity.getName())
                .inputData(entity.getInputData())
                .expectedResult(entity.getExpectedResult())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
