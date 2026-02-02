package com.dms.liteflow.api.config;

import com.dms.liteflow.infrastructure.liteflow.FlowConfigService;
import com.dms.liteflow.infrastructure.liteflow.service.MultiTenantFlowRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 配置管理控制器
 * <p>
 * 提供配置刷新、缓存清理等管理功能
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/config")
@RequiredArgsConstructor
@Tag(name = "Config Management", description = "配置管理 API")
public class ConfigManagementController {

    private final FlowConfigService flowConfigService;
    private final MultiTenantFlowRuleService multiTenantFlowRuleService;

    /**
     * 手动刷新所有配置缓存
     *
     * @return 刷新结果
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新所有配置", description = "清除所有配置缓存，重新从数据库加载规则")
    public ResponseEntity<Map<String, Object>> refreshAllConfigs() {
        log.info("Manual config refresh triggered");

        try {
            // 清除缓存
            flowConfigService.clearAllCache();

            // 重新加载规则到 FlowExecutor
            multiTenantFlowRuleService.refreshAllRules();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "All configs refreshed successfully");
            result.put("timestamp", System.currentTimeMillis());

            log.info("Manual config refresh completed successfully");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to refresh config", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to refresh config: " + e.getMessage());
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 刷新指定租户的配置
     *
     * @param tenantId 租户ID
     * @return 刷新结果
     */
    @PostMapping("/refresh/{tenantId}")
    @Operation(summary = "刷新租户配置", description = "重新加载指定租户的流程规则")
    public ResponseEntity<Map<String, Object>> refreshTenantConfig(
            @Parameter(description = "租户ID", required = true)
            @PathVariable Long tenantId) {

        log.info("Manual config refresh triggered for tenant: {}", tenantId);

        try {
            // 检查租户是否有已发布配置
            boolean hasPublishedConfig = flowConfigService.hasPublishedConfig(tenantId);

            if (!hasPublishedConfig) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", false);
                result.put("message", "No published configuration found for tenant: " + tenantId);
                result.put("timestamp", System.currentTimeMillis());

                return ResponseEntity.status(404).body(result);
            }

            // 清除缓存
            flowConfigService.refreshConfig(tenantId);

            // 重新加载规则
            multiTenantFlowRuleService.refreshTenant(tenantId);

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Config refreshed for tenant: " + tenantId);
            result.put("tenantId", tenantId);
            result.put("timestamp", System.currentTimeMillis());

            log.info("Manual config refresh completed for tenant: {}", tenantId);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Failed to refresh config for tenant: {}", tenantId, e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Failed to refresh config: " + e.getMessage());
            result.put("tenantId", tenantId);
            result.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 检查配置加载状态
     *
     * @param tenantId 租户ID（可选）
     * @return 配置状态
     */
    @GetMapping("/status")
    @Operation(summary = "检查配置状态", description = "检查配置加载状态和缓存信息")
    public ResponseEntity<Map<String, Object>> getConfigStatus(
            @Parameter(description = "租户ID（可选）")
            @RequestParam(required = false) Long tenantId) {

        Map<String, Object> status = new HashMap<>();

        if (tenantId != null) {
            // 检查指定租户的配置状态
            boolean hasPublishedConfig = flowConfigService.hasPublishedConfig(tenantId);
            status.put("tenantId", tenantId);
            status.put("hasPublishedConfig", hasPublishedConfig);
            status.put("configSource", "database");
            status.put("configType", "dynamic");

        } else {
            // 返回全局配置状态
            status.put("configSource", "database");
            status.put("configType", "dynamic");
            status.put("autoRefreshEnabled", true);
            status.put("autoRefreshInterval", "60 seconds");
        }

        status.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(status);
    }
}
