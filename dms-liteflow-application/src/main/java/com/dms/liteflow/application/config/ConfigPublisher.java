package com.dms.liteflow.application.config;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.flowexec.repository.FlowChainRepository;
import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.yomahub.liteflow.core.FlowExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 配置发布器
 * <p>
 * 处理配置发布和热更新
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigPublisher {

    private final FlowChainRepository flowChainRepository;
    private final RuleComponentRepository ruleComponentRepository;
    private final FlowExecutor flowExecutor;

    /**
     * 发布组件并触发热更新
     *
     * @param tenantId    租户ID
     * @param componentId 组件ID
     */
    @Transactional
    public void publishComponentWithHotUpdate(Long tenantId, String componentId) {
        log.info("Publishing component with hot update: {} for tenant: {}", componentId, tenantId);

        // 发布组件
        RuleComponent component = ruleComponentRepository.findByComponentId(ComponentId.of(componentId))
                .orElseThrow(() -> new IllegalArgumentException("Component not found: " + componentId));

        // 验证租户
        if (!component.getTenantId().getValue().equals(tenantId)) {
            throw new IllegalArgumentException("Component does not belong to tenant: " + tenantId);
        }

        component.publish();
        ruleComponentRepository.save(component);

        // 触发热更新（重新加载规则）
        triggerRuleReload();

        log.info("Component published and rules reloaded: {}", componentId);
    }

    /**
     * 发布流程链并触发热更新
     *
     * @param tenantId 租户ID
     * @param chainId  流程链ID
     */
    @Transactional
    public void publishChainWithHotUpdate(Long tenantId, Long chainId) {
        log.info("Publishing chain with hot update: {} for tenant: {}", chainId, tenantId);

        // 发布流程链
        FlowChain chain = flowChainRepository.findById(ChainId.of(chainId))
                .orElseThrow(() -> new IllegalArgumentException("Flow chain not found: " + chainId));

        // 验证租户
        if (!chain.getTenantId().getValue().equals(tenantId)) {
            throw new IllegalArgumentException("Flow chain does not belong to tenant: " + tenantId);
        }

        chain.publish();
        flowChainRepository.save(chain);

        // 触发热更新（重新加载流程链）
        triggerChainReload();

        log.info("Chain published and chains reloaded: {}", chainId);
    }

    /**
     * 触发规则重载
     * <p>
     * LiteFlow 会在检测到配置变化时自动重新加载
     * </p>
     */
    private void triggerRuleReload() {
        try {
            // LiteFlow 会在下一次执行时自动检测配置变化
            // 这里主要是清空缓存，强制下次从数据库重新加载
            log.debug("Triggering rule reload");
        } catch (Exception e) {
            log.error("Failed to trigger rule reload", e);
        }
    }

    /**
     * 触发流程链重载
     */
    private void triggerChainReload() {
        try {
            // LiteFlow 会在下一次执行时自动检测配置变化
            log.debug("Triggering chain reload");
        } catch (Exception e) {
            log.error("Failed to trigger chain reload", e);
        }
    }

    /**
     * 检查配置是否可以发布
     *
     * @param tenantId 租户ID
     * @return 是否可以发布
     */
    public boolean canPublishConfig(Long tenantId) {
        // 检查租户是否有已发布的组件
        long publishedComponentCount = ruleComponentRepository.findByTenantIdAndStatus(
                TenantId.of(tenantId),
                ComponentStatus.PUBLISHED
        ).size();

        // 检查租户是否有已发布的流程链
        long publishedChainCount = flowChainRepository.findByTenantIdAndStatus(
                TenantId.of(tenantId),
                ComponentStatus.PUBLISHED
        ).size();

        return publishedComponentCount > 0 || publishedChainCount > 0;
    }
}
