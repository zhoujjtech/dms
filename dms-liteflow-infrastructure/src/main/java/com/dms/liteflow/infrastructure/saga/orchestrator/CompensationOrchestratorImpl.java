package com.dms.liteflow.infrastructure.saga.orchestrator;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.service.CompensationOrchestrator;
import com.dms.liteflow.domain.saga.service.SagaStateService;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.saga.valueobject.CompensationLog;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Saga 补偿编排器实现
 * 负责按相反顺序执行补偿操作
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompensationOrchestratorImpl implements CompensationOrchestrator {

    private final SagaStateService sagaStateService;
    private final FlowExecutor flowExecutor;

    @Override
    public boolean compensate(SagaExecutionId executionId) {
        log.info("Starting compensation for executionId={}", executionId.getValue());

        try {
            SagaExecution sagaExecution = sagaStateService.getExecution(executionId);
            if (sagaExecution == null) {
                log.error("SagaExecution not found: {}", executionId.getValue());
                return false;
            }

            // 标记开始补偿
            if (sagaExecution.getStatus() == SagaStatus.FAILED) {
                sagaExecution.startCompensating();
                sagaStateService.saveExecution(sagaExecution);
            }

            // 获取需要补偿的步骤列表（已按相反顺序）
            List<StepExecution> stepsToCompensate = sagaStateService.getExecutionStack(executionId);

            if (stepsToCompensate.isEmpty()) {
                log.info("No steps to compensate for executionId={}", executionId.getValue());
                sagaExecution.compensateComplete();
                sagaStateService.saveExecution(sagaExecution);
                return true;
            }

            boolean allSuccess = true;

            // 按相反顺序补偿
            for (StepExecution step : stepsToCompensate) {
                if (step.needsCompensation()) {
                    boolean success = compensateStep(executionId, step);
                    if (!success) {
                        allSuccess = false;
                        log.warn("Compensation failed for step: {}, continuing with next step", step.getStepId());
                    }
                }
            }

            // 标记补偿完成
            sagaExecution.compensateComplete();
            sagaStateService.saveExecution(sagaExecution);

            log.info("Compensation completed for executionId={}, allSuccess={}", executionId.getValue(), allSuccess);
            return allSuccess;

        } catch (Exception e) {
            log.error("Failed to compensate executionId={}", executionId.getValue(), e);
            return false;
        }
    }

    @Override
    public boolean compensateStep(SagaExecutionId executionId, StepExecution stepExecution) {
        log.info("Compensating step: executionId={}, stepId={}, component={}",
                executionId.getValue(),
                stepExecution.getStepId(),
                stepExecution.getComponentName());

        String compensateComponent = stepExecution.getCompensateComponent();
        if (compensComponent == null || compensateComponent.isEmpty()) {
            log.warn("No compensate component defined for step: {}", stepExecution.getStepId());
            return false;
        }

        try {
            // 创建补偿上下文
            DefaultContext context = new DefaultContext();

            // 将原始节点的输出数据传递给补偿组件
            if (stepExecution.getOutputData() != null) {
                context.setData("originalOutput", stepExecution.getOutputData());
            }

            // 设置执行ID
            context.setData("executionId", executionId.getValue());
            context.setData("compensating", true);
            context.setData("originalStepId", stepExecution.getStepId());

            // 执行补偿组件
            log.debug("Executing compensate component: {}", compensateComponent);
            flowExecutor.execute2Resp(compensateComponent, context);

            // 记录补偿成功
            stepExecution.compensate();
            sagaStateService.saveExecution(sagaStateService.getExecution(executionId));

            // 记录补偿日志
            CompensationLog logEntry = CompensationLog.success(
                    executionId.getValue(),
                    stepExecution.getStepId(),
                    compensateComponent
            );
            // TODO: 保存补偿日志到数据库

            log.info("Step compensation successful: stepId={}, compensateComponent={}",
                    stepExecution.getStepId(), compensateComponent);
            return true;

        } catch (Exception e) {
            log.error("Failed to compensate step: stepId={}, compensateComponent={}",
                    stepExecution.getStepId(), compensateComponent, e);

            // 记录补偿失败
            CompensationLog logEntry = CompensationLog.failure(
                    executionId.getValue(),
                    stepExecution.getStepId(),
                    compensateComponent,
                    e.getMessage()
            );
            // TODO: 保存补偿日志到数据库

            return false;
        }
    }

    @Override
    public boolean checkNeedsCompensation(StepExecution stepExecution) {
        return stepExecution.needsCompensation();
    }

    @Override
    public List<StepExecution> getCompensatableSteps(SagaExecutionId executionId) {
        return sagaStateService.getExecutionStack(executionId);
    }

    @Override
    public boolean manualCompensate(SagaExecutionId executionId, String operator) {
        log.info("Manual compensation triggered: executionId={}, operator={}",
                executionId.getValue(), operator);

        try {
            SagaExecution sagaExecution = sagaStateService.getExecution(executionId);
            if (sagaExecution == null) {
                log.error("SagaExecution not found: {}", executionId.getValue());
                return false;
            }

            // 标记开始补偿
            sagaExecution.startCompensating();
            sagaExecution.setFailureReason("Manual intervention: " + operator);
            sagaStateService.saveExecution(sagaExecution);

            // 执行补偿
            return compensate(executionId);

        } catch (Exception e) {
            log.error("Failed to manually compensate executionId={}", executionId.getValue(), e);
            return false;
        }
    }

    @Override
    public boolean retryCompensation(SagaExecutionId executionId, StepId stepId) {
        log.info("Retrying compensation: executionId={}, stepId={}",
                executionId.getValue(), stepId.getValue());

        try {
            SagaExecution sagaExecution = sagaStateService.getExecution(executionId);
            if (sagaExecution == null) {
                log.error("SagaExecution not found: {}", executionId.getValue());
                return false;
            }

            // 查找指定的步骤
            StepExecution stepToCompensate = null;
            for (StepExecution step : sagaExecution.getExecutionStack()) {
                if (step.getStepId().equals(stepId.getValue())) {
                    stepToCompensate = step;
                    break;
                }
            }

            if (stepToCompensate == null) {
                log.error("Step not found in execution: stepId={}", stepId.getValue());
                return false;
            }

            // 执行补偿
            boolean success = compensateStep(executionId, stepToCompensate);

            if (success) {
                log.info("Compensation retry successful: stepId={}", stepId.getValue());
            } else {
                log.warn("Compensation retry failed: stepId={}", stepId.getValue());
            }

            return success;

        } catch (Exception e) {
            log.error("Failed to retry compensation: executionId={}, stepId={}",
                    executionId.getValue(), stepId.getValue(), e);
            return false;
        }
    }
}
