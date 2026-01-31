package com.dms.liteflow.api.subchain;

import com.dms.liteflow.application.subchain.SubChainApplicationService;
import com.dms.liteflow.domain.flowexec.entity.FlowSubChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 子流程管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/subchains")
@RequiredArgsConstructor
public class SubChainController {

    private final SubChainApplicationService subChainApplicationService;

    /**
     * 创建子流程
     * POST /api/subchains
     */
    @PostMapping
    public ResponseEntity<FlowSubChain> createSubChain(
            @RequestParam Long tenantId,
            @RequestParam String subChainName,
            @RequestParam String chainCode,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Long parentChainId
    ) {
        log.info("POST /api/subchains - tenantId: {}, subChainName: {}", tenantId, subChainName);

        FlowSubChain subChain = subChainApplicationService.createSubChain(
                tenantId, subChainName, chainCode, description, parentChainId
        );

        return ResponseEntity.ok(subChain);
    }

    /**
     * 查询子流程详情
     * GET /api/subchains/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<FlowSubChain> getSubChain(@PathVariable Long id) {
        log.info("GET /api/subchains/{}", id);

        FlowSubChain subChain = subChainApplicationService.getSubChain(id);

        return ResponseEntity.ok(subChain);
    }

    /**
     * 更新子流程
     * PUT /api/subchains/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<FlowSubChain> updateSubChain(
            @PathVariable Long id,
            @RequestParam String subChainName,
            @RequestParam String chainCode,
            @RequestParam(required = false) String description
    ) {
        log.info("PUT /api/subchains/{}", id);

        FlowSubChain subChain = subChainApplicationService.updateSubChain(
                id, subChainName, chainCode, description
        );

        return ResponseEntity.ok(subChain);
    }

    /**
     * 发布子流程
     * POST /api/subchains/{id}/publish
     */
    @PostMapping("/{id}/publish")
    public ResponseEntity<Void> publishSubChain(@PathVariable Long id) {
        log.info("POST /api/subchains/{}/publish", id);

        subChainApplicationService.publishSubChain(id);

        return ResponseEntity.ok().build();
    }

    /**
     * 删除子流程
     * DELETE /api/subchains/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubChain(@PathVariable Long id) {
        log.info("DELETE /api/subchains/{}", id);

        subChainApplicationService.deleteSubChain(id);

        return ResponseEntity.ok().build();
    }

    /**
     * 查询租户的所有子流程
     * GET /api/subchains?tenantId={tenantId}
     */
    @GetMapping
    public ResponseEntity<List<FlowSubChain>> getSubChains(@RequestParam Long tenantId) {
        log.info("GET /api/subchains - tenantId: {}", tenantId);

        List<FlowSubChain> subChains = subChainApplicationService.getSubChainsByTenant(tenantId);

        return ResponseEntity.ok(subChains);
    }

    /**
     * 查询父流程的所有子流程
     * GET /api/subchains/parent/{parentChainId}
     */
    @GetMapping("/parent/{parentChainId}")
    public ResponseEntity<List<FlowSubChain>> getSubChainsByParent(@PathVariable Long parentChainId) {
        log.info("GET /api/subchains/parent/{}", parentChainId);

        List<FlowSubChain> subChains = subChainApplicationService.getSubChainsByParent(parentChainId);

        return ResponseEntity.ok(subChains);
    }
}
