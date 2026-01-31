package com.dms.liteflow.infrastructure.persistence.repository;

import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.domain.version.aggregate.ConfigVersion;
import com.dms.liteflow.domain.version.repository.ConfigVersionRepository;
import com.dms.liteflow.infrastructure.persistence.entity.ConfigVersionEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.ConfigVersionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 配置版本仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ConfigVersionRepositoryImpl implements ConfigVersionRepository {

    private final ConfigVersionMapper configVersionMapper;

    @Override
    public ConfigVersion save(ConfigVersion version) {
        ConfigVersionEntity entity = toEntity(version);
        if (entity.getId() == null) {
            configVersionMapper.insert(entity);
        } else {
            configVersionMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public Optional<ConfigVersion> findById(Long id) {
        ConfigVersionEntity entity = configVersionMapper.selectById(id);
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public List<ConfigVersion> findByTenantIdAndConfigTypeAndConfigId(
            TenantId tenantId,
            String configType,
            Long configId
    ) {
        List<ConfigVersionEntity> entities = configVersionMapper.selectByTenantIdAndConfigTypeAndConfigId(
                tenantId.getValue(),
                configType,
                configId
        );
        return entities.stream().map(this::toDomain).toList();
    }

    @Override
    public Optional<ConfigVersion> findByTenantIdAndConfigTypeAndConfigIdAndVersion(
            TenantId tenantId,
            String configType,
            Long configId,
            Integer version
    ) {
        ConfigVersionEntity entity = configVersionMapper.selectByTenantIdAndConfigTypeAndConfigIdAndVersion(
                tenantId.getValue(),
                configType,
                configId,
                version
        );
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public Optional<ConfigVersion> findLatestVersion(
            TenantId tenantId,
            String configType,
            Long configId
    ) {
        ConfigVersionEntity entity = configVersionMapper.selectLatestVersion(
                tenantId.getValue(),
                configType,
                configId
        );
        return Optional.ofNullable(entity).map(this::toDomain);
    }

    @Override
    public long countVersions(
            TenantId tenantId,
            String configType,
            Long configId
    ) {
        return configVersionMapper.countVersions(
                tenantId.getValue(),
                configType,
                configId
        );
    }

    @Override
    public void deleteById(Long id) {
        configVersionMapper.deleteById(id);
    }

    /**
     * 领域对象转实体
     */
    private ConfigVersionEntity toEntity(ConfigVersion domain) {
        return ConfigVersionEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId().getValue())
                .configType(domain.getConfigType())
                .configId(domain.getConfigId())
                .version(domain.getVersion())
                .content(domain.getContent())
                .status(domain.getStatus().getCode())
                .createdBy(domain.getCreatedBy())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * 实体转领域对象
     */
    private ConfigVersion toDomain(ConfigVersionEntity entity) {
        return ConfigVersion.builder()
                .id(entity.getId())
                .tenantId(TenantId.of(entity.getTenantId()))
                .configType(entity.getConfigType())
                .configId(entity.getConfigId())
                .version(entity.getVersion())
                .content(entity.getContent())
                .status(ComponentStatus.fromCode(entity.getStatus()))
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
