package com.dms.liteflow.infrastructure.persistence.repository;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.flowexec.repository.FlowChainRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.persistence.entity.FlowChainEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.FlowChainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 流程链仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FlowChainRepositoryImpl implements FlowChainRepository {

    private final FlowChainMapper flowChainMapper;

    @Override
    public FlowChain save(FlowChain chain) {
        FlowChainEntity entity = toEntity(chain);
        if (entity.getId() == null) {
            flowChainMapper.insert(entity);
        } else {
            flowChainMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<FlowChain> findById(ChainId chainId) {
        FlowChainEntity entity = flowChainMapper.selectById(chainId.getValue());
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<FlowChain> findByTenantId(TenantId tenantId) {
        List<FlowChainEntity> entities = flowChainMapper.selectByTenantId(tenantId.getValue());
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public List<FlowChain> findByTenantIdAndStatus(TenantId tenantId, ComponentStatus status) {
        List<FlowChainEntity> entities = flowChainMapper.selectByTenantIdAndStatus(
                tenantId.getValue(),
                status.getCode()
        );
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<FlowChain> findByTenantIdAndName(TenantId tenantId, String chainName) {
        FlowChainEntity entity = flowChainMapper.selectByTenantIdAndName(
                tenantId.getValue(),
                chainName
        );
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public boolean existsByTenantIdAndName(TenantId tenantId, String chainName) {
        return flowChainMapper.countByTenantIdAndName(tenantId.getValue(), chainName) > 0;
    }

    @Override
    public void deleteById(ChainId chainId) {
        flowChainMapper.softDeleteById(chainId.getValue());
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return flowChainMapper.countByTenantId(tenantId.getValue());
    }

    /**
     * 领域对象转实体
     */
    private FlowChainEntity toEntity(FlowChain domain) {
        return FlowChainEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId().getValue())
                .chainName(domain.getChainName())
                .chainCode(domain.getChainCode())
                .description(domain.getDescription())
                .configType(domain.getConfigType())
                .status(domain.getStatus().getCode())
                .currentVersion(domain.getCurrentVersion())
                .transactional(domain.getTransactional())
                .transactionTimeout(domain.getTransactionTimeout())
                .transactionPropagation(domain.getTransactionPropagation())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .deletedAt(domain.getDeletedAt())
                .build();
    }

    /**
     * 实体转领域对象
     */
    private FlowChain toDomain(FlowChainEntity entity) {
        return FlowChain.builder()
                .id(entity.getId())
                .tenantId(TenantId.of(entity.getTenantId()))
                .chainName(entity.getChainName())
                .chainCode(entity.getChainCode())
                .description(entity.getDescription())
                .configType(entity.getConfigType())
                .status(ComponentStatus.fromCode(entity.getStatus()))
                .currentVersion(entity.getCurrentVersion())
                .transactional(entity.getTransactional())
                .transactionTimeout(entity.getTransactionTimeout())
                .transactionPropagation(entity.getTransactionPropagation())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .deletedAt(entity.getDeletedAt())
                .build();
    }
}
