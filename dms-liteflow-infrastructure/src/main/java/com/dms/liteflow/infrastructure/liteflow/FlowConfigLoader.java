package com.dms.liteflow.infrastructure.liteflow;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.flowexec.repository.FlowChainRepository;
import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程配置加载器
 * <p>
 * 从数据库动态加载 LiteFlow 配置
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowConfigLoader {

    private final FlowChainRepository flowChainRepository;
    private final RuleComponentRepository ruleComponentRepository;

    /**
     * 加载租户的所有流程链配置
     *
     * @param tenantId 租户ID
     * @return XML 格式的流程链配置
     */
    public String loadFlowChainConfig(Long tenantId) {
        log.debug("Loading flow chain config for tenant: {}", tenantId);

        List<FlowChain> chains = flowChainRepository.findByTenantIdAndStatus(
                TenantId.of(tenantId),
                ComponentStatus.PUBLISHED
        );

        if (chains.isEmpty()) {
            log.warn("No published flow chains found for tenant: {}", tenantId);
            return "";
        }

        // 构建 XML 格式配置
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<flow>\n");

        for (FlowChain chain : chains) {
            xml.append("  <chain name=\"").append(chain.getChainName()).append("\">\n");
            xml.append("    ").append(chain.getChainCode()).append("\n");
            xml.append("  </chain>\n");
        }

        xml.append("</flow>");
        String config = xml.toString();

        log.debug("Loaded flow chain config for tenant {}: {}", tenantId, config);
        return config;
    }

    /**
     * 加载租户的所有组件配置
     *
     * @param tenantId 租户ID
     * @return 组件列表
     */
    public List<String> loadComponentConfig(Long tenantId) {
        log.debug("Loading component config for tenant: {}", tenantId);

        List<RuleComponent> components = ruleComponentRepository.findByTenantIdAndStatus(
                TenantId.of(tenantId),
                ComponentStatus.PUBLISHED
        );

        List<String> componentIds = components.stream()
                .map(component -> component.getComponentId().getValue())
                .collect(Collectors.toList());

        log.debug("Loaded {} components for tenant: {}", componentIds.size(), tenantId);
        return componentIds;
    }

    /**
     * 加载指定流程链的配置
     *
     * @param tenantId  租户ID
     * @param chainName 流程链名称
     * @return 流程链配置
     */
    public FlowChain loadChainConfig(Long tenantId, String chainName) {
        log.debug("Loading chain config for tenant: {}, chain: {}", tenantId, chainName);

        return flowChainRepository.findByTenantIdAndName(
                TenantId.of(tenantId),
                chainName
        ).orElse(null);
    }

    /**
     * 检查租户是否有任何已发布的配置
     *
     * @param tenantId 租户ID
     * @return 是否有已发布的配置
     */
    public boolean hasPublishedConfig(Long tenantId) {
        List<FlowChain> chains = flowChainRepository.findByTenantIdAndStatus(
                TenantId.of(tenantId),
                ComponentStatus.PUBLISHED
        );
        return !chains.isEmpty();
    }
}
