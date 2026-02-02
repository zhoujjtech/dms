package com.dms.liteflow.infrastructure.config;

import com.dms.liteflow.application.monitoring.MonitoringQueryService;
import com.dms.liteflow.infrastructure.liteflow.FlowConfigService;
import com.dms.liteflow.infrastructure.liteflow.service.MultiTenantFlowRuleService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * XXL-JOB 任务处理器
 * <p>
 * 定义所有分布式任务的处理逻辑
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class XxlJobHandlers {

    private final FlowConfigService flowConfigService;
    private final MultiTenantFlowRuleService multiTenantFlowRuleService;
    private final MonitoringQueryService monitoringQueryService;

    /**
     * 配置刷新任务
     * <p>
     * 每5分钟执行一次，清空所有配置缓存
     * </p>
     */
    @XxlJob("configRefreshJob")
    public void configRefreshJob() {
        log.info("XXL-JOB: Starting config refresh job");
        try {
            flowConfigService.clearAllCache();
            log.info("XXL-JOB: Config refresh job completed successfully");
        } catch (Exception e) {
            log.error("XXL-JOB: Config refresh job failed", e);
            throw e;  // 抛出异常，触发 XXL-JOB 失败告警
        }
    }

    /**
     * 规则刷新任务
     * <p>
     * 每60秒执行一次，刷新所有租户的流程规则
     * </p>
     */
    @XxlJob("ruleRefreshJob")
    public void ruleRefreshJob() {
        log.info("XXL-JOB: Starting rule refresh job");
        try {
            multiTenantFlowRuleService.refreshAllRules();
            log.info("XXL-JOB: Rule refresh job completed successfully");
        } catch (Exception e) {
            log.error("XXL-JOB: Rule refresh job failed", e);
            throw e;
        }
    }

    /**
     * 执行记录清理任务
     * <p>
     * 每天凌晨2点执行，清理7天前的执行记录
     * </p>
     */
    @XxlJob("cleanupExecutionJob")
    public void cleanupExecutionJob() {
        log.info("XXL-JOB: Starting cleanup execution records job");
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
            int deletedCount = monitoringQueryService.deleteExpiredRecords(cutoffTime);
            log.info("XXL-JOB: Deleted {} execution records", deletedCount);
        } catch (Exception e) {
            log.error("XXL-JOB: Cleanup execution records job failed", e);
            throw e;
        }
    }

    /**
     * 小时级统计数据清理任务
     * <p>
     * 每周日凌晨3点执行，清理30天前的小时级统计
     * </p>
     */
    @XxlJob("cleanupHourlyStatsJob")
    public void cleanupHourlyStatsJob() {
        log.info("XXL-JOB: Starting cleanup hourly stats job");
        try {
            // TODO: 实现小时级统计数据清理
            log.info("XXL-JOB: Cleanup hourly stats job completed");
        } catch (Exception e) {
            log.error("XXL-JOB: Cleanup hourly stats job failed", e);
            throw e;
        }
    }

    /**
     * 日级统计数据清理任务
     * <p>
     * 每月1号凌晨4点执行，清理1年前的日级统计
     * </p>
     */
    @XxlJob("cleanupDailyStatsJob")
    public void cleanupDailyStatsJob() {
        log.info("XXL-JOB: Starting cleanup daily stats job");
        try {
            // TODO: 实现日级统计数据清理
            log.info("XXL-JOB: Cleanup daily stats job completed");
        } catch (Exception e) {
            log.error("XXL-JOB: Cleanup daily stats job failed", e);
            throw e;
        }
    }

    /**
     * 任务执行示例（带参数）
     * <p>
     * 演示如何从 XXL-JOB 获取参数
     * </p>
     */
    @XxlJob("demoJob")
    public void demoJob() {
        // 获取任务参数
        String param = XxlJobHelper.getJobParam();
        log.info("XXL-JOB: Demo job executed with param: {}", param);

        // 获取任务ID
        int jobId = XxlJobHelper.getJobId();
        log.info("XXL-JOB: Job ID: {}", jobId);

        // 手动记录日志到 XXL-JOB
        XxlJobHelper.log("Custom log message");

        // 其他操作...
    }
}
