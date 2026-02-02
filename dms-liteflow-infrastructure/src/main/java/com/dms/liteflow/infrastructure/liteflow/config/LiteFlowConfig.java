package com.dms.liteflow.infrastructure.liteflow.config;

import com.dms.liteflow.infrastructure.persistence.mapper.FlowChainMapper;
import com.dms.liteflow.infrastructure.persistence.mapper.RuleComponentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LiteFlow 数据库配置源配置类
 * <p>
 * 配置 CustomJdbcRuleSource Bean，支持多租户配置隔离
 * 使用 MyBatis Mapper 进行数据访问
 * </p>
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "liteflow.rule-source-ext-data-map.pollingEnabled", havingValue = "true", matchIfMissing = false)
public class LiteFlowConfig {

    /**
     * 创建自定义 JDBC 规则源 Bean
     *
     * @param flowChainMapper 流程链 Mapper
     * @param ruleComponentMapper 规则组件 Mapper
     * @param properties LiteFlow SQL 插件配置属性
     * @return CustomJdbcRuleSource
     */
    @Bean
    public CustomJdbcRuleSource customJdbcRuleSource(
            FlowChainMapper flowChainMapper,
            RuleComponentMapper ruleComponentMapper,
            LiteFlowSqlProperties properties) {
        log.info("Creating CustomJdbcRuleSource with application: {}", properties.getApplicationName());
        return new CustomJdbcRuleSource(
                flowChainMapper,
                ruleComponentMapper,
                properties.getApplicationName()
        );
    }

    /**
     * LiteFlow SQL 插件配置属性
     */
    @Bean
    @ConfigurationProperties(prefix = "liteflow.rule-source-ext-data-map")
    public LiteFlowSqlProperties liteFlowSqlProperties() {
        return new LiteFlowSqlProperties();
    }

    /**
     * LiteFlow SQL 插件配置属性类
     */
    public static class LiteFlowSqlProperties {
        private String applicationName;
        private boolean pollingEnabled = true;
        private int pollingIntervalSeconds = 60;
        private int pollingStartSeconds = 60;
        private boolean sqlLogEnabled = false;

        public String getApplicationName() { return applicationName; }
        public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

        public boolean isPollingEnabled() { return pollingEnabled; }
        public void setPollingEnabled(boolean pollingEnabled) { this.pollingEnabled = pollingEnabled; }

        public int getPollingIntervalSeconds() { return pollingIntervalSeconds; }
        public void setPollingIntervalSeconds(int pollingIntervalSeconds) { this.pollingIntervalSeconds = pollingIntervalSeconds; }

        public int getPollingStartSeconds() { return pollingStartSeconds; }
        public void setPollingStartSeconds(int pollingStartSeconds) { this.pollingStartSeconds = pollingStartSeconds; }

        public boolean isSqlLogEnabled() { return sqlLogEnabled; }
        public void setSqlLogEnabled(boolean sqlLogEnabled) { this.sqlLogEnabled = sqlLogEnabled; }
    }
}
