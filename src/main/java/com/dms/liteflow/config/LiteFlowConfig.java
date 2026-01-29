package com.dms.liteflow.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * LiteFlow 自动配置类
 */
@Slf4j
@Configuration
public class LiteFlowConfig {

    @Value("${liteflow.source}")
    private String sourcePath;

    @Value("${liteflow.monitoring.enabled:true}")
    private boolean monitoringEnabled;

    @Value("${liteflow.monitoring.collect.collectTime:true}")
    private boolean collectTime;

    @Value("${liteflow.monitoring.collect.collectPath:true}")
    private boolean collectPath;

    @Value("${liteflow.monitoring.collect.collectError:true}")
    private boolean collectError;

    @Value("${liteflow.transaction.enabled:true}")
    private boolean transactionEnabled;

    /**
     * 配置 FlowExecutor
     */
    @Bean
    public FlowExecutor flowExecutor() {
        log.info("Initializing LiteFlow FlowExecutor with source path: {}", sourcePath);
        log.info("Monitoring enabled: {}, collectTime: {}, collectPath: {}, collectError: {}",
                monitoringEnabled, collectTime, collectPath, collectError);
        log.info("Transaction enabled: {}", transactionEnabled);

        return FlowExecutor.loadInstance();
    }

    /**
     * 配置默认上下文
     */
    @Bean
    public DefaultContext defaultContext() {
        log.info("Initializing LiteFlow DefaultContext");
        return new DefaultContext();
    }
}
