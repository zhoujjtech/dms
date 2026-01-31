package com.dms.liteflow.infrastructure.liteflow;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 流程配置服务
 * <p>
 * 提供配置查询和缓存管理
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlowConfigService {

    private final FlowConfigLoader flowConfigLoader;

    /**
     * 获取租户的流程链配置（带缓存）
     *
     * @param tenantId 租户ID
     * @return XML 格式的流程链配置
     */
    @Cacheable(value = "flowConfig", key = "#tenantId")
    public String getFlowChainConfig(Long tenantId) {
        log.info("Loading flow chain config from database for tenant: {}", tenantId);
        return flowConfigLoader.loadFlowChainConfig(tenantId);
    }

    /**
     * 获取租户的组件配置（带缓存）
     *
     * @param tenantId 租户ID
     * @return 组件ID列表
     */
    @Cacheable(value = "componentConfig", key = "#tenantId")
    public List<String> getComponentConfig(Long tenantId) {
        log.info("Loading component config from database for tenant: {}", tenantId);
        return flowConfigLoader.loadComponentConfig(tenantId);
    }

    /**
     * 获取指定流程链配置
     *
     * @param tenantId  租户ID
     * @param chainName 流程链名称
     * @return 流程链配置
     */
    @Cacheable(value = "chainConfig", key = "#tenantId + ':' + #chainName")
    public FlowChain getChainConfig(Long tenantId, String chainName) {
        log.info("Loading chain config from database for tenant: {}, chain: {}", tenantId, chainName);
        return flowConfigLoader.loadChainConfig(tenantId, chainName);
    }

    /**
     * 刷新租户配置缓存
     *
     * @param tenantId 租户ID
     */
    @CacheEvict(value = {"flowConfig", "componentConfig"}, key = "#tenantId")
    public void refreshConfig(Long tenantId) {
        log.info("Refreshing config cache for tenant: {}", tenantId);
    }

    /**
     * 清空所有配置缓存
     */
    @CacheEvict(value = {"flowConfig", "componentConfig", "chainConfig"}, allEntries = true)
    public void clearAllCache() {
        log.info("Clearing all config cache");
    }

    /**
     * 检查租户是否有已发布的配置
     *
     * @param tenantId 租户ID
     * @return 是否有已发布的配置
     */
    public boolean hasPublishedConfig(Long tenantId) {
        return flowConfigLoader.hasPublishedConfig(tenantId);
    }
}
