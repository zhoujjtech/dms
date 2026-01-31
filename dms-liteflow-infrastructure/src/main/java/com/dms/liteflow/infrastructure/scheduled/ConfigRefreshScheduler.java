package com.dms.liteflow.infrastructure.scheduled;

import com.dms.liteflow.infrastructure.liteflow.FlowConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 配置刷新调度器
 * <p>
 * 定时刷新配置缓存
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigRefreshScheduler {

    private final FlowConfigService flowConfigService;

    /**
     * 定时刷新所有配置缓存
     * 每5分钟执行一次
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    public void refreshAllConfigs() {
        log.debug("Starting scheduled config cache refresh");
        try {
            flowConfigService.clearAllCache();
            log.debug("Successfully cleared all config cache");
        } catch (Exception e) {
            log.error("Failed to refresh config cache", e);
        }
    }
}
