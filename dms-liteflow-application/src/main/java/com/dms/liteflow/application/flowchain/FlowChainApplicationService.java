package com.dms.liteflow.application.flowchain;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.flowexec.repository.FlowChainRepository;
import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 流程链应用服务
 * <p>
 * 应用层：编排业务逻辑，协调领域模型
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowChainApplicationService {

    private final FlowChainRepository flowChainRepository;
    private final RuleComponentRepository ruleComponentRepository;

    /**
     * 创建流程链
     */
    @Transactional
    public FlowChain createChain(
            Long tenantId,
            String chainName,
            String chainCode,
            String description,
            String configType,
            Boolean transactional,
            Integer transactionTimeout,
            String transactionPropagation
    ) {
        log.info("Creating flow chain: {} for tenant: {}", chainName, tenantId);

        // 检查名称是否已存在
        TenantId tenant = TenantId.of(tenantId);
        if (flowChainRepository.existsByTenantIdAndName(tenant, chainName)) {
            throw new IllegalArgumentException("Flow chain name already exists: " + chainName);
        }

        // 验证组件引用
        validateComponentReferences(tenant, chainCode);

        // 构建领域对象
        FlowChain chain = FlowChain.builder()
                .tenantId(tenant)
                .chainName(chainName)
                .chainCode(chainCode)
                .description(description)
                .configType(configType)
                .status(ComponentStatus.DRAFT)
                .currentVersion(1)
                .transactional(transactional)
                .transactionTimeout(transactionTimeout)
                .transactionPropagation(transactionPropagation)
                .build();

        // 保存
        return flowChainRepository.save(chain);
    }

    /**
     * 更新流程链
     */
    @Transactional
    public FlowChain updateChain(
            Long tenantId,
            Long chainId,
            String chainCode,
            String description
    ) {
        log.info("Updating flow chain: {} for tenant: {}", chainId, tenantId);

        FlowChain chain = flowChainRepository.findById(ChainId.of(chainId))
                .orElseThrow(() -> new IllegalArgumentException("Flow chain not found: " + chainId));

        // 验证租户
        if (!chain.getTenantId().getValue().equals(tenantId)) {
            throw new IllegalArgumentException("Flow chain does not belong to tenant: " + tenantId);
        }

        // 验证组件引用
        validateComponentReferences(chain.getTenantId(), chainCode);

        // 更新
        chain.updateChain(chainCode, description);
        return flowChainRepository.save(chain);
    }

    /**
     * 发布流程链
     */
    @Transactional
    public void publishChain(Long tenantId, Long chainId) {
        log.info("Publishing flow chain: {} for tenant: {}", chainId, tenantId);

        FlowChain chain = flowChainRepository.findById(ChainId.of(chainId))
                .orElseThrow(() -> new IllegalArgumentException("Flow chain not found: " + chainId));

        // 验证租户
        if (!chain.getTenantId().getValue().equals(tenantId)) {
            throw new IllegalArgumentException("Flow chain does not belong to tenant: " + tenantId);
        }

        chain.publish();
        flowChainRepository.save(chain);
    }

    /**
     * 启用流程链
     */
    @Transactional
    public void enableChain(Long tenantId, Long chainId) {
        log.info("Enabling flow chain: {} for tenant: {}", chainId, tenantId);

        FlowChain chain = flowChainRepository.findById(ChainId.of(chainId))
                .orElseThrow(() -> new IllegalArgumentException("Flow chain not found: " + chainId));

        // 验证租户
        if (!chain.getTenantId().getValue().equals(tenantId)) {
            throw new IllegalArgumentException("Flow chain does not belong to tenant: " + tenantId);
        }

        chain.enable();
        flowChainRepository.save(chain);
    }

    /**
     * 禁用流程链
     */
    @Transactional
    public void disableChain(Long tenantId, Long chainId) {
        log.info("Disabling flow chain: {} for tenant: {}", chainId, tenantId);

        FlowChain chain = flowChainRepository.findById(ChainId.of(chainId))
                .orElseThrow(() -> new IllegalArgumentException("Flow chain not found: " + chainId));

        // 验证租户
        if (!chain.getTenantId().getValue().equals(tenantId)) {
            throw new IllegalArgumentException("Flow chain does not belong to tenant: " + tenantId);
        }

        chain.disable();
        flowChainRepository.save(chain);
    }

    /**
     * 删除流程链
     */
    @Transactional
    public void deleteChain(Long tenantId, Long chainId) {
        log.info("Deleting flow chain: {} for tenant: {}", chainId, tenantId);

        FlowChain chain = flowChainRepository.findById(ChainId.of(chainId))
                .orElseThrow(() -> new IllegalArgumentException("Flow chain not found: " + chainId));

        // 验证租户
        if (!chain.getTenantId().getValue().equals(tenantId)) {
            throw new IllegalArgumentException("Flow chain does not belong to tenant: " + tenantId);
        }

        if (chain.getStatus() == ComponentStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot delete published chain. Archive it first.");
        }

        chain.softDelete();
        flowChainRepository.save(chain);
    }

    /**
     * 查询租户的所有流程链
     */
    public List<FlowChain> getChainsByTenant(Long tenantId) {
        return flowChainRepository.findByTenantId(TenantId.of(tenantId));
    }

    /**
     * 查询已发布的流程链
     */
    public List<FlowChain> getPublishedChains(Long tenantId) {
        return flowChainRepository.findByTenantIdAndStatus(
                TenantId.of(tenantId),
                ComponentStatus.PUBLISHED
        );
    }

    /**
     * 根据名称查询流程链
     */
    public FlowChain getChainByName(Long tenantId, String chainName) {
        return flowChainRepository.findByTenantIdAndName(TenantId.of(tenantId), chainName)
                .orElseThrow(() -> new IllegalArgumentException("Flow chain not found: " + chainName));
    }

    /**
     * 验证组件引用
     */
    private void validateComponentReferences(TenantId tenantId, String chainCode) {
        // TODO: 实现EL表达式解析和组件引用验证
        // 1. 解析EL表达式，提取所有组件ID
        // 2. 检查每个组件ID是否存在于租户的组件列表中
        // 3. 如果有组件不存在，抛出异常
        log.debug("Validating component references for chain code: {}", chainCode);
    }
}
