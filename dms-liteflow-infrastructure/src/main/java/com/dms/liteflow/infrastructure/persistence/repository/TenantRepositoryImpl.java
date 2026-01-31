package com.dms.liteflow.infrastructure.persistence.repository;

import com.dms.liteflow.domain.tenant.aggregate.Tenant;
import com.dms.liteflow.domain.tenant.repository.TenantRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.persistence.entity.TenantEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.TenantMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 租户仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class TenantRepositoryImpl implements TenantRepository {

    private final TenantMapper tenantMapper;

    @Override
    public Tenant save(Tenant tenant) {
        TenantEntity entity = toEntity(tenant);
        if (entity.getId() == null) {
            tenantMapper.insert(entity);
        } else {
            tenantMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<Tenant> findById(TenantId tenantId) {
        TenantEntity entity = tenantMapper.selectById(tenantId.getValue());
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<Tenant> findByTenantCode(String tenantCode) {
        TenantEntity entity = tenantMapper.selectByTenantCode(tenantCode);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<Tenant> findAll() {
        List<TenantEntity> entities = tenantMapper.selectAll();
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByTenantCode(String tenantCode) {
        return tenantMapper.countByTenantCode(tenantCode) > 0;
    }

    @Override
    public void deleteById(TenantId tenantId) {
        tenantMapper.softDeleteById(tenantId.getValue());
    }

    /**
     * 领域对象转实体
     */
    private TenantEntity toEntity(Tenant domain) {
        return TenantEntity.builder()
                .id(domain.getId())
                .tenantCode(domain.getTenantCode())
                .tenantName(domain.getTenantName())
                .status(domain.getStatus())
                .maxChains(domain.getMaxChains())
                .maxComponents(domain.getMaxComponents())
                .executorCached(domain.getExecutorCached())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    /**
     * 实体转领域对象
     */
    private Tenant toDomain(TenantEntity entity) {
        return Tenant.builder()
                .id(entity.getId())
                .tenantCode(entity.getTenantCode())
                .tenantName(entity.getTenantName())
                .status(entity.getStatus())
                .maxChains(entity.getMaxChains())
                .maxComponents(entity.getMaxComponents())
                .executorCached(entity.getExecutorCached())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}
