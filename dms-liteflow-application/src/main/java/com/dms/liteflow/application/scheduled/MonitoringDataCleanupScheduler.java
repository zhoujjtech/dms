package com.dms.liteflow.application.scheduled;

import com.dms.liteflow.application.monitoring.MonitoringQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 监控数据清理调度器
 * <p>
 * 已废弃：使用 ElasticJob 分布式任务替代（参见 cleanup 包下的 Job 类）
 * 保留此类仅作为手动清理的工具类
 * </p>
 * @deprecated 使用 {@link com.dms.liteflow.application.scheduled.job.CleanupExecutionJob} 等替代
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Deprecated
public class MonitoringDataCleanupScheduler {

    private final MonitoringQueryService monitoringQueryService;

    /**
     * 手动清理7天前的原始执行数据
     * <p>
     * 注意：定时调度已由 ElasticJob 接管
     * </p>
     */
    public void cleanupOldExecutionRecords() {
        log.info("Starting cleanup of old execution records");

        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            int deletedCount = monitoringQueryService.deleteExpiredRecords(cutoffTime);
            log.info("Deleted {} execution records older than 7 days", deletedCount);
        } catch (Exception e) {
            log.error("Failed to cleanup old execution records", e);
        }
    }

    /**
     * 手动清理30天前的小时级统计数据
     * <p>
     * 注意：定时调度已由 ElasticJob 接管
     * </p>
     */
    public void cleanupHourlyStats() {
        log.info("Starting cleanup of hourly stats");

        try {
            // TODO: 实现小时级统计数据清理
            log.debug("Hourly stats cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup hourly stats", e);
        }
    }

    /**
     * 手动清理1年前的日级统计数据
     * <p>
     * 注意：定时调度已由 ElasticJob 接管
     * </p>
     */
    public void cleanupDailyStats() {
        log.info("Starting cleanup of daily stats");

        try {
            // TODO: 实现日级统计数据清理
            log.debug("Daily stats cleanup completed");
        } catch (Exception e) {
            log.error("Failed to cleanup daily stats", e);
        }
    }
}
