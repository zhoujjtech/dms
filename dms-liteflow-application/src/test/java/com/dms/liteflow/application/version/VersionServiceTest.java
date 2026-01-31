package com.dms.liteflow.application.version;

import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.domain.version.aggregate.ConfigVersion;
import com.dms.liteflow.domain.version.repository.ConfigVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VersionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private ConfigVersionRepository configVersionRepository;

    @Mock
    private RuleComponentRepository ruleComponentRepository;

    @InjectMocks
    private VersionService versionService;

    private TenantId tenantId;
    private String configType = "COMPONENT";
    private Long configId = 1L;

    @BeforeEach
    void setUp() {
        tenantId = TenantId.of(1L);
    }

    @Test
    void testSaveVersion() {
        // Given
        String content = "public class TestComponent extends NodeComponent { }";
        String createdBy = "test-user";

        when(configVersionRepository.countVersions(any(), anyString(), anyLong()))
                .thenReturn(0L);
        when(configVersionRepository.save(any(ConfigVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ConfigVersion result = versionService.saveVersion(
                tenantId.getValue(), configType, configId, content, createdBy
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(1);
        assertThat(result.getContent()).isEqualTo(content);
        assertThat(result.getStatus()).isEqualTo(ComponentStatus.DRAFT);

        verify(configVersionRepository).save(any(ConfigVersion.class));
    }

    @Test
    void testSaveVersion_MaxVersionsReached() {
        // Given
        String content = "public class TestComponent extends NodeComponent { }";
        String createdBy = "test-user";

        when(configVersionRepository.countVersions(any(), anyString(), anyLong()))
                .thenReturn(50L); // 已达到最大版本数
        when(configVersionRepository.findByTenantIdAndConfigTypeAndConfigId(any(), anyString(), anyLong()))
                .thenReturn(Arrays.asList(
                        createMockConfigVersion(1, ComponentStatus.ARCHIVED),
                        createMockConfigVersion(2, ComponentStatus.ARCHIVED)
                ));
        when(configVersionRepository.save(any(ConfigVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        ConfigVersion result = versionService.saveVersion(
                tenantId.getValue(), configType, configId, content, createdBy
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getVersion()).isEqualTo(1);
    }

    @Test
    void testGetVersions() {
        // Given
        ConfigVersion version1 = createMockConfigVersion(1, ComponentStatus.DRAFT);
        ConfigVersion version2 = createMockConfigVersion(2, ComponentStatus.PUBLISHED);

        when(configVersionRepository.findByTenantIdAndConfigTypeAndConfigId(any(), anyString(), anyLong()))
                .thenReturn(Arrays.asList(version1, version2));

        // When
        List<ConfigVersion> versions = versionService.getVersions(
                tenantId.getValue(), configType, configId
        );

        // Then
        assertThat(versions).hasSize(2);
    }

    @Test
    void testGetVersion() {
        // Given
        ConfigVersion version = createMockConfigVersion(1, ComponentStatus.PUBLISHED);

        when(configVersionRepository.findByTenantIdAndConfigTypeAndConfigIdAndVersion(
                any(), anyString(), anyLong(), anyInt()
        )).thenReturn(Optional.of(version));

        // When
        Optional<ConfigVersion> result = versionService.getVersion(
                tenantId.getValue(), configType, configId, 1
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(1);
    }

    @Test
    void testPublishVersion() {
        // Given
        ConfigVersion version = createMockConfigVersion(1, ComponentStatus.DRAFT);

        when(configVersionRepository.findByTenantIdAndConfigTypeAndConfigIdAndVersion(
                any(), anyString(), anyLong(), anyInt()
        )).thenReturn(Optional.of(version));
        when(configVersionRepository.save(any(ConfigVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        versionService.publishVersion(tenantId.getValue(), configType, configId, 1);

        // Then
        verify(configVersionRepository).save(version);
    }

    @Test
    void testArchiveVersion() {
        // Given
        ConfigVersion version = createMockConfigVersion(1, ComponentStatus.PUBLISHED);

        when(configVersionRepository.findByTenantIdAndConfigTypeAndConfigIdAndVersion(
                any(), anyString(), anyLong(), anyInt()
        )).thenReturn(Optional.of(version));
        when(configVersionRepository.save(any(ConfigVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        versionService.archiveVersion(tenantId.getValue(), configType, configId, 1);

        // Then
        verify(configVersionRepository).save(version);
    }

    @Test
    void testDeleteVersion() {
        // When
        versionService.deleteVersion(1L);

        // Then
        verify(configVersionRepository).deleteById(1L);
    }

    @Test
    void testRollbackToVersion() {
        // Given
        ConfigVersion targetVersion = createMockConfigVersion(2, ComponentStatus.PUBLISHED);
        targetVersion.setContent("old content");

        when(configVersionRepository.findByTenantIdAndConfigTypeAndConfigIdAndVersion(
                any(), anyString(), anyLong(), anyInt()
        )).thenReturn(Optional.of(targetVersion));
        when(configVersionRepository.findByTenantIdAndConfigTypeAndConfigId(any(), anyString(), anyLong()))
                .thenReturn(Arrays.asList(targetVersion));
        when(configVersionRepository.save(any(ConfigVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        versionService.rollbackToVersion(tenantId.getValue(), configType, configId, 2);

        // Then
        verify(configVersionRepository).save(any(ConfigVersion.class));
    }

    @Test
    void testGetCurrentVersion() {
        // Given
        ConfigVersion latestVersion = createMockConfigVersion(3, ComponentStatus.PUBLISHED);

        when(configVersionRepository.findLatestVersion(any(), anyString(), anyLong()))
                .thenReturn(Optional.of(latestVersion));

        // When
        Optional<ConfigVersion> result = versionService.getCurrentVersion(
                tenantId.getValue(), configType, configId
        );

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getVersion()).isEqualTo(3);
    }

    @Test
    void testGetCurrentVersionNumber() {
        // Given
        ConfigVersion latestVersion = createMockConfigVersion(3, ComponentStatus.PUBLISHED);

        when(configVersionRepository.findLatestVersion(any(), anyString(), anyLong()))
                .thenReturn(Optional.of(latestVersion));

        // When
        Integer versionNumber = versionService.getCurrentVersionNumber(
                tenantId.getValue(), configType, configId
        );

        // Then
        assertThat(versionNumber).isEqualTo(3);
    }

    @Test
    void testGetCurrentVersionNumber_NotFound() {
        // Given
        when(configVersionRepository.findLatestVersion(any(), anyString(), anyLong()))
                .thenReturn(Optional.empty());

        // When
        Integer versionNumber = versionService.getCurrentVersionNumber(
                tenantId.getValue(), configType, configId
        );

        // Then
        assertThat(versionNumber).isEqualTo(0);
    }

    @Test
    void testUpdateVersionStatus() {
        // Given
        ConfigVersion version = createMockConfigVersion(1, ComponentStatus.DRAFT);

        when(configVersionRepository.findById(1L))
                .thenReturn(Optional.of(version));
        when(configVersionRepository.save(any(ConfigVersion.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        versionService.updateVersionStatus(1L, "PUBLISHED");

        // Then
        verify(configVersionRepository).save(version);
    }

    /**
     * 创建模拟的 ConfigVersion
     */
    private ConfigVersion createMockConfigVersion(Integer version, ComponentStatus status) {
        return ConfigVersion.builder()
                .id(1L)
                .tenantId(tenantId)
                .configType(configType)
                .configId(configId)
                .version(version)
                .content("content")
                .status(status)
                .createdBy("test-user")
                .createdAt(LocalDateTime.now())
                .build();
    }
}
