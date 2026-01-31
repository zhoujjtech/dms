package com.dms.liteflow.application.subchain;

import com.dms.liteflow.domain.flowexec.entity.FlowSubChain;
import com.dms.liteflow.domain.flowexec.repository.FlowSubChainRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 子流程应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubChainApplicationService {

    private final FlowSubChainRepository flowSubChainRepository;

    /**
     * 创建子流程
     */
    @Transactional
    public FlowSubChain createSubChain(
            Long tenantId,
            String subChainName,
            String chainCode,
            String description,
            Long parentChainId
    ) {
        log.info("Creating sub chain: {} for tenant: {}", subChainName, tenantId);

        FlowSubChain subChain = FlowSubChain.builder()
                .tenantId(tenantId)
                .subChainName(subChainName)
                .chainCode(chainCode)
                .description(description)
                .parentChainId(parentChainId)
                .status("DRAFT")
                .build();

        return flowSubChainRepository.save(subChain);
    }

    /**
     * 更新子流程
     */
    @Transactional
    public FlowSubChain updateSubChain(
            Long id,
            String subChainName,
            String chainCode,
            String description
    ) {
        log.info("Updating sub chain: {}", id);

        FlowSubChain subChain = flowSubChainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sub chain not found: " + id));

        subChain.setSubChainName(subChainName);
        subChain.setChainCode(chainCode);
        subChain.setDescription(description);

        return flowSubChainRepository.save(subChain);
    }

    /**
     * 发布子流程
     */
    @Transactional
    public void publishSubChain(Long id) {
        log.info("Publishing sub chain: {}", id);

        FlowSubChain subChain = flowSubChainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sub chain not found: " + id));

        subChain.setStatus("PUBLISHED");
        flowSubChainRepository.save(subChain);
    }

    /**
     * 删除子流程
     */
    @Transactional
    public void deleteSubChain(Long id) {
        log.info("Deleting sub chain: {}", id);
        flowSubChainRepository.deleteById(id);
    }

    /**
     * 查询子流程详情
     */
    public FlowSubChain getSubChain(Long id) {
        log.info("Getting sub chain: {}", id);

        return flowSubChainRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Sub chain not found: " + id));
    }

    /**
     * 查询租户的所有子流程
     */
    public List<FlowSubChain> getSubChainsByTenant(Long tenantId) {
        return flowSubChainRepository.findByTenantId(TenantId.of(tenantId));
    }

    /**
     * 查询父流程的所有子流程
     */
    public List<FlowSubChain> getSubChainsByParent(Long parentChainId) {
        return flowSubChainRepository.findByParentChainId(parentChainId);
    }

    /**
     * 根据名称查询子流程
     */
    public FlowSubChain getSubChainByName(Long tenantId, String subChainName) {
        return flowSubChainRepository.findByTenantIdAndName(
                TenantId.of(tenantId),
                subChainName
        ).orElseThrow(() -> new IllegalArgumentException("Sub chain not found: " + subChainName));
    }
}
