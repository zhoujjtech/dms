package com.dms.liteflow.infrastructure.persistence.repository;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.flowexec.repository.FlowChainRepository;
import com.dms.liteflow.domain.monitoring.aggregate.ExecutionRecord;
import com.dms.liteflow.domain.monitoring.repository.ExecutionRecordRepository;
import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentType;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.persistence.entity.RuleComponentEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.RuleComponentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 规则组件仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RuleComponentRepositoryImpl implements RuleComponentRepository {

    private final RuleComponentMapper ruleComponentMapper;

    @Override
    public RuleComponent save(RuleComponent component) {
        RuleComponentEntity entity = toEntity(component);
        if (entity.getId() == null) {
            ruleComponentMapper.insert(entity);
        } else {
            ruleComponentMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<RuleComponent> findByComponentId(ComponentId componentId) {
        RuleComponentEntity entity = ruleComponentMapper.selectByComponentId(componentId.getValue());
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<RuleComponent> findByTenantId(TenantId tenantId) {
        List<RuleComponentEntity> entities = ruleComponentMapper.selectByTenantId(tenantId.getValue());
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public List<RuleComponent> findByTenantIdAndStatus(TenantId tenantId, ComponentStatus status) {
        List<RuleComponentEntity> entities = ruleComponentMapper.selectByTenantIdAndStatus(
                tenantId.getValue(),
                status.getCode()
        );
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public List<RuleComponent> findByTenantIdAndType(TenantId tenantId, String componentType) {
        List<RuleComponentEntity> entities = ruleComponentMapper.selectByTenantIdAndType(
                tenantId.getValue(),
                componentType
        );
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public boolean existsByComponentId(ComponentId componentId) {
        return ruleComponentMapper.countByComponentId(componentId.getValue()) > 0;
    }

    @Override
    public void deleteByComponentId(ComponentId componentId) {
        // TODO: 实现删除逻辑
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return ruleComponentMapper.countByTenantId(tenantId.getValue());
    }

    /**
     * 领域对象转实体
     */
    private RuleComponentEntity toEntity(RuleComponent domain) {
        return RuleComponentEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId().getValue())
                .componentId(domain.getComponentId().getValue())
                .componentName(domain.getComponentName())
                .description(domain.getDescription())
                .componentType(domain.getComponentType().getCode())
                .content(domain.getContent())
                .status(domain.getStatus().getCode())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    /**
     * 实体转领域对象
     */
    private RuleComponent toDomain(RuleComponentEntity entity) {
        return RuleComponent.builder()
                .id(entity.getId())
                .tenantId(TenantId.of(entity.getTenantId()))
                .componentId(ComponentId.of(entity.getComponentId()))
                .componentName(entity.getComponentName())
                .description(entity.getDescription())
                .componentType(ComponentType.fromCode(entity.getComponentType()))
                .content(entity.getContent())
                .status(ComponentStatus.fromCode(entity.getStatus()))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
