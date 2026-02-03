package com.dms.liteflow.infrastructure.saga.persistence.repository;

import com.dms.liteflow.domain.saga.repository.SagaComponentMetadataRepository;
import com.dms.liteflow.domain.saga.valueobject.SagaComponentMetadata;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.saga.persistence.entity.SagaComponentMetadataEntity;
import com.dms.liteflow.infrastructure.saga.persistence.mapper.SagaComponentMetadataMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Saga 组件元数据 Repository 实现
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SagaComponentMetadataRepositoryImpl implements SagaComponentMetadataRepository {

    private final SagaComponentMetadataMapper sagaComponentMetadataMapper;

    @Override
    public SagaComponentMetadata save(SagaComponentMetadata metadata) {
        SagaComponentMetadataEntity entity = toEntity(metadata);
        sagaComponentMetadataMapper.insert(entity);
        return metadata;
    }

    @Override
    public List<SagaComponentMetadata> saveAll(List<SagaComponentMetadata> metadataList) {
        List<SagaComponentMetadataEntity> entities = metadataList.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        sagaComponentMetadataMapper.insertBatch(entities);
        return metadataList;
    }

    @Override
    public Optional<SagaComponentMetadata> findByTenantIdAndComponentName(TenantId tenantId, String componentName) {
        SagaComponentMetadataEntity entity = sagaComponentMetadataMapper.selectByTenantIdAndComponentName(
                tenantId.getValue(), componentName);
        return Optional.ofNullable(entity).map(this::toValueObject);
    }

    @Override
    public List<SagaComponentMetadata> findByTenantId(TenantId tenantId) {
        List<SagaComponentMetadataEntity> entities = sagaComponentMetadataMapper.selectByTenantId(tenantId.getValue());
        return entities.stream().map(this::toValueObject).collect(Collectors.toList());
    }

    @Override
    public List<SagaComponentMetadata> findAll() {
        List<SagaComponentMetadataEntity> entities = sagaComponentMetadataMapper.selectAll();
        return entities.stream().map(this::toValueObject).collect(Collectors.toList());
    }

    @Override
    public List<SagaComponentMetadata> findByComponentName(String componentName) {
        List<SagaComponentMetadataEntity> entities = sagaComponentMetadataMapper.selectByComponentName(componentName);
        return entities.stream().map(this::toValueObject).collect(Collectors.toList());
    }

    @Override
    public void delete(SagaComponentMetadata metadata) {
        // TODO: 实现
    }

    @Override
    public void deleteByTenantId(TenantId tenantId) {
        sagaComponentMetadataMapper.deleteByTenantId(tenantId.getValue());
    }

    private SagaComponentMetadataEntity toEntity(SagaComponentMetadata metadata) {
        return SagaComponentMetadataEntity.builder()
                .tenantId(metadata.getComponentName() != null ? 0L : 0L) // 简化
                .componentName(metadata.getComponentName())
                .compensateComponent(metadata.getCompensateComponent())
                .needsCompensation(metadata.getNeedsCompensation())
                .defaultFailureStrategy(metadata.getDefaultFailureStrategy() != null ? metadata.getDefaultFailureStrategy().name() : null)
                .timeoutMs(metadata.getTimeoutMs())
                .metadata(null) // JSON 序列化
                .build();
    }

    private SagaComponentMetadata toValueObject(SagaComponentMetadataEntity entity) {
        return SagaComponentMetadata.builder()
                .componentName(entity.getComponentName())
                .compensateComponent(entity.getCompensateComponent())
                .needsCompensation(entity.getNeedsCompensation())
                .defaultFailureStrategy(entity.getDefaultFailureStrategy() != null ?
                        ActionType.valueOf(entity.getDefaultFailureStrategy()) : null)
                .timeoutMs(entity.getTimeoutMs())
                .metadata(new java.util.HashMap<>())
                .build();
    }
}
