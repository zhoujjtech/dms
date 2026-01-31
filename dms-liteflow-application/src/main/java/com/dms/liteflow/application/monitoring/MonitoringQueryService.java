package com.dms.liteflow.application.monitoring;

import com.dms.liteflow.domain.monitoring.aggregate.ExecutionRecord;
import com.dms.liteflow.domain.monitoring.repository.ExecutionRecordRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 监控数据查询服务
 * <p>
 * 提供监控数据的查询和聚合功能
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringQueryService {

    private final ExecutionRecordRepository executionRecordRepository;

    /**
     * 查询执行记录
     *
     * @param executionId 执行ID
     * @return 执行记录列表
     */
    public List<ExecutionRecord> getExecutionRecords(String executionId) {
        log.debug("Querying execution records for: {}", executionId);
        return executionRecordRepository.findByExecutionId(executionId);
    }

    /**
     * 查询流程链的执行记录
     *
     * @param tenantId  租户ID
     * @param chainId   流程链ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 执行记录列表
     */
    public List<ExecutionRecord> getChainExecutionRecords(
            Long tenantId,
            Long chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        log.debug("Querying chain execution records for tenant: {}, chain: {}", tenantId, chainId);
        return executionRecordRepository.findByChainIdAndTimeRange(
                ChainId.of(chainId),
                startTime,
                endTime
        );
    }

    /**
     * 查询租户的执行记录
     *
     * @param tenantId  租户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 执行记录列表
     */
    public List<ExecutionRecord> getTenantExecutionRecords(
            Long tenantId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        log.debug("Querying tenant execution records for tenant: {}", tenantId);
        return executionRecordRepository.findByTenantIdAndTimeRange(
                TenantId.of(tenantId),
                startTime,
                endTime
        );
    }

    /**
     * 统计流程执行次数
     *
     * @param tenantId  租户ID
     * @param chainId   流程链ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 执行次数
     */
    public long countExecutions(
            Long tenantId,
            Long chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        log.debug("Counting executions for tenant: {}, chain: {}", tenantId, chainId);
        return executionRecordRepository.countExecutions(
                TenantId.of(tenantId),
                ChainId.of(chainId),
                startTime,
                endTime
        );
    }

    /**
     * 计算执行成功率
     *
     * @param tenantId  租户ID
     * @param chainId   流程链ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 成功率（0-100）
     */
    public double calculateSuccessRate(
            Long tenantId,
            Long chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        log.debug("Calculating success rate for tenant: {}, chain: {}", tenantId, chainId);

        List<ExecutionRecord> records = executionRecordRepository.findByChainIdAndTimeRange(
                ChainId.of(chainId),
                startTime,
                endTime
        );

        if (records.isEmpty()) {
            return 0.0;
        }

        long successCount = records.stream()
                .filter(ExecutionRecord::isSuccess)
                .count();

        return (double) successCount / records.size() * 100;
    }

    /**
     * 计算平均执行时间
     *
     * @param tenantId  租户ID
     * @param chainId   流程链ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 平均执行时间（毫秒）
     */
    public double calculateAverageExecuteTime(
            Long tenantId,
            Long chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        log.debug("Calculating average execute time for tenant: {}, chain: {}", tenantId, chainId);

        List<ExecutionRecord> records = executionRecordRepository.findByChainIdAndTimeRange(
                ChainId.of(chainId),
                startTime,
                endTime
        );

        if (records.isEmpty()) {
            return 0.0;
        }

        return records.stream()
                .mapToLong(ExecutionRecord::getExecuteTime)
                .average()
                .orElse(0.0);
    }

    /**
     * 删除过期记录
     *
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    public int deleteExpiredRecords(LocalDateTime beforeTime) {
        log.info("Deleting execution records before: {}", beforeTime);
        return executionRecordRepository.deleteRecordsBefore(beforeTime);
    }

    /**
     * 获取执行统计信息
     *
     * @param tenantId  租户ID
     * @param chainId   流程链ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 统计信息
     */
    public ExecutionStats getExecutionStats(
            Long tenantId,
            Long chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        List<ExecutionRecord> records = executionRecordRepository.findByChainIdAndTimeRange(
                ChainId.of(chainId),
                startTime,
                endTime
        );

        ExecutionStats stats = new ExecutionStats();
        stats.setTotalExecutions(records.size());
        stats.setSuccessExecutions((int) records.stream().filter(ExecutionRecord::isSuccess).count());
        stats.setFailureExecutions(stats.getTotalExecutions() - stats.getSuccessExecutions());
        stats.setSuccessRate(stats.getTotalExecutions() > 0 ?
                (double) stats.getSuccessExecutions() / stats.getTotalExecutions() * 100 : 0.0);
        stats.setAverageExecuteTime(records.stream()
                .mapToLong(ExecutionRecord::getExecuteTime)
                .average()
                .orElse(0.0));

        return stats;
    }

    /**
     * 执行统计信息
     */
    @lombok.Data
    public static class ExecutionStats {
        private int totalExecutions;
        private int successExecutions;
        private int failureExecutions;
        private double successRate;
        private double averageExecuteTime;
    }
}
