package com.dms.liteflow.api.saga.controller;

import com.dms.liteflow.application.saga.monitoring.SagaAlertService;
import com.dms.liteflow.application.saga.monitoring.SagaMonitoringService;
import com.dms.liteflow.application.saga.monitoring.SagaMonitoringService.SagaMetrics;
import com.dms.liteflow.application.saga.monitoring.SagaAlertService.Alert;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saga 监控 API 控制器
 * 提供 Saga 监控指标和告警查询
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@RestController
@RequestMapping("/api/saga/monitoring")
@RequiredArgsConstructor
public class SagaMonitoringController {

    private final SagaMonitoringService monitoringService;
    private final SagaAlertService alertService;

    /**
     * 获取所有监控指标
     *
     * GET /api/saga/monitoring/metrics
     *
     * @return 监控指标
     */
    @GetMapping("/metrics")
    public ResponseEntity<SagaMetrics> getMetrics() {
        log.info("Querying Saga monitoring metrics");

        SagaMetrics metrics = monitoringService.getMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * 获取指标摘要
     *
     * GET /api/saga/monitoring/summary
     *
     * @return 指标摘要
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        log.info("Querying Saga monitoring summary");

        Map<String, Object> summary = new HashMap<>();
        summary.put("successRate", monitoringService.getSuccessRate());
        summary.put("compensationSuccessRate", monitoringService.getCompensationSuccessRate());
        summary.put("averageExecutionDuration", monitoringService.getAverageExecutionDuration());
        summary.put("averageCompensationDuration", monitoringService.getAverageCompensationDuration());

        SagaMetrics metrics = monitoringService.getMetrics();
        summary.put("executionTotal", metrics.getExecutionTotal());
        summary.put("executionSuccessTotal", metrics.getExecutionSuccessTotal());
        summary.put("executionFailedTotal", metrics.getExecutionFailedTotal());
        summary.put("compensationTotal", metrics.getCompensationTotal());
        summary.put("manualInterventionTotal", metrics.getManualInterventionTotal());
        summary.put("retryTotal", metrics.getRetryTotal());
        summary.put("skipTotal", metrics.getSkipTotal());

        return ResponseEntity.ok(summary);
    }

    /**
     * 按流程链统计
     *
     * GET /api/saga/monitoring/by-chain
     *
     * @return 按流程链分组的统计
     */
    @GetMapping("/by-chain")
    public ResponseEntity<Map<String, Map<String, Long>>> getMetricsByChain() {
        log.info("Querying Saga metrics by chain");

        SagaMetrics metrics = monitoringService.getMetrics();

        Map<String, Map<String, Long>> byChain = new HashMap<>();

        // 执行次数按流程链
        Map<String, Long> executionCounts = new HashMap<>();
        metrics.getExecutionByChain().forEach((chain, counter) -> {
            executionCounts.put(chain, counter.get());
        });
        byChain.put("executions", executionCounts);

        // 失败次数按流程链
        Map<String, Long> failureCounts = new HashMap<>();
        metrics.getFailureByChain().forEach((chain, counter) -> {
            failureCounts.put(chain, counter.get());
        });
        byChain.put("failures", failureCounts);

        return ResponseEntity.ok(byChain);
    }

    /**
     * 手动检查告警
     *
     * POST /api/saga/monitoring/check-alerts
     *
     * @return 触发的告警列表
     */
    @PostMapping("/check-alerts")
    public ResponseEntity<List<Alert>> checkAlerts() {
        log.info("Manual trigger alert check");

        List<Alert> alerts = alertService.checkAndTriggerAlerts();

        log.info("Alert check completed: {} alerts triggered", alerts.size());
        return ResponseEntity.ok(alerts);
    }

    /**
     * 重置监控指标
     *
     * POST /api/saga/monitoring/reset
     *
     * @return 操作结果
     */
    @PostMapping("/reset")
    public ResponseEntity<Map<String, String>> resetMetrics() {
        log.info("Resetting Saga monitoring metrics");

        monitoringService.reset();

        Map<String, String> result = new HashMap<>();
        result.put("message", "Metrics reset successfully");
        result.put("timestamp", String.valueOf(System.currentTimeMillis()));

        return ResponseEntity.ok(result);
    }

    /**
     * 获取健康状态
     *
     * GET /api/saga/monitoring/health
     *
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getHealth() {
        log.info("Querying Saga health status");

        Map<String, Object> health = new HashMap<>();

        double successRate = monitoringService.getSuccessRate();
        double compensationRate = monitoringService.getCompensationSuccessRate();

        // 判断健康状态
        String status = "HEALTHY";
        if (successRate < 80.0) {
            status = "UNHEALTHY";
        } else if (successRate < 95.0) {
            status = "DEGRADED";
        }

        health.put("status", status);
        health.put("successRate", successRate);
        health.put("compensationSuccessRate", compensationRate);
        health.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(health);
    }
}
