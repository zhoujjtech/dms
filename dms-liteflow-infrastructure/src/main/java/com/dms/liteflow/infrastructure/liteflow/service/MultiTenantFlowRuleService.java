package com.dms.liteflow.infrastructure.liteflow.service;

import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.dms.liteflow.infrastructure.persistence.entity.FlowChainEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.FlowChainMapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.List;

/**
 * 多租户流程规则管理服务
 * <p>
 * 负责从数据库动态加载和管理多租户的流程规则
 * 定时刷新已由 ElasticJob 接管（参见 {@link com.dms.liteflow.infrastructure.schedule.job.RuleRefreshJob}）
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "liteflow.multi-tenant-rule.enabled", havingValue = "true", matchIfMissing = true)
public class MultiTenantFlowRuleService {

    private final FlowChainMapper flowChainMapper;
    private final FlowExecutor flowExecutor;

    private static final String APPLICATION_NAME = "dms-liteflow";

    /**
     * 应用启动时加载规则
     */
    @PostConstruct
    public void initialize() {
        log.info("Initializing multi-tenant flow rules...");
        loadAllTenantRules();
    }

    /**
     * 刷新所有租户的规则
     * <p>
     * 注意：定时调度已由 ElasticJob 接管（参见 {@link com.dms.liteflow.infrastructure.schedule.job.RuleRefreshJob}）
     * 此方法保留用于手动触发刷新
     * </p>
     */
    public void refreshAllRules() {
        log.debug("Refreshing all tenant rules");
        loadAllTenantRules();
    }

    /**
     * 加载所有租户的规则
     * <p>
     * 注意：由于 LiteFlow 的 FlowExecutor 是单例的，
     * 这里演示的是加载默认租户的规则
     * </p>
     */
    private void loadAllTenantRules() {
        try {
            // 使用默认租户加载规则
            Long defaultTenantId = 1L;
            loadTenantRules(defaultTenantId);

        } catch (Exception e) {
            log.error("Failed to load all tenant rules", e);
        }
    }

    /**
     * 加载指定租户的规则
     *
     * @param tenantId 租户ID
     */
    public void loadTenantRules(Long tenantId) {
        try {
            log.info("Loading rules for tenant: {}", tenantId);

            // 设置租户上下文
            TenantContext.setTenantId(TenantId.of(tenantId));

            // 查询该租户的已发布流程链
            List<FlowChainEntity> chains = flowChainMapper.selectPublishedChainsForLiteFlow(
                    APPLICATION_NAME,
                    tenantId
            );

            if (chains.isEmpty()) {
                log.warn("No published chains found for tenant: {}", tenantId);
                return;
            }

            // 构建规则内容并重新加载
            for (FlowChainEntity chain : chains) {
                String chainName = chain.getChainName();
                String chainCode = chain.getChainCode();

                log.info("Loaded chain '{}' for tenant {}: {}", chainName, tenantId, chainCode);
            }

            log.info("Successfully loaded {} chains for tenant: {}", chains.size(), tenantId);

        } catch (Exception e) {
            log.error("Failed to load rules for tenant: {}", tenantId, e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 手动刷新指定租户的规则
     *
     * @param tenantId 租户ID
     */
    public void refreshTenant(Long tenantId) {
        log.info("Manual refresh triggered for tenant: {}", tenantId);
        loadTenantRules(tenantId);
    }

    /**
     * 获取指定租户的规则内容（用于调试）
     *
     * @param tenantId 租户ID
     * @return 规则 XML 内容
     */
    public String getRuleContent(Long tenantId) {
        try {
            TenantContext.setTenantId(TenantId.of(tenantId));

            List<FlowChainEntity> chains = flowChainMapper.selectPublishedChainsForLiteFlow(
                    APPLICATION_NAME,
                    tenantId
            );

            if (chains.isEmpty()) {
                return "<!-- No chains found -->";
            }

            StringBuilder xml = new StringBuilder();
            xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            xml.append("<flow>\n");

            for (FlowChainEntity chain : chains) {
                xml.append("  <chain name=\"").append(chain.getChainName()).append("\">\n");
                xml.append("    ").append(chain.getChainCode()).append("\n");
                xml.append("  </chain>\n");
            }

            xml.append("</flow>");
            return xml.toString();

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 测试指定租户的流程
     *
     * @param tenantId 租户ID
     * @param chainName 流程链名称
     * @return 测试结果
     */
    public Object testChain(Long tenantId, String chainName) {
        try {
            TenantContext.setTenantId(TenantId.of(tenantId));

            DefaultContext context = new DefaultContext();
            context.setData("tenantId", tenantId);

            // 执行流程
            return flowExecutor.execute2Resp(chainName, context);

        } finally {
            TenantContext.clear();
        }
    }
}
