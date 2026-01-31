package com.dms.liteflow.application.scheduled;

import com.dms.liteflow.application.monitoring.MonitoringQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 监控数据清理调度器
 * <p>
 * 定期清理过期的监控数据
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringDataCleanupScheduler {

    private final MonitoringQueryService monitoringQueryService;

    /**
     * 清理7天前的原始执行数据
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
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
     * 清理30天前的小时级统计数据
     * 每周日凌晨3点执行
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
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
     * 清理1年前的日级统计数据
     * 每月1号凌晨4点执行
     */
    @Scheduled(cron = "0 0 4 1 * ?")
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
