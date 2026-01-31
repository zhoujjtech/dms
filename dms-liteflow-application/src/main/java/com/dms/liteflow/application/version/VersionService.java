package com.dms.liteflow.application.version;

import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.domain.version.aggregate.ConfigVersion;
import com.dms.liteflow.domain.version.repository.ConfigVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 版本管理服务
 * <p>
 * 管理组件和流程链的版本
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VersionService {

    private final ConfigVersionRepository configVersionRepository;
    private final RuleComponentRepository ruleComponentRepository;
    private final DiffService diffService;

    private static final int MAX_VERSIONS = 50;

    /**
     * 保存配置版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型 (COMPONENT/CHAIN/SUB_CHAIN)
     * @param configId   配置ID
     * @param content    配置内容
     * @param createdBy  创建人
     * @return 保存的版本
     */
    @Transactional
    public ConfigVersion saveVersion(
            Long tenantId,
            String configType,
            Long configId,
            String content,
            String createdBy
    ) {
        log.info("Saving version for configType: {}, configId: {}", configType, configId);

        // 获取当前最新版本号
        int nextVersion = getNextVersionNumber(tenantId, configType, configId);

        // 检查版本数量限制
        long versionCount = configVersionRepository.countVersions(
                TenantId.of(tenantId),
                configType,
                configId
        );

        if (versionCount >= MAX_VERSIONS) {
            // 删除最旧的版本
            removeOldestVersion(tenantId, configType, configId);
        }

        // 创建新版本
        ConfigVersion version = ConfigVersion.builder()
                .tenantId(TenantId.of(tenantId))
                .configType(configType)
                .configId(configId)
                .version(nextVersion)
                .content(content)
                .status(ComponentStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        return configVersionRepository.save(version);
    }

    /**
     * 获取配置的所有版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @return 版本列表
     */
    public List<ConfigVersion> getVersions(Long tenantId, String configType, Long configId) {
        log.info("Getting versions for configType: {}, configId: {}", configType, configId);

        List<ConfigVersion> versions = configVersionRepository.findByTenantIdAndConfigTypeAndConfigId(
                TenantId.of(tenantId),
                configType,
                configId
        );

        // 按版本号降序排序
        versions.sort(Comparator.comparingInt(ConfigVersion::getVersion).reversed());

        return versions;
    }

    /**
     * 获取指定版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @param version    版本号
     * @return 版本 Optional
     */
    public Optional<ConfigVersion> getVersion(
            Long tenantId,
            String configType,
            Long configId,
            Integer version
    ) {
        log.info("Getting version {} for configType: {}, configId: {}", version, configType, configId);

        return configVersionRepository.findByTenantIdAndConfigTypeAndConfigIdAndVersion(
                TenantId.of(tenantId),
                configType,
                configId,
                version
        );
    }

    /**
     * 获取最新版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @return 最新版本 Optional
     */
    public Optional<ConfigVersion> getLatestVersion(Long tenantId, String configType, Long configId) {
        log.info("Getting latest version for configType: {}, configId: {}", configType, configId);

        return configVersionRepository.findLatestVersion(
                TenantId.of(tenantId),
                configType,
                configId
        );
    }

    /**
     * 发布版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @param version    版本号
     */
    @Transactional
    public void publishVersion(Long tenantId, String configType, Long configId, Integer version) {
        log.info("Publishing version {} for configType: {}, configId: {}", version, configType, configId);

        ConfigVersion configVersion = configVersionRepository.findByTenantIdAndConfigTypeAndConfigIdAndVersion(
                TenantId.of(tenantId),
                configType,
                configId,
                version
        ).orElseThrow(() -> new IllegalArgumentException("Version not found: " + version));

        configVersion.publish();
        configVersionRepository.save(configVersion);
    }

    /**
     * 更新版本状态
     *
     * @param versionId 版本ID
     * @param status    新状态
     */
    @Transactional
    public void updateVersionStatus(Long versionId, String status) {
        log.info("Updating version {} status to: {}", versionId, status);

        ConfigVersion configVersion = configVersionRepository.findById(versionId)
                .orElseThrow(() -> new IllegalArgumentException("Version not found: " + versionId));

        configVersion.setStatus(ComponentStatus.fromCode(status));
        configVersionRepository.save(configVersion);
    }

    /**
     * 归档版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @param version    版本号
     */
    @Transactional
    public void archiveVersion(Long tenantId, String configType, Long configId, Integer version) {
        log.info("Archiving version {} for configType: {}, configId: {}", version, configType, configId);

        ConfigVersion configVersion = configVersionRepository.findByTenantIdAndConfigTypeAndConfigIdAndVersion(
                TenantId.of(tenantId),
                configType,
                configId,
                version
        ).orElseThrow(() -> new IllegalArgumentException("Version not found: " + version));

        configVersion.archive();
        configVersionRepository.save(configVersion);
    }

    /**
     * 删除版本
     *
     * @param versionId 版本ID
     */
    @Transactional
    public void deleteVersion(Long versionId) {
        log.info("Deleting version: {}", versionId);
        configVersionRepository.deleteById(versionId);
    }

    /**
     * 对比两个版本（文本格式）
     *
     * @param versionId1 第一个版本ID
     * @param versionId2 第二个版本ID
     * @return 差异信息
     */
    public DiffService.DiffResult compareVersions(Long versionId1, Long versionId2) {
        log.info("Comparing versions: {} and {}", versionId1, versionId2);
        return diffService.compareVersionsText(versionId1, versionId2);
    }

    /**
     * 对比两个版本（HTML格式）
     *
     * @param versionId1 第一个版本ID
     * @param versionId2 第二个版本ID
     * @return HTML格式的差异信息
     */
    public String compareVersionsHtml(Long versionId1, Long versionId2) {
        log.info("Comparing versions with HTML: {} and {}", versionId1, versionId2);
        return diffService.compareVersionsHtml(versionId1, versionId2);
    }

    /**
     * 获取当前版本（最新版本）
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @return 当前版本信息
     */
    public Optional<ConfigVersion> getCurrentVersion(Long tenantId, String configType, Long configId) {
        log.info("Getting current version for configType: {}, configId: {}", configType, configId);

        return configVersionRepository.findLatestVersion(
                TenantId.of(tenantId),
                configType,
                configId
        );
    }

    /**
     * 获取当前版本号
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @return 当前版本号，如果没有版本返回0
     */
    public Integer getCurrentVersionNumber(Long tenantId, String configType, Long configId) {
        return getCurrentVersion(tenantId, configType, configId)
                .map(ConfigVersion::getVersion)
                .orElse(0);
    }

    /**
     * 回滚到指定版本
     *
     * @param tenantId   租户ID
     * @param configType 配置类型
     * @param configId   配置ID
     * @param version    要回滚到的版本号
     */
    @Transactional
    public void rollbackToVersion(Long tenantId, String configType, Long configId, Integer version) {
        log.info("Rolling back to version {} for configType: {}, configId: {}", version, configType, configId);

        ConfigVersion targetVersion = configVersionRepository.findByTenantIdAndConfigTypeAndConfigIdAndVersion(
                TenantId.of(tenantId),
                configType,
                configId,
                version
        ).orElseThrow(() -> new IllegalArgumentException("Version not found: " + version));

        // 创建新版本，内容使用目标版本的内容
        int nextVersion = getNextVersionNumber(tenantId, configType, configId);

        ConfigVersion newVersion = ConfigVersion.builder()
                .tenantId(TenantId.of(tenantId))
                .configType(configType)
                .configId(configId)
                .version(nextVersion)
                .content(targetVersion.getContent())
                .status(ComponentStatus.DRAFT)
                .createdBy("ROLLBACK")
                .build();

        configVersionRepository.save(newVersion);

        log.info("Rolled back to version {}, created new version: {}", version, nextVersion);
    }

    /**
     * 获取下一个版本号
     */
    private int getNextVersionNumber(Long tenantId, String configType, Long configId) {
        List<ConfigVersion> versions = configVersionRepository.findByTenantIdAndConfigTypeAndConfigId(
                TenantId.of(tenantId),
                configType,
                configId
        );

        return versions.stream()
                .mapToInt(ConfigVersion::getVersion)
                .max()
                .orElse(0) + 1;
    }

    /**
     * 删除最旧的版本
     */
    private void removeOldestVersion(Long tenantId, String configType, Long configId) {
        List<ConfigVersion> versions = configVersionRepository.findByTenantIdAndConfigTypeAndConfigId(
                TenantId.of(tenantId),
                configType,
                configId
        );

        if (!versions.isEmpty()) {
            // 找到最旧的已归档版本
            versions.stream()
                    .filter(v -> v.getStatus() == ComponentStatus.ARCHIVED)
                    .min(Comparator.comparingInt(ConfigVersion::getVersion))
                    .ifPresent(v -> {
                        configVersionRepository.deleteById(v.getId());
                        log.info("Removed oldest version: {}", v.getVersion());
                    });
        }
    }
}
