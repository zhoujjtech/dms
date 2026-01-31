package com.dms.liteflow.api.version;

import com.dms.liteflow.application.version.DiffService;
import com.dms.liteflow.application.version.VersionService;
import com.dms.liteflow.domain.version.aggregate.ConfigVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 版本管理控制器
 * <p>
 * API 层：处理版本管理请求
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/versions")
@RequiredArgsConstructor
public class VersionController {

    private final VersionService versionService;
    private final DiffService diffService;

    /**
     * 获取配置的所有版本
     * GET /api/versions?tenantId={tenantId}&configType={configType}&configId={configId}
     */
    @GetMapping
    public ResponseEntity<List<ConfigVersion>> getVersions(
            @RequestParam Long tenantId,
            @RequestParam String configType,
            @RequestParam Long configId
    ) {
        log.info("GET /api/versions - tenantId: {}, configType: {}, configId: {}", tenantId, configType, configId);

        List<ConfigVersion> versions = versionService.getVersions(tenantId, configType, configId);

        return ResponseEntity.ok(versions);
    }

    /**
     * 获取指定版本
     * GET /api/versions/{configType}/{configId}/versions/{version}
     */
    @GetMapping("/{configType}/{configId}/versions/{version}")
    public ResponseEntity<ConfigVersion> getVersion(
            @PathVariable String configType,
            @PathVariable Long configId,
            @PathVariable Integer version,
            @RequestParam Long tenantId
    ) {
        log.info("GET /api/vversions/{}/{}/versions/{}", configType, configId, version);

        return versionService.getVersion(tenantId, configType, configId, version)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 发布版本
     * POST /api/versions/{configType}/{configId}/versions/{version}/publish
     */
    @PostMapping("/{configType}/{configId}/versions/{version}/publish")
    public ResponseEntity<Void> publishVersion(
            @PathVariable String configType,
            @PathVariable Long configId,
            @PathVariable Integer version,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/versions/{}/{}/versions/{}/publish", configType, configId, version);

        versionService.publishVersion(tenantId, configType, configId, version);

        return ResponseEntity.ok().build();
    }

    /**
     * 更新版本状态
     * PUT /api/versions/{versionId}/status
     */
    @PutMapping("/{versionId}/status")
    public ResponseEntity<Void> updateVersionStatus(
            @PathVariable Long versionId,
            @RequestParam String status
    ) {
        log.info("PUT /api/versions/{}/status - status: {}", versionId, status);

        versionService.updateVersionStatus(versionId, status);

        return ResponseEntity.ok().build();
    }

    /**
     * 归档版本
     * POST /api/versions/{configType}/{configId}/versions/{version}/archive
     */
    @PostMapping("/{configType}/{configId}/versions/{version}/archive")
    public ResponseEntity<Void> archiveVersion(
            @PathVariable String configType,
            @PathVariable Long configId,
            @PathVariable Integer version,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/versions/{}/{}/versions/{}/archive", configType, configId, version);

        versionService.archiveVersion(tenantId, configType, configId, version);

        return ResponseEntity.ok().build();
    }

    /**
     * 删除版本
     * DELETE /api/versions/{versionId}
     */
    @DeleteMapping("/{versionId}")
    public ResponseEntity<Void> deleteVersion(@PathVariable Long versionId) {
        log.info("DELETE /api/versions/{}", versionId);

        versionService.deleteVersion(versionId);

        return ResponseEntity.ok().build();
    }

    /**
     * 对比两个版本（文本格式）
     * GET /api/versions/compare?versionId1={versionId1}&versionId2={versionId2}
     */
    @GetMapping("/compare")
    public ResponseEntity<DiffService.DiffResult> compareVersions(
            @RequestParam Long versionId1,
            @RequestParam Long versionId2
    ) {
        log.info("GET /api/versions/compare - versionId1: {}, versionId2: {}", versionId1, versionId2);

        DiffService.DiffResult diff = versionService.compareVersions(versionId1, versionId2);

        return ResponseEntity.ok(diff);
    }

    /**
     * 对比两个版本（HTML格式）
     * GET /api/versions/compare/html?versionId1={versionId1}&versionId2={versionId2}
     */
    @GetMapping(value = "/compare/html", produces = "text/html;charset=UTF-8")
    public ResponseEntity<String> compareVersionsHtml(
            @RequestParam Long versionId1,
            @RequestParam Long versionId2
    ) {
        log.info("GET /api/versions/compare/html - versionId1: {}, versionId2: {}", versionId1, versionId2);

        String html = versionService.compareVersionsHtml(versionId1, versionId2);

        return ResponseEntity.ok(html);
    }

    /**
     * 获取当前版本
     * GET /api/versions/current?tenantId={tenantId}&configType={configType}&configId={configId}
     */
    @GetMapping("/current")
    public ResponseEntity<ConfigVersion> getCurrentVersion(
            @RequestParam Long tenantId,
            @RequestParam String configType,
            @RequestParam Long configId
    ) {
        log.info("GET /api/versions/current - tenantId: {}, configType: {}, configId: {}",
                tenantId, configType, configId);

        return versionService.getCurrentVersion(tenantId, configType, configId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取当前版本号
     * GET /api/versions/current/number?tenantId={tenantId}&configType={configType}&configId={configId}
     */
    @GetMapping("/current/number")
    public ResponseEntity<Map<String, Object>> getCurrentVersionNumber(
            @RequestParam Long tenantId,
            @RequestParam String configType,
            @RequestParam Long configId
    ) {
        log.info("GET /api/versions/current/number - tenantId: {}, configType: {}, configId: {}",
                tenantId, configType, configId);

        Integer versionNumber = versionService.getCurrentVersionNumber(tenantId, configType, configId);

        Map<String, Object> response = Map.of(
                "tenantId", tenantId,
                "configType", configType,
                "configId", configId,
                "currentVersion", versionNumber
        );

        return ResponseEntity.ok(response);
    }

    /**
     * 回滚到指定版本
     * POST /api/versions/rollback?tenantId={tenantId}&configType={configType}&configId={configId}&version={version}
     */
    @PostMapping("/rollback")
    public ResponseEntity<Void> rollbackToVersion(
            @RequestParam Long tenantId,
            @RequestParam String configType,
            @RequestParam Long configId,
            @RequestParam Integer version
    ) {
        log.info("POST /api/versions/rollback - tenantId: {}, configType: {}, configId: {}, version: {}",
                tenantId, configType, configId, version);

        versionService.rollbackToVersion(tenantId, configType, configId, version);

        return ResponseEntity.ok().build();
    }
}
