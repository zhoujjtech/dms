package com.dms.liteflow.application.saga.monitoring;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Saga 告警服务
 * 监控 Saga 执行指标并在触发阈值时发送告警
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaAlertService {

    private final SagaMonitoringService monitoringService;

    // 告警阈值配置
    private static final double COMPENSATION_FAILURE_RATE_THRESHOLD = 5.0; // 5%
    private static final int MANUAL_INTERVENTION_THRESHOLD_PER_HOUR = 10; // 10次/小时
    private static final long EXECUTION_TIMEOUT_THRESHOLD_MS = 300000; // 5分钟

    // 告警历史记录
    private final ConcurrentHashMap<String, LocalDateTime> alertHistory = new ConcurrentHashMap<>();

    // 人工介入计数（按小时统计）
    private final ConcurrentHashMap<String, AtomicLongWrapper> manualInterventionCounts = new ConcurrentHashMap<>();

    /**
     * 检查并触发告警
     * 应该由定时任务定期调用
     */
    public List<Alert> checkAndTriggerAlerts() {
        List<Alert> alerts = new ArrayList<>();

        // 1. 检查补偿失败率
        Alert compensationAlert = checkCompensationFailureRate();
        if (compensationAlert != null) {
            alerts.add(compensationAlert);
        }

        // 2. 检查人工介入频率
        Alert manualInterventionAlert = checkManualInterventionRate();
        if (manualInterventionAlert != null) {
            alerts.add(manualInterventionAlert);
        }

        // 3. 清理过期的计数器（每小时）
        cleanupOldCounters();

        return alerts;
    }

    /**
     * 检查补偿失败率
     */
    private Alert checkCompensationFailureRate() {
        double compensationRate = monitoringService.getCompensationSuccessRate();
        double failureRate = 100.0 - compensationRate;

        if (failureRate > COMPENSATION_FAILURE_RATE_THRESHOLD) {
            String alertKey = "compensation_failure_rate";

            // 避免重复告警（1小时内只告警一次）
            if (shouldAlert(alertKey, 60)) {
                String message = String.format(
                        "Saga compensation failure rate is %.2f%%, exceeding threshold of %.2f%%",
                        failureRate, COMPENSATION_FAILURE_RATE_THRESHOLD
                );

                log.warn("ALERT: {}", message);
                sendAlert(AlertType.COMPENSATION_FAILURE_RATE, message);

                return Alert.builder()
                        .type(AlertType.COMPENSATION_FAILURE_RATE)
                        .message(message)
                        .severity(AlertSeverity.WARNING)
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        }

        return null;
    }

    /**
     * 检查人工介入频率
     */
    private Alert checkManualInterventionRate() {
        // 统计最近1小时的人工介入次数
        long recentCount = getManualInterventionCountLastHour();

        if (recentCount > MANUAL_INTERVENTION_THRESHOLD_PER_HOUR) {
            String alertKey = "manual_intervention_rate";

            // 避免重复告警（30分钟内只告警一次）
            if (shouldAlert(alertKey, 30)) {
                String message = String.format(
                        "Saga manual intervention count is %d in the last hour, exceeding threshold of %d",
                        recentCount, MANUAL_INTERVENTION_THRESHOLD_PER_HOUR
                );

                log.warn("ALERT: {}", message);
                sendAlert(AlertType.MANUAL_INTERVENTION_RATE, message);

                return Alert.builder()
                        .type(AlertType.MANUAL_INTERVENTION_RATE)
                        .message(message)
                        .severity(AlertSeverity.INFO)
                        .timestamp(LocalDateTime.now())
                        .build();
            }
        }

        return null;
    }

    /**
     * 记录人工介入（用于计数）
     */
    public void recordManualIntervention() {
        String hourKey = getCurrentHourKey();

        manualInterventionCounts.compute(hourKey, (k, v) -> {
            if (v == null) {
                return new AtomicLongWrapper(1L, LocalDateTime.now());
            }
            v.increment();
            return v;
        });
    }

    /**
     * 获取最近1小时的人工介入次数
     */
    private long getManualInterventionCountLastHour() {
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long count = 0;

        for (ConcurrentHashMap.Entry<String, AtomicLongWrapper> entry : manualInterventionCounts.entrySet()) {
            if (entry.getValue().getTimestamp().isAfter(oneHourAgo)) {
                count += entry.getValue().get();
            }
        }

        return count;
    }

    /**
     * 清理过期的计数器（超过2小时的）
     */
    private void cleanupOldCounters() {
        LocalDateTime twoHoursAgo = LocalDateTime.now().minusHours(2);

        manualInterventionCounts.entrySet().removeIf(entry ->
                entry.getValue().getTimestamp().isBefore(twoHoursAgo)
        );

        log.debug("Cleaned up old alert counters");
    }

    /**
     * 判断是否应该告警（避免重复告警）
     *
     * @param alertKey    告警键
     * @param intervalMin 告警间隔（分钟）
     * @return 是否应该告警
     */
    private boolean shouldAlert(String alertKey, int intervalMin) {
        LocalDateTime lastAlertTime = alertHistory.get(alertKey);

        if (lastAlertTime == null) {
            alertHistory.put(alertKey, LocalDateTime.now());
            return true;
        }

        long minutesSinceLastAlert = ChronoUnit.MINUTES.between(lastAlertTime, LocalDateTime.now());

        if (minutesSinceLastAlert >= intervalMin) {
            alertHistory.put(alertKey, LocalDateTime.now());
            return true;
        }

        return false;
    }

    /**
     * 发送告警
     * 实际实现中应该集成邮件、企业微信、钉钉等
     */
    private void sendAlert(AlertType type, String message) {
        // TODO: 实现实际的告警发送逻辑
        // - 发送邮件
        // - 发送企业微信消息
        // - 发送钉钉消息
        // - 写入告警日志表

        log.info("Alert sent: type={}, message={}", type, message);
    }

    /**
     * 获取当前小时的键（用于按小时统计）
     */
    private String getCurrentHourKey() {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%d-%02d-%02d-%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth(), now.getHour());
    }

    /**
     * 原子长整型包装器（附带时间戳）
     */
    private static class AtomicLongWrapper {
        private final AtomicLong value;
        private final LocalDateTime timestamp;

        public AtomicLongWrapper(long value, LocalDateTime timestamp) {
            this.value = new AtomicLong(value);
            this.timestamp = timestamp;
        }

        public long get() {
            return value.get();
        }

        public void increment() {
            value.incrementAndGet();
        }

        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }

    /**
     * 告警类型枚举
     */
    public enum AlertType {
        COMPENSATION_FAILURE_RATE,      // 补偿失败率过高
        MANUAL_INTERVENTION_RATE,       // 人工介入频率过高
        EXECUTION_TIMEOUT,              // 执行超时
        REDIS_UNAVAILABLE               // Redis 不可用
    }

    /**
     * 告警严重程度
     */
    public enum AlertSeverity {
        INFO,      // 信息
        WARNING,   // 警告
        ERROR,     // 错误
        CRITICAL   // 严重
    }

    /**
     * 告警数据类
     */
    @lombok.Builder
    @lombok.Data
    public static class Alert {
        private AlertType type;
        private String message;
        private AlertSeverity severity;
        private LocalDateTime timestamp;
    }
}
