package com.dms.liteflow.infrastructure.saga.job;

import com.dms.liteflow.domain.saga.entity.CompensationLog;
import com.dms.liteflow.domain.saga.repository.CompensationLogRepository;
import com.dms.liteflow.domain.saga.service.CompensationOrchestrator;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
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
 * Saga 补偿重试 Handler
 * 重试失败的补偿操作
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCompensationRetryHandler {

    private final CompensationLogRepository compensationLogRepository;
    private final CompensationOrchestrator compensationOrchestrator;

    /**
     * 重试失败的补偿操作
     * 执行频率：每30分钟执行一次
     */
    @XxlJob("sagaCompensationRetryHandler")
    public void retryCompensation() {
        log.info("Starting Saga compensation retry job...");

        int retryCount = 0;
        int successCount = 0;
        int failedCount = 0;

        try {
            // 使用系统租户ID
            TenantId tenantId = TenantId.of(1L);
            TenantContext.setTenantId(tenantId);

            // 查询所有失败的补偿日志
            // 这里需要通过 CompensationLogRepository 的方法获取失败记录
            // 假设我们有按状态查询的方法
            List<CompensationLog> failedLogs = findFailedCompensationLogs(tenantId);

            log.info("Found {} failed compensation logs to retry", failedLogs.size());

            for (CompensationLog log : failedLogs) {
                try {
                    // 检查重试次数限制（最多3次）
                    if (log.getRetryCount() != null && log.getRetryCount() >= 3) {
                        log.debug("Skipping compensation log (max retries reached): {}", log.getId());
                        continue;
                    }

                    retryCount++;

                    // 执行重试
                    SagaExecutionId executionId = log.getExecutionId();

                    // 重新执行该步骤的补偿
                    boolean success = compensationOrchestrator.retryCompensation(
                            executionId,
                            log.getStepId()
                    );

                    if (success) {
                        successCount++;
                        log.info("Successfully retried compensation for execution: {}, step: {}",
                                executionId.getValue(), log.getStepId());
                    } else {
                        failedCount++;
                        log.warn("Failed to retry compensation for execution: {}, step: {}",
                                executionId.getValue(), log.getStepId());
                    }

                } catch (Exception e) {
                    failedCount++;
                    log.error("Error retrying compensation for log: {}", log.getId(), e);
                }
            }

            log.info("Saga compensation retry job completed. Retried: {}, Success: {}, Failed: {}",
                    retryCount, successCount, failedCount);
            XxlJobHelper.handleSuccess(String.format("Retried: %d, Success: %d, Failed: %d",
                    retryCount, successCount, failedCount));

        } catch (Exception e) {
            log.error("Saga compensation retry job failed", e);
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 查找失败的补偿日志
     */
    private List<CompensationLog> findFailedCompensationLogs(TenantId tenantId) {
        // 获取最近1小时内失败的补偿日志
        LocalDateTime since = LocalDateTime.now().minusHours(1);

        // 这里需要根据实际的 Repository 方法来查询
        // 暂时返回空列表，需要实现相应的查询方法
        return List.of();
    }
}
