package com.dms.liteflow.infrastructure.saga.state;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.repository.SagaExecutionRepository;
import com.dms.liteflow.domain.saga.service.SagaStateService;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.dms.liteflow.infrastructure.saga.redis.SagaDistributedLock;
import com.dms.liteflow.infrastructure.saga.redis.SagaRedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Saga 状态管理服务实现
 * 提供 Redis + MySQL 混合存储的状态管理
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaStateServiceImpl implements SagaStateService {

    private final SagaExecutionRepository sagaExecutionRepository;
    private final SagaRedisService sagaRedisService;
    private final SagaDistributedLock distributedLock;

    private static final long LOCK_WAIT_TIME = 10L;
    private static final long LOCK_LEASE_TIME = 30L;

    @Override
    public void recordStepStart(SagaExecutionId executionId, StepId stepId, String componentName, Map<String, Object> inputData) {
        String lockKey = executionId.getValue();
        boolean locked = distributedLock.tryLock(lockKey, LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

        try {
            TenantId tenantId = TenantContext.getTenantId();
            if (tenantId == null) {
                throw new IllegalStateException("TenantId not found in context");
            }

            // 获取或创建 SagaExecution
            SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(executionId)
                    .orElseGet(() -> {
                        SagaExecution newExecution = SagaExecution.create(tenantId, componentName, inputData);
                        return sagaExecutionRepository.save(newExecution);
                    });

            // 创建步骤执行记录
            StepExecution stepExecution = StepExecution.create(
                    executionId.getValue(),
                    stepId,
                    componentName,
                    inputData,
                    null, // compensateComponent 稍后设置
                    false  // needsCompensation 稍后设置
            );
            stepExecution.start();

            // 保存到数据库（同步，确保不丢失）
            sagaExecution.addStep(stepExecution);
            sagaExecutionRepository.save(sagaExecution);

            // 异步保存到 Redis
            saveToRedisAsync(tenantId.getValue(), executionId.getValue(), sagaExecution);

            log.debug("Recorded step start: executionId={}, stepId={}, component={}",
                    executionId.getValue(), stepId.getValue(), componentName);

        } catch (Exception e) {
            log.error("Failed to record step start: executionId={}, stepId={}", executionId.getValue(), stepId.getValue(), e);
            throw new RuntimeException("Failed to record step start", e);
        } finally {
            if (locked) {
                distributedLock.unlock(lockKey);
            }
        }
    }

    @Override
    public void recordStepSuccess(SagaExecutionId executionId, StepId stepId, Map<String, Object> outputData) {
        String lockKey = executionId.getValue();
        boolean locked = distributedLock.tryLock(lockKey, LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

        try {
            TenantId tenantId = TenantContext.getTenantId();

            // 获取 SagaExecution
            SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(executionId)
                    .orElseThrow(() -> new RuntimeException("SagaExecution not found: " + executionId.getValue()));

            // 更新步骤状态
            StepExecution step = sagaExecution.getStep(stepId);
            if (step == null) {
                log.warn("Step not found: stepId={}", stepId.getValue());
                return;
            }

            step.complete(outputData);
            sagaExecutionRepository.save(sagaExecution);

            // 如果需要补偿，加入执行栈
            if (step.needsCompensation()) {
                sagaExecution.pushToStack(step);
                sagaExecutionRepository.save(sagaExecution);
            }

            // 异步更新 Redis
            saveToRedisAsync(tenantId.getValue(), executionId.getValue(), sagaExecution);

            log.debug("Recorded step success: executionId={}, stepId={}", executionId.getValue(), stepId.getValue());

        } catch (Exception e) {
            log.error("Failed to record step success: executionId={}, stepId={}", executionId.getValue(), stepId.getValue(), e);
            throw new RuntimeException("Failed to record step success", e);
        } finally {
            if (locked) {
                distributedLock.unlock(lockKey);
            }
        }
    }

    @Override
    public void recordStepFailure(SagaExecutionId executionId, StepId stepId, String errorCode, String errorMessage, String stackTrace) {
        String lockKey = executionId.getValue();
        boolean locked = distributedLock.tryLock(lockKey, LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

        try {
            TenantId tenantId = TenantContext.getTenantId();

            // 获取 SagaExecution
            SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(executionId)
                    .orElseThrow(() -> new RuntimeException("SagaExecution not found: " + executionId.getValue()));

            // 更新步骤状态
            StepExecution step = sagaExecution.getStep(stepId);
            if (step == null) {
                log.warn("Step not found: stepId={}", stepId.getValue());
                return;
            }

            step.fail(errorCode, errorMessage, stackTrace);
            sagaExecution.fail(errorMessage);

            // 同步保存到数据库（失败数据必须持久化）
            sagaExecutionRepository.save(sagaExecution);

            // 同步更新 Redis（失败信息需要实时）
            sagaRedisService.saveExecution(tenantId.getValue(), executionId.getValue(), sagaExecution);

            log.error("Recorded step failure: executionId={}, stepId={}, errorCode={}",
                    executionId.getValue(), stepId.getValue(), errorCode);

        } catch (Exception e) {
            log.error("Failed to record step failure: executionId={}, stepId={}", executionId.getValue(), stepId.getValue(), e);
            throw new RuntimeException("Failed to record step failure", e);
        } finally {
            if (locked) {
                distributedLock.unlock(lockKey);
            }
        }
    }

    @Override
    public List<StepExecution> getExecutionStack(SagaExecutionId executionId) {
        try {
            TenantId tenantId = TenantContext.getTenantId();

            // 先从 Redis 读取
            List<StepExecution> stack = sagaRedisService.getExecutionStack(tenantId.getValue(), executionId.getValue());

            if (stack == null || stack.isEmpty()) {
                // Redis 未命中，从数据库加载
                SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(executionId)
                        .orElse(null);
                if (sagaExecution != null) {
                    stack = sagaExecution.getExecutionStack();
                    // 回写 Redis
                    if (stack != null && !stack.isEmpty()) {
                        sagaRedisService.saveExecutionStack(tenantId.getValue(), executionId.getValue(), stack);
                    }
                }
            }

            return stack != null ? stack : new ArrayList<>();
        } catch (Exception e) {
            log.error("Failed to get execution stack: executionId={}", executionId.getValue(), e);
            return new ArrayList<>();
        }
    }

    @Override
    public boolean updateStatus(SagaExecutionId executionId, SagaStatus currentStatus, SagaStatus newStatus) {
        String lockKey = executionId.getValue();
        boolean locked = distributedLock.tryLock(lockKey, LOCK_WAIT_TIME, LOCK_LEASE_TIME, TimeUnit.SECONDS);

        try {
            boolean updated = sagaExecutionRepository.updateStatus(executionId, currentStatus, newStatus, null);

            if (updated) {
                // 更新 Redis
                TenantId tenantId = TenantContext.getTenantId();
                SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(executionId).orElse(null);
                if (sagaExecution != null) {
                    sagaRedisService.saveExecution(tenantId.getValue(), executionId.getValue(), sagaExecution);
                }
            }

            return updated;
        } finally {
            if (locked) {
                distributedLock.unlock(lockKey);
            }
        }
    }

    @Override
    public SagaExecution getExecution(SagaExecutionId executionId) {
        try {
            TenantId tenantId = TenantContext.getTenantId();

            // 先从 Redis 读取
            SagaExecution sagaExecution = sagaRedisService.getExecution(tenantId.getValue(), executionId.getValue());

            if (sagaExecution == null) {
                // Redis 未命中，从数据库加载
                sagaExecution = sagaExecutionRepository.findByExecutionId(executionId).orElse(null);
                if (sagaExecution != null) {
                    // 回写 Redis
                    sagaRedisService.saveExecution(tenantId.getValue(), executionId.getValue(), sagaExecution);
                }
            }

            return sagaExecution;
        } catch (Exception e) {
            log.error("Failed to get execution: executionId={}", executionId.getValue(), e);
            return null;
        }
    }

    @Override
    public SagaExecution saveExecution(SagaExecution sagaExecution) {
        try {
            // 先保存到数据库
            SagaExecution saved = sagaExecutionRepository.save(sagaExecution);

            // 异步更新 Redis
            TenantId tenantId = sagaExecution.getTenantId();
            saveToRedisAsync(tenantId.getValue(), sagaExecution.getExecutionId().getValue(), saved);

            return saved;
        } catch (Exception e) {
            log.error("Failed to save execution: executionId={}", sagaExecution.getExecutionId().getValue(), e);
            throw new RuntimeException("Failed to save execution", e);
        }
    }

    @Override
    public void deleteExecution(SagaExecutionId executionId) {
        try {
            // 从数据库删除
            SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(executionId).orElse(null);
            if (sagaExecution != null) {
                sagaExecutionRepository.delete(sagaExecution);
            }

            // 从 Redis 删除
            TenantId tenantId = TenantContext.getTenantId();
            sagaRedisService.deleteExecution(tenantId.getValue(), executionId.getValue());
            sagaRedisService.deleteExecutionStack(tenantId.getValue(), executionId.getValue());

        } catch (Exception e) {
            log.error("Failed to delete execution: executionId={}", executionId.getValue(), e);
            throw new RuntimeException("Failed to delete execution", e);
        }
    }

    @Override
    public void pushToStack(SagaExecutionId executionId, StepExecution stepExecution) {
        try {
            TenantId tenantId = TenantContext.getTenantId();
            SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(executionId)
                    .orElseThrow(() -> new RuntimeException("SagaExecution not found: " + executionId.getValue()));

            sagaExecution.pushToStack(stepExecution);
            sagaExecutionRepository.save(sagaExecution);

            // 更新 Redis
            sagaRedisService.saveExecutionStack(tenantId.getValue(), executionId.getValue(),
                    sagaExecution.getExecutionStack());

        } catch (Exception e) {
            log.error("Failed to push to stack: executionId={}, stepId={}",
                    executionId.getValue(), stepExecution.getStepId().getValue(), e);
            throw new RuntimeException("Failed to push to stack", e);
        }
    }

    @Override
    public StepExecution popFromStack(SagaExecutionId executionId) {
        try {
            TenantId tenantId = TenantContext.getTenantId();
            SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(executionId)
                    .orElseThrow(() -> new RuntimeException("SagaExecution not found: " + executionId.getValue()));

            StepExecution step = sagaExecution.popFromStack();
            if (step != null) {
                sagaExecutionRepository.save(sagaExecution);
                // 更新 Redis
                sagaRedisService.saveExecutionStack(tenantId.getValue(), executionId.getValue(),
                        sagaExecution.getExecutionStack());
            }

            return step;
        } catch (Exception e) {
            log.error("Failed to pop from stack: executionId={}", executionId.getValue(), e);
            throw new RuntimeException("Failed to pop from stack", e);
        }
    }

    /**
     * 异步保存到 Redis
     */
    @Async
    protected void saveToRedisAsync(Long tenantId, String executionId, SagaExecution sagaExecution) {
        try {
            sagaRedisService.saveExecution(tenantId, executionId, sagaExecution);
        } catch (Exception e) {
            log.warn("Failed to save to Redis asynchronously: executionId={}", executionId, e);
            // Redis 写入失败不影响主流程
        }
    }
}
