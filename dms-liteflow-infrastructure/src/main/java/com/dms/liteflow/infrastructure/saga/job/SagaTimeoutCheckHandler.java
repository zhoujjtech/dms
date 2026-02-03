package com.dms.liteflow.infrastructure.saga.job;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.repository.SagaExecutionRepository;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.dms.liteflow.infrastructure.saga.redis.SagaRedisService;
import com.dms.liteflow.infrastructure.saga.state.SagaStateServiceImpl;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Saga 超时检查 Handler
 * 检查并标记超时的 Saga 执行
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaTimeoutCheckHandler {

    private final SagaExecutionRepository sagaExecutionRepository;
    private final SagaStateServiceImpl sagaStateService;
    private final SagaRedisService sagaRedisService;

    /**
     * 检查并标记超时的 Saga
     * 执行频率：每5分钟执行一次
     */
    @XxlJob("sagaTimeoutCheckHandler")
    public void checkTimeout() {
        log.info("Starting Saga timeout check job...");

        int timeoutCount = 0;
        int failedCount = 0;

        try {
            // 使用系统租户ID
            TenantId tenantId = TenantId.of(1L);
            TenantContext.setTenantId(tenantId);

            // 查询所有运行中的 Saga 执行
            List<SagaExecution> runningExecutions = sagaExecutionRepository.findByTenantIdAndStatus(
                    tenantId,
                    SagaStatus.RUNNING
            );

            // 也查询补偿中的执行
            runningExecutions.addAll(
                    sagaExecutionRepository.findByTenantIdAndStatus(tenantId, SagaStatus.COMPENSATING)
            );

            log.info("Found {} running/compensating executions to check", runningExecutions.size());

            // 默认超时时间：30分钟
            long defaultTimeoutMinutes = 30;

            for (SagaExecution execution : runningExecutions) {
                try {
                    if (isExecutionTimeout(execution, defaultTimeoutMinutes)) {
                        // 标记为超时失败
                        execution.fail("Execution timeout");
                        sagaStateService.saveExecution(execution);

                        // 从 Redis 清理
                        sagaRedisService.deleteExecution(execution.getExecutionId());

                        timeoutCount++;

                        log.warn("Marked execution as timeout: {}, started at: {}",
                                execution.getExecutionId().getValue(),
                                execution.getStartedAt());
                    }

                } catch (Exception e) {
                    failedCount++;
                    log.error("Failed to check timeout for execution: {}",
                            execution.getExecutionId().getValue(), e);
                }
            }

            log.info("Saga timeout check job completed. Timeout: {}, Failed: {}", timeoutCount, failedCount);
            XxlJobHelper.handleSuccess(String.format("Timeout: %d, Failed: %d", timeoutCount, failedCount));

        } catch (Exception e) {
            log.error("Saga timeout check job failed", e);
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 检查执行是否超时
     */
    private boolean isExecutionTimeout(SagaExecution execution, long timeoutMinutes) {
        if (execution.getStartedAt() == null) {
            // 没有开始时间，认为是异常数据，标记为超时
            return true;
        }

        long minutesElapsed = ChronoUnit.MINUTES.between(
                execution.getStartedAt(),
                LocalDateTime.now()
        );

        return minutesElapsed > timeoutMinutes;
    }
}
