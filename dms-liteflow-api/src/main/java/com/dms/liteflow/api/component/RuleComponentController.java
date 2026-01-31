package com.dms.liteflow.api.component;

import com.dms.liteflow.application.component.RuleComponentApplicationService;
import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 规则组件控制器
 * <p>
 * API 层：处理 HTTP 请求
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
public class RuleComponentController {

    private final RuleComponentApplicationService ruleComponentApplicationService;

    /**
     * 创建规则组件
     * POST /api/components
     */
    @PostMapping
    public ResponseEntity<RuleComponent> createComponent(
            @RequestParam Long tenantId,
            @RequestParam String componentId,
            @RequestParam String componentName,
            @RequestParam(required = false) String description,
            @RequestParam String componentType,
            @RequestParam String content
    ) {
        log.info("POST /api/components - tenantId: {}, componentId: {}", tenantId, componentId);

        RuleComponent component = ruleComponentApplicationService.createComponent(
                tenantId, componentId, componentName, description, componentType, content
        );

        return ResponseEntity.ok(component);
    }

    /**
     * 查询租户的所有组件
     * GET /api/components?tenantId={tenantId}&status={status}
     */
    @GetMapping
    public ResponseEntity<List<RuleComponent>> getComponents(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String status
    ) {
        log.info("GET /api/components - tenantId: {}, status: {}", tenantId, status);

        List<RuleComponent> components;
        if ("PUBLISHED".equals(status)) {
            components = ruleComponentApplicationService.getPublishedComponents(tenantId);
        } else {
            components = ruleComponentApplicationService.getComponentsByTenant(tenantId);
        }

        return ResponseEntity.ok(components);
    }

    /**
     * 查询组件详情
     * GET /api/components/{componentId}
     */
    @GetMapping("/{componentId}")
    public ResponseEntity<RuleComponent> getComponent(
            @PathVariable String componentId
    ) {
        log.info("GET /api/components/{}", componentId);

        RuleComponent component = ruleComponentApplicationService.getComponent(componentId);

        return ResponseEntity.ok(component);
    }

    /**
     * 更新组件
     * PUT /api/components/{componentId}
     */
    @PutMapping("/{componentId}")
    public ResponseEntity<RuleComponent> updateComponent(
            @PathVariable String componentId,
            @RequestParam String componentName,
            @RequestParam(required = false) String description,
            @RequestParam String content
    ) {
        log.info("PUT /api/components/{} - componentName: {}", componentId, componentName);

        RuleComponent component = ruleComponentApplicationService.updateComponent(
                componentId, componentName, description, content
        );

        return ResponseEntity.ok(component);
    }

    /**
     * 发布组件
     * POST /api/components/{componentId}/publish
     */
    @PostMapping("/{componentId}/publish")
    public ResponseEntity<Void> publishComponent(
            @PathVariable String componentId,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/components/{}/publish - tenantId: {}", componentId, tenantId);

        ruleComponentApplicationService.publishComponent(tenantId, componentId);

        return ResponseEntity.ok().build();
    }

    /**
     * 启用组件
     * POST /api/components/{componentId}/enable
     */
    @PostMapping("/{componentId}/enable")
    public ResponseEntity<Void> enableComponent(
            @PathVariable String componentId,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/components/{}/enable - tenantId: {}", componentId, tenantId);

        ruleComponentApplicationService.enableComponent(tenantId, componentId);

        return ResponseEntity.ok().build();
    }

    /**
     * 禁用组件
     * POST /api/components/{componentId}/disable
     */
    @PostMapping("/{componentId}/disable")
    public ResponseEntity<Void> disableComponent(
            @PathVariable String componentId,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/components/{}/disable - tenantId: {}", componentId, tenantId);

        ruleComponentApplicationService.disableComponent(tenantId, componentId);

        return ResponseEntity.ok().build();
    }

    /**
     * 删除组件
     * DELETE /api/components/{componentId}
     */
    @DeleteMapping("/{componentId}")
    public ResponseEntity<Void> deleteComponent(
            @PathVariable String componentId
    ) {
        log.info("DELETE /api/components/{}", componentId);

        ruleComponentApplicationService.deleteComponent(componentId);

        return ResponseEntity.ok().build();
    }
}
