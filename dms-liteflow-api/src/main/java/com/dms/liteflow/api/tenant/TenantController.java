package com.dms.liteflow.api.tenant;

import com.dms.liteflow.application.tenant.TenantApplicationService;
import com.dms.liteflow.domain.tenant.aggregate.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantApplicationService tenantApplicationService;

    /**
     * 创建租户
     * POST /api/tenants
     */
    @PostMapping
    public ResponseEntity<Tenant> createTenant(
            @RequestParam String tenantCode,
            @RequestParam String tenantName,
            @RequestParam(required = false, defaultValue = "100") Integer maxChains,
            @RequestParam(required = false, defaultValue = "1000") Integer maxComponents
    ) {
        log.info("POST /api/tenants - tenantCode: {}", tenantCode);

        Tenant tenant = tenantApplicationService.createTenant(
                tenantCode, tenantName, maxChains, maxComponents
        );

        return ResponseEntity.ok(tenant);
    }

    /**
     * 更新租户
     * PUT /api/tenants/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<Tenant> updateTenant(
            @PathVariable Long id,
            @RequestParam(required = false) String tenantName,
            @RequestParam(required = false) Integer maxChains,
            @RequestParam(required = false) Integer maxComponents
    ) {
        log.info("PUT /api/tenants/{}", id);

        Tenant tenant = tenantApplicationService.updateTenant(
                id, tenantName, maxChains, maxComponents
        );

        return ResponseEntity.ok(tenant);
    }

    /**
     * 激活租户
     * POST /api/tenants/{id}/activate
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateTenant(@PathVariable Long id) {
        log.info("POST /api/tenants/{}/activate", id);

        tenantApplicationService.activateTenant(id);

        return ResponseEntity.ok().build();
    }

    /**
     * 停用租户
     * POST /api/tenants/{id}/deactivate
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateTenant(@PathVariable Long id) {
        log.info("POST /api/tenants/{}/deactivate", id);

        tenantApplicationService.deactivateTenant(id);

        return ResponseEntity.ok().build();
    }

    /**
     * 检查租户配额
     * GET /api/tenants/{id}/quota
     */
    @GetMapping("/{id}/quota")
    public ResponseEntity<Boolean> checkQuota(@PathVariable Long id) {
        log.info("GET /api/tenants/{}/quota", id);

        boolean withinQuota = tenantApplicationService.checkQuota(id);

        return ResponseEntity.ok(withinQuota);
    }

    /**
     * 查询所有租户
     * GET /api/tenants
     */
    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        log.info("GET /api/tenants");

        List<Tenant> tenants = tenantApplicationService.getAllTenants();

        return ResponseEntity.ok(tenants);
    }

    /**
     * 根据代码查询租户
     * GET /api/tenants/code/{tenantCode}
     */
    @GetMapping("/code/{tenantCode}")
    public ResponseEntity<Tenant> getTenantByCode(@PathVariable String tenantCode) {
        log.info("GET /api/tenants/code/{}", tenantCode);

        Tenant tenant = tenantApplicationService.getTenantByCode(tenantCode);

        return ResponseEntity.ok(tenant);
    }
}
