package com.dms.liteflow.infrastructure.liteflow.config;

import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.dms.liteflow.infrastructure.persistence.entity.FlowChainEntity;
import com.dms.liteflow.infrastructure.persistence.entity.RuleComponentEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.FlowChainMapper;
import com.dms.liteflow.infrastructure.persistence.mapper.RuleComponentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 自定义 JDBC 规则源（基于 MyBatis）
 * <p>
 * 支持多租户配置隔离，从 TenantContext 获取当前租户ID
 * 使用 MyBatis Mapper 进行数据访问，而非直接使用 JDBC
 * </p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "liteflow.rule-source", havingValue = "sql", matchIfMissing = false)
public class CustomJdbcRuleSource {

    private final FlowChainMapper flowChainMapper;
    private final RuleComponentMapper ruleComponentMapper;
    private final String applicationName;

    public CustomJdbcRuleSource(
            FlowChainMapper flowChainMapper,
            RuleComponentMapper ruleComponentMapper,
            String applicationName) {
        this.flowChainMapper = flowChainMapper;
        this.ruleComponentMapper = ruleComponentMapper;
        this.applicationName = applicationName;
        log.info("CustomJdbcRuleSource initialized with application: {}", applicationName);
    }

    /**
     * 获取流程链配置
     *
     * @return 流程链配置列表
     */
    public List<ChainConfig> getChainConfigs() {
        Long tenantId = getCurrentTenantId();

        log.debug("Loading chain configs for application: {}, tenant: {}", applicationName, tenantId);

        try {
            List<FlowChainEntity> entities = flowChainMapper.selectPublishedChainsForLiteFlow(
                    applicationName,
                    tenantId
            );

            List<ChainConfig> configs = entities.stream()
                    .map(this::convertToChainConfig)
                    .collect(Collectors.toList());

            log.info("Loaded {} chain configs for tenant: {}", configs.size(), tenantId);
            return configs;

        } catch (Exception e) {
            log.error("Failed to load chain configs for tenant: {}", tenantId, e);
            return List.of();
        }
    }

    /**
     * 获取脚本组件配置
     *
     * @return 脚本组件配置列表
     */
    public List<ScriptConfig> getScriptConfigs() {
        Long tenantId = getCurrentTenantId();

        log.debug("Loading script configs for application: {}, tenant: {}", applicationName, tenantId);

        try {
            List<RuleComponentEntity> entities = ruleComponentMapper.selectPublishedComponentsForLiteFlow(
                    applicationName,
                    tenantId
            );

            List<ScriptConfig> configs = entities.stream()
                    .map(this::convertToScriptConfig)
                    .collect(Collectors.toList());

            log.info("Loaded {} script configs for tenant: {}", configs.size(), tenantId);
            return configs;

        } catch (Exception e) {
            log.error("Failed to load script configs for tenant: {}", tenantId, e);
            return List.of();
        }
    }

    /**
     * 获取当前租户ID
     * <p>
     * 从 TenantContext 获取，如果为空则使用默认租户ID (1)
     * </p>
     *
     * @return 租户ID
     */
    private Long getCurrentTenantId() {
        try {
            TenantId tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                log.warn("TenantId not found in context, using default tenant: 1");
                return 1L;
            }
            return tenantId.getValue();
        } catch (Exception e) {
            log.warn("Failed to get tenantId from context, using default tenant: 1", e);
            return 1L;
        }
    }

    /**
     * 转换 FlowChainEntity 到 ChainConfig
     */
    private ChainConfig convertToChainConfig(FlowChainEntity entity) {
        ChainConfig config = new ChainConfig();
        config.setChainName(entity.getChainName());
        config.setChainCode(entity.getChainCode());
        config.setNamespace(entity.getNamespace());
        return config;
    }

    /**
     * 转换 RuleComponentEntity 到 ScriptConfig
     */
    private ScriptConfig convertToScriptConfig(RuleComponentEntity entity) {
        ScriptConfig config = new ScriptConfig();
        config.setScriptId(entity.getComponentId());
        config.setScriptData(entity.getContent()); // content 对应 component_code
        config.setScriptType(entity.getComponentType());
        config.setLanguage(entity.getLanguage());
        return config;
    }

    /**
     * 流程链配置
     */
    public static class ChainConfig {
        private String chainName;
        private String chainCode;
        private String namespace;

        public String getChainName() { return chainName; }
        public void setChainName(String chainName) { this.chainName = chainName; }

        public String getChainCode() { return chainCode; }
        public void setChainCode(String chainCode) { this.chainCode = chainCode; }

        public String getNamespace() { return namespace; }
        public void setNamespace(String namespace) { this.namespace = namespace; }
    }

    /**
     * 脚本组件配置
     */
    public static class ScriptConfig {
        private String scriptId;
        private String scriptData;
        private String scriptType;
        private String language;

        public String getScriptId() { return scriptId; }
        public void setScriptId(String scriptId) { this.scriptId = scriptId; }

        public String getScriptData() { return scriptData; }
        public void setScriptData(String scriptData) { this.scriptData = scriptData; }

        public String getScriptType() { return scriptType; }
        public void setScriptType(String scriptType) { this.scriptType = scriptType; }

        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
    }
}
