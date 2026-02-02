package com.dms.liteflow.infrastructure.scheduled;

import com.dms.liteflow.infrastructure.liteflow.FlowConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 配置刷新调度器
 * <p>
 * 已废弃：使用 ElasticJob 分布式任务替代（参见 {@link com.dms.liteflow.infrastructure.schedule.job.ConfigRefreshJob}）
 * 保留此类仅作为手动刷新的工具类
 * </p>
 * @deprecated 使用 {@link com.dms.liteflow.infrastructure.schedule.job.ConfigRefreshJob} 替代
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Deprecated
public class ConfigRefreshScheduler {

    private final FlowConfigService flowConfigService;

    /**
     * 手动刷新所有配置缓存
     * <p>
     * 注意：定时调度已由 ElasticJob 接管
     * </p>
     */
    public void refreshAllConfigs() {
        log.debug("Starting manual config cache refresh");
        try {
            flowConfigService.clearAllCache();
            log.debug("Successfully cleared all config cache");
        } catch (Exception e) {
            log.error("Failed to refresh config cache", e);
        }
    }
}
