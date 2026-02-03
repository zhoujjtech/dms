package com.dms.liteflow.infrastructure.saga.job;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.repository.SagaExecutionRepository;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.dms.liteflow.infrastructure.saga.redis.SagaRedisService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga Redis 清理 Handler
 * 定期清理 Redis 中超过 24 小时的已完成 Saga 执行记录
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCleanupRedisHandler {

    private final SagaRedisService sagaRedisService;
    private final SagaExecutionRepository sagaExecutionRepository;

    /**
     * 清理 Redis 中的过期 Saga 数据
     * 执行频率：每小时执行一次
     */
    @XxlJob("sagaCleanupRedisHandler")
    public void cleanupRedis() {
        log.info("Starting Saga Redis cleanup job...");

        int cleanedCount = 0;
        int failedCount = 0;

        try {
            // 获取任务参数
            String param = XxlJobHelper.getJobParam();
            int hoursThreshold = 24; // 默认24小时
            if (param != null && !param.isEmpty()) {
                try {
                    hoursThreshold = Integer.parseInt(param);
                } catch (NumberFormatException e) {
                    log.warn("Invalid job param: {}, using default: 24", param);
                }
            }

            LocalDateTime thresholdTime = LocalDateTime.now().minusHours(hoursThreshold);

            // 查询所有租户（简化处理，实际应该遍历所有活跃租户）
            // 这里使用系统租户ID=1
            TenantId tenantId = TenantId.of(1L);
            TenantContext.setTenantId(tenantId);

            // 查询已完成的 Saga 执行记录
            List<SagaExecution> completedExecutions = sagaExecutionRepository.findByTenantIdAndStatus(
                    tenantId,
                    SagaStatus.COMPLETED
            );

            completedExecutions.addAll(
                    sagaExecutionRepository.findByTenantIdAndStatus(tenantId, SagaStatus.COMPENSATED)
            );

            completedExecutions.addAll(
                    sagaExecutionRepository.findByTenantIdAndStatus(tenantId, SagaStatus.FAILED)
            );

            // 清理超过阈值时间的记录
            for (SagaExecution execution : completedExecutions) {
                if (execution.getCompletedAt() != null
                        && execution.getCompletedAt().isBefore(thresholdTime)) {

                    try {
                        // 从 Redis 删除
                        sagaRedisService.deleteExecution(execution.getExecutionId());
                        cleanedCount++;

                        log.debug("Cleaned up Redis data for execution: {}",
                                execution.getExecutionId().getValue());

                    } catch (Exception e) {
                        log.error("Failed to cleanup Redis for execution: {}",
                                execution.getExecutionId().getValue(), e);
                        failedCount++;
                    }
                }
            }

            log.info("Saga Redis cleanup job completed. Cleaned: {}, Failed: {}", cleanedCount, failedCount);
            XxlJobHelper.handleSuccess(String.format("Cleaned: %d, Failed: %d", cleanedCount, failedCount));

        } catch (Exception e) {
            log.error("Saga Redis cleanup job failed", e);
            XxlJobHelper.handleFail(e.getMessage());
        } finally {
            TenantContext.clear();
        }
    }
}
