package com.dms.liteflow.api.flowchain;

import com.dms.liteflow.application.flowchain.FlowChainApplicationService;
import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 流程链控制器
 * <p>
 * API 层：处理 HTTP 请求
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/chains")
@RequiredArgsConstructor
public class FlowChainController {

    private final FlowChainApplicationService flowChainApplicationService;

    /**
     * 创建流程链
     * POST /api/chains
     */
    @PostMapping
    public ResponseEntity<FlowChain> createChain(
            @RequestParam Long tenantId,
            @RequestParam String chainName,
            @RequestParam String chainCode,
            @RequestParam(required = false) String description,
            @RequestParam(defaultValue = "DATABASE") String configType,
            @RequestParam(required = false, defaultValue = "false") Boolean transactional,
            @RequestParam(required = false) Integer transactionTimeout,
            @RequestParam(required = false) String transactionPropagation
    ) {
        log.info("POST /api/chains - tenantId: {}, chainName: {}", tenantId, chainName);

        FlowChain chain = flowChainApplicationService.createChain(
                tenantId, chainName, chainCode, description, configType,
                transactional, transactionTimeout, transactionPropagation
        );

        return ResponseEntity.ok(chain);
    }

    /**
     * 查询租户的所有流程链
     * GET /api/chains?tenantId={tenantId}&status={status}
     */
    @GetMapping
    public ResponseEntity<List<FlowChain>> getChains(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String status
    ) {
        log.info("GET /api/chains - tenantId: {}, status: {}", tenantId, status);

        List<FlowChain> chains;
        if ("PUBLISHED".equals(status)) {
            chains = flowChainApplicationService.getPublishedChains(tenantId);
        } else {
            chains = flowChainApplicationService.getChainsByTenant(tenantId);
        }

        return ResponseEntity.ok(chains);
    }

    /**
     * 查询流程链详情
     * GET /api/chains/{chainId}
     */
    @GetMapping("/{chainId}")
    public ResponseEntity<FlowChain> getChain(
            @PathVariable Long chainId,
            @RequestParam Long tenantId
    ) {
        log.info("GET /api/chains/{} - tenantId: {}", chainId, tenantId);

        FlowChain chain = flowChainApplicationService.getChainByName(tenantId, chainId.toString());
        return ResponseEntity.ok(chain);
    }

    /**
     * 更新流程链
     * PUT /api/chains/{chainId}
     */
    @PutMapping("/{chainId}")
    public ResponseEntity<FlowChain> updateChain(
            @PathVariable Long chainId,
            @RequestParam Long tenantId,
            @RequestParam String chainCode,
            @RequestParam(required = false) String description
    ) {
        log.info("PUT /api/chains/{} - tenantId: {}", chainId, tenantId);

        FlowChain chain = flowChainApplicationService.updateChain(
                tenantId, chainId, chainCode, description
        );

        return ResponseEntity.ok(chain);
    }

    /**
     * 发布流程链
     * POST /api/chains/{chainId}/publish
     */
    @PostMapping("/{chainId}/publish")
    public ResponseEntity<Void> publishChain(
            @PathVariable Long chainId,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/chains/{}/publish - tenantId: {}", chainId, tenantId);

        flowChainApplicationService.publishChain(tenantId, chainId);

        return ResponseEntity.ok().build();
    }

    /**
     * 删除流程链
     * DELETE /api/chains/{chainId}
     */
    @DeleteMapping("/{chainId}")
    public ResponseEntity<Void> deleteChain(
            @PathVariable Long chainId,
            @RequestParam Long tenantId
    ) {
        log.info("DELETE /api/chains/{} - tenantId: {}", chainId, tenantId);

        flowChainApplicationService.deleteChain(tenantId, chainId);

        return ResponseEntity.ok().build();
    }
}
