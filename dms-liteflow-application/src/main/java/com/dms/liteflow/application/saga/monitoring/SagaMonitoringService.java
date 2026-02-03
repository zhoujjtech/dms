package com.dms.liteflow.application.saga.monitoring;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Saga 监控指标收集服务
 * 收集 Saga 执行和补偿的监控指标
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaMonitoringService {

    // 指标计数器（线程安全）
    private final AtomicLong sagaExecutionTotal = new AtomicLong(0);
    private final AtomicLong sagaExecutionSuccessTotal = new AtomicLong(0);
    private final AtomicLong sagaExecutionFailedTotal = new AtomicLong(0);
    private final AtomicLong sagaCompensationTotal = new AtomicLong(0);
    private final AtomicLong sagaCompensationSuccessTotal = new AtomicLong(0);
    private final AtomicLong sagaCompensationFailedTotal = new AtomicLong(0);
    private final AtomicLong sagaManualInterventionTotal = new AtomicLong(0);
    private final AtomicLong sagaRetryTotal = new AtomicLong(0);
    private final AtomicLong sagaSkipTotal = new AtomicLong(0);

    // 执行时长统计（用于计算平均值和P99等）
    private final ConcurrentHashMap<String, Long> executionDurations = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> compensationDurations = new ConcurrentHashMap<>();

    // 按流程链统计
    private final ConcurrentHashMap<String, AtomicLong> executionByChain = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, AtomicLong> failureByChain = new ConcurrentHashMap<>();

    /**
     * 记录 Saga 执行开始
     */
    public void recordExecutionStart(String executionId, String chainName) {
        sagaExecutionTotal.incrementAndGet();
        incrementChainCounter(executionByChain, chainName);

        log.debug("Saga execution started: executionId={}, chain={}", executionId, chainName);
    }

    /**
     * 记录 Saga 执行成功
     */
    public void recordExecutionSuccess(String executionId, String chainName, LocalDateTime startTime) {
        sagaExecutionSuccessTotal.incrementAndGet();

        // 记录执行时长
        if (startTime != null) {
            long duration = Duration.between(startTime, LocalDateTime.now()).toMillis();
            executionDurations.put(executionId, duration);
        }

        log.debug("Saga execution succeeded: executionId={}, chain={}", executionId, chainName);
    }

    /**
     * 记录 Saga 执行失败
     */
    public void recordExecutionFailure(String executionId, String chainName, LocalDateTime startTime, String reason) {
        sagaExecutionFailedTotal.incrementAndGet();
        incrementChainCounter(failureByChain, chainName);

        // 记录执行时长
        if (startTime != null) {
            long duration = Duration.between(startTime, LocalDateTime.now()).toMillis();
            executionDurations.put(executionId, duration);
        }

        log.debug("Saga execution failed: executionId={}, chain={}, reason={}", executionId, chainName, reason);
    }

    /**
     * 记录 Saga 补偿开始
     */
    public void recordCompensationStart(String executionId, String chainName) {
        sagaCompensationTotal.incrementAndGet();

        log.debug("Saga compensation started: executionId={}, chain={}", executionId, chainName);
    }

    /**
     * 记录 Saga 补偿成功
     */
    public void recordCompensationSuccess(String executionId, String chainName, LocalDateTime startTime) {
        sagaCompensationSuccessTotal.incrementAndGet();

        // 记录补偿时长
        if (startTime != null) {
            long duration = Duration.between(startTime, LocalDateTime.now()).toMillis();
            compensationDurations.put(executionId, duration);
        }

        log.debug("Saga compensation succeeded: executionId={}, chain={}", executionId, chainName);
    }

    /**
     * 记录 Saga 补偿失败
     */
    public void recordCompensationFailure(String executionId, String chainName, LocalDateTime startTime, String reason) {
        sagaCompensationFailedTotal.incrementAndGet();

        // 记录补偿时长
        if (startTime != null) {
            long duration = Duration.between(startTime, LocalDateTime.now()).toMillis();
            compensationDurations.put(executionId, duration);
        }

        log.debug("Saga compensation failed: executionId={}, chain={}, reason={}", executionId, chainName, reason);
    }

    /**
     * 记录人工介入
     */
    public void recordManualIntervention(String executionId, String chainName, String operator, String decision) {
        sagaManualInterventionTotal.incrementAndGet();

        log.info("Saga manual intervention: executionId={}, chain={}, operator={}, decision={}",
                executionId, chainName, operator, decision);
    }

    /**
     * 记录重试操作
     */
    public void recordRetry(String executionId, String chainName, String stepId) {
        sagaRetryTotal.incrementAndGet();

        log.debug("Saga retry: executionId={}, chain={}, stepId={}", executionId, chainName, stepId);
    }

    /**
     * 记录跳过操作
     */
    public void recordSkip(String executionId, String chainName, String stepId, String reason) {
        sagaSkipTotal.incrementAndGet();

        log.debug("Saga skip: executionId={}, chain={}, stepId={}, reason={}", executionId, chainName, stepId, reason);
    }

    /**
     * 获取所有指标
     */
    public SagaMetrics getMetrics() {
        return SagaMetrics.builder()
                .executionTotal(sagaExecutionTotal.get())
                .executionSuccessTotal(sagaExecutionSuccessTotal.get())
                .executionFailedTotal(sagaExecutionFailedTotal.get())
                .compensationTotal(sagaCompensationTotal.get())
                .compensationSuccessTotal(sagaCompensationSuccessTotal.get())
                .compensationFailedTotal(sagaCompensationFailedTotal.get())
                .manualInterventionTotal(sagaManualInterventionTotal.get())
                .retryTotal(sagaRetryTotal.get())
                .skipTotal(sagaSkipTotal.get())
                .executionByChain(new ConcurrentHashMap<>(executionByChain))
                .failureByChain(new ConcurrentHashMap<>(failureByChain))
                .build();
    }

    /**
     * 获取成功率
     */
    public double getSuccessRate() {
        long total = sagaExecutionTotal.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) sagaExecutionSuccessTotal.get() / total * 100.0;
    }

    /**
     * 获取补偿成功率
     */
    public double getCompensationSuccessRate() {
        long total = sagaCompensationTotal.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) sagaCompensationSuccessTotal.get() / total * 100.0;
    }

    /**
     * 获取平均执行时长（毫秒）
     */
    public long getAverageExecutionDuration() {
        if (executionDurations.isEmpty()) {
            return 0;
        }
        return executionDurations.values().stream()
                .mapToLong(Long::longValue)
                .sum() / executionDurations.size();
    }

    /**
     * 获取平均补偿时长（毫秒）
     */
    public long getAverageCompensationDuration() {
        if (compensationDurations.isEmpty()) {
            return 0;
        }
        return compensationDurations.values().stream()
                .mapToLong(Long::longValue)
                .sum() / compensationDurations.size();
    }

    /**
     * 重置指标
     */
    public void reset() {
        sagaExecutionTotal.set(0);
        sagaExecutionSuccessTotal.set(0);
        sagaExecutionFailedTotal.set(0);
        sagaCompensationTotal.set(0);
        sagaCompensationSuccessTotal.set(0);
        sagaCompensationFailedTotal.set(0);
        sagaManualInterventionTotal.set(0);
        sagaRetryTotal.set(0);
        sagaSkipTotal.set(0);
        executionDurations.clear();
        compensationDurations.clear();
        executionByChain.clear();
        failureByChain.clear();

        log.info("Saga monitoring metrics reset");
    }

    /**
     * 递增流程链计数器
     */
    private void incrementChainCounter(ConcurrentHashMap<String, AtomicLong> map, String chainName) {
        map.compute(chainName, (k, v) -> {
            if (v == null) {
                return new AtomicLong(1);
            }
            v.incrementAndGet();
            return v;
        });
    }

    /**
     * Saga 监控指标数据类
     */
    @lombok.Builder
    @lombok.Data
    public static class SagaMetrics {
        private long executionTotal;
        private long executionSuccessTotal;
        private long executionFailedTotal;
        private long compensationTotal;
        private long compensationSuccessTotal;
        private long compensationFailedTotal;
        private long manualInterventionTotal;
        private long retryTotal;
        private long skipTotal;
        private ConcurrentHashMap<String, AtomicLong> executionByChain;
        private ConcurrentHashMap<String, AtomicLong> failureByChain;
    }
}
