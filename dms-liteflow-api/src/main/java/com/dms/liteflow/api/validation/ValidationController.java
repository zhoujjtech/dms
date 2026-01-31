package com.dms.liteflow.api.validation;

import com.dms.liteflow.application.validation.ConfigValidatorService;
import com.dms.liteflow.domain.shared.kernel.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 配置验证控制器
 * <p>
 * API 层：处理配置验证请求
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/validation")
@RequiredArgsConstructor
public class ValidationController {

    private final ConfigValidatorService configValidatorService;

    /**
     * 验证流程链配置
     * POST /api/validation/chains/{chainId}
     */
    @PostMapping("/chains/{chainId}")
    public ResponseEntity<ValidationResult> validateChain(
            @PathVariable Long chainId,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/validation/chains/{} - tenantId: {}", chainId, tenantId);

        ValidationResult result = configValidatorService.validateChain(tenantId, chainId);

        return ResponseEntity.ok(result);
    }

    /**
     * 验证组件配置
     * POST /api/validation/components/{componentId}
     */
    @PostMapping("/components/{componentId}")
    public ResponseEntity<ValidationResult> validateComponent(
            @PathVariable String componentId,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/validation/components/{} - tenantId: {}", componentId, tenantId);

        ValidationResult result = configValidatorService.validateComponent(tenantId, componentId);

        return ResponseEntity.ok(result);
    }

    /**
     * 验证租户的所有配置
     * POST /api/validation/tenant
     */
    @PostMapping("/tenant")
    public ResponseEntity<ValidationResult> validateTenantConfig(
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/validation/tenant - tenantId: {}", tenantId);

        ValidationResult result = configValidatorService.validateTenantConfig(tenantId);

        return ResponseEntity.ok(result);
    }

    /**
     * 验证所有流程链
     * POST /api/validation/chains
     */
    @PostMapping("/chains")
    public ResponseEntity<ValidationResult> validateAllChains(
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/validation/chains - tenantId: {}", tenantId);

        ValidationResult result = configValidatorService.validateAllChains(tenantId);

        return ResponseEntity.ok(result);
    }
}
