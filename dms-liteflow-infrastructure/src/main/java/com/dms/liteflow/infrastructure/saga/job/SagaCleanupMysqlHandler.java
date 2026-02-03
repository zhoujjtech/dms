package com.dms.liteflow.infrastructure.saga.job;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.repository.SagaExecutionRepository;
import com.dms.liteflow.domain.saga.repository.StepExecutionRepository;
import com.dms.liteflow.domain.saga.repository.CompensationLogRepository;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga MySQL 数据归档 Handler
 * 归档 90 天前的数据到归档表（简化实现：直接删除）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCleanupMysqlHandler {

    private final SagaExecutionRepository sagaExecutionRepository;
    private final StepExecutionRepository stepExecutionRepository;
    private final CompensationLogRepository compensationLogRepository;

    /**
     * 归档/清理 MySQL 中的过期 Saga 数据
     * 执行频率：每天凌晨2点执行
     */
    @XxlJob("sagaCleanupMysqlHandler")
    public void cleanupMysql() {
        log.info("Starting Saga MySQL cleanup job...");

        int archivedCount = 0;
        int failedCount = 0;

        try {
            // 获取任务参数
            String param = XxlJobHelper.getJobParam();
            int daysThreshold = 90; // 默认90天
            if (param != null && !param.isEmpty()) {
                try {
                    daysThreshold = Integer.parseInt(param);
                } catch (NumberFormatException e) {
                    log.warn("Invalid job param: {}, using default: 90", param);
                }
            }

            LocalDateTime thresholdTime = LocalDateTime.now().minusDays(daysThreshold);

            // 使用系统租户ID
            TenantId tenantId = TenantId.of(1L);
            TenantContext.setTenantId(tenantId);

            // 查询需要归档的执行记录
            List<SagaExecution> expiredExecutions = findExpiredExecutions(tenantId, thresholdTime);

            log.info("Found {} expired executions to archive", expiredExecutions.size());

            for (SagaExecution execution : expiredExecutions) {
                try {
                    String executionId = execution.getExecutionId().getValue();

                    // 1. 删除补偿日志
                    compensationLogRepository.deleteByExecutionId(executionId);

                    // 2. 删除步骤执行记录（如果有单独的表）
                    // stepExecutionRepository.deleteByExecutionId(executionId);

                    // 3. 删除 Saga 执行记录
                    sagaExecutionRepository.delete(execution);

                    archivedCount++;

                    log.debug("Archived execution: {}", executionId);

                    // 批量提交，避免事务过大
                    if (archivedCount % 100 == 0) {
                        log.info("Archived {} executions so far...", archivedCount);
                    }

                } catch (Exception e) {
                    log.error("Failed to archive execution: {}",
                            execution.getExecutionId().getValue(), e);
                    failedCount++;
                }
            }

            log.info("Saga MySQL cleanup job completed. Archived: {}, Failed: {}", archivedCount, failedCount);
            XxlJobHelper.handleSuccess(String.format("Archived: %d, Failed: %d", archivedCount, failedCount));

        } catch (Exception e) {
            log.error("Saga MySQL cleanup job failed", e);
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 查找过期的执行记录
     */
    private List<SagaExecution> findExpiredExecutions(TenantId tenantId, LocalDateTime thresholdTime) {
        List<SagaExecution> result = sagaExecutionRepository.findByTenantId(tenantId);

        return result.stream()
                .filter(exec -> exec.getCompletedAt() != null
                        && exec.getCompletedAt().isBefore(thresholdTime))
                .toList();
    }
}
