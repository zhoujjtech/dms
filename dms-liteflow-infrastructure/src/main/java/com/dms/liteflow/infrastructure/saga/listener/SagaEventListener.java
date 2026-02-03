package com.dms.liteflow.infrastructure.saga.listener;

import com.dms.liteflow.application.saga.monitoring.SagaAlertService;
import com.dms.liteflow.application.saga.monitoring.SagaMonitoringService;
import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.service.SagaStateService;
import com.dms.liteflow.domain.saga.service.CompensationOrchestrator;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.slot.Slot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Saga 事件监听器
 * 监听 LiteFlow 的节点执行事件，记录状态、收集监控指标并触发补偿
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaEventListener {

    private final SagaStateService sagaStateService;
    private final CompensationOrchestrator compensationOrchestrator;
    private final SagaMonitoringService monitoringService;
    private final SagaAlertService alertService;
    private final ObjectMapper objectMapper;

    /**
     * 节点开始执行前
     */
    public void beforeNode(String component, DefaultContext context) {
        try {
            // 检查是否是 Saga 流程
            String executionId = getExecutionId(context);
            if (executionId == null) {
                return; // 不是 Saga 流程，跳过
            }

            StepId stepId = StepId.generate();
            Map<String, Object> inputData = extractInputData(context);

            log.info("Saga step started: executionId={}, stepId={}, component={}",
                    executionId, stepId.getValue(), component);

            sagaStateService.recordStepStart(
                    SagaExecutionId.of(executionId),
                    stepId,
                    component,
                    inputData
            );

            // 保存 stepId 到上下文，供后续使用
            context.setData("saga.stepId", stepId.getValue());

            // 收集监控指标：如果是第一个步骤，记录执行开始
            // 获取 chainName
            String chainName = context.getData("chainName");
            if (chainName == null) {
                // 尝试从其他来源获取 chainName
                Object chainObj = context.getData("chainId");
                chainName = chainObj != null ? chainObj.toString() : component;
            }

            // 检查是否是第一个步骤（通过检查是否已经记录过开始）
            Object startRecorded = context.getData("saga.startRecorded");
            if (startRecorded == null) {
                monitoringService.recordExecutionStart(executionId, chainName);
                context.setData("saga.startRecorded", true);
                context.setData("saga.chainName", chainName);
            }

        } catch (Exception e) {
            log.error("Failed to record step start for component: {}", component, e);
        }
    }

    /**
     * 节点成功执行后
     */
    public void afterSuccess(String component, DefaultContext context) {
        try {
            String executionId = getExecutionId(context);
            if (executionId == null) {
                return;
            }

            String stepId = context.getData("saga.stepId");
            if (stepId == null) {
                log.warn("StepId not found in context for component: {}", component);
                return;
            }

            Map<String, Object> outputData = extractOutputData(context);

            log.info("Saga step completed: executionId={}, stepId={}, component={}",
                    executionId, stepId, component);

            sagaStateService.recordStepSuccess(
                    SagaExecutionId.of(executionId),
                    StepId.of(stepId),
                    outputData
            );

            // 检查是否是 Saga 流程的最后一步
            // 通过检查 SagaExecution 状态判断
            SagaExecution sagaExecution = sagaStateService.getExecution(SagaExecutionId.of(executionId));
            if (sagaExecution != null && sagaExecution.getStatus() == SagaStatus.COMPLETED) {
                String chainName = context.getData("saga.chainName");
                if (chainName == null) {
                    chainName = sagaExecution.getChainName();
                }
                LocalDateTime startTime = sagaExecution.getStartedAt();

                // 记录执行成功指标
                monitoringService.recordExecutionSuccess(executionId, chainName, startTime);
                log.info("Saga execution completed successfully: executionId={}, chain={}", executionId, chainName);
            }

        } catch (Exception e) {
            log.error("Failed to record step success for component: {}", component, e);
        }
    }

    /**
     * 节点执行失败后
     */
    public void afterFailure(String component, Exception exception, DefaultContext context) {
        try {
            String executionId = getExecutionId(context);
            if (executionId == null) {
                return;
            }

            String stepId = context.getData("saga.stepId");
            if (stepId == null) {
                log.warn("StepId not found in context for component: {}", component);
                return;
            }

            String errorCode = extractErrorCode(exception);
            String errorMessage = exception.getMessage();
            String stackTrace = getStackTrace(exception);

            log.error("Saga step failed: executionId={}, stepId={}, component={}, errorCode={}",
                    executionId, stepId, component, errorCode, exception);

            // 记录失败
            sagaStateService.recordStepFailure(
                    SagaExecutionId.of(executionId),
                    StepId.of(stepId),
                    errorCode,
                    errorMessage,
                    stackTrace
            );

            // 收集监控指标 - 获取 SagaExecution 以获取 chainName 和 startTime
            SagaExecution sagaExecution = sagaStateService.getExecution(SagaExecutionId.of(executionId));
            String chainName = sagaExecution != null ? sagaExecution.getChainName() : component;
            LocalDateTime startTime = sagaExecution != null ? sagaExecution.getStartedAt() : null;

            // 记录执行失败指标
            monitoringService.recordExecutionFailure(executionId, chainName, startTime, errorMessage);

            // 触发补偿流程
            log.info("Triggering compensation for executionId={}", executionId);
            LocalDateTime compensationStartTime = LocalDateTime.now();

            // 记录补偿开始指标
            monitoringService.recordCompensationStart(executionId, chainName);

            boolean success = compensationOrchestrator.compensate(
                    SagaExecutionId.of(executionId)
            );

            if (success) {
                log.info("Compensation completed successfully for executionId={}", executionId);
                monitoringService.recordCompensationSuccess(executionId, chainName, compensationStartTime);
            } else {
                log.warn("Compensation partially failed for executionId={}", executionId);
                monitoringService.recordCompensationFailure(executionId, chainName, compensationStartTime, "Partial failure");
            }

            // 检查并触发告警（异步执行，避免影响主流程）
            try {
                new Thread(() -> {
                    try {
                        var alerts = alertService.checkAndTriggerAlerts();
                        if (!alerts.isEmpty()) {
                            log.info("Generated {} alerts for executionId={}", alerts.size(), executionId);
                        }
                    } catch (Exception e) {
                        log.error("Failed to check alerts", e);
                    }
                }).start();
            } catch (Exception e) {
                log.error("Failed to start alert checking thread", e);
            }

        } catch (Exception e) {
            log.error("Failed to handle step failure for component: {}", component, e);
        }
    }

    /**
     * 从上下文获取执行ID
     */
    private String getExecutionId(DefaultContext context) {
        Object executionId = context.getData("executionId");
        return executionId != null ? executionId.toString() : null;
    }

    /**
     * 提取输入数据
     */
    private Map<String, Object> extractInputData(DefaultContext context) {
        try {
            Map<String, Object> inputData = new HashMap<>();
            for (String key : context.getData().keySet()) {
                if (!key.startsWith("saga.")) { // 跳过 Saga 内部数据
                    inputData.put(key, context.getData(key));
                }
            }
            return inputData;
        } catch (Exception e) {
            log.warn("Failed to extract input data", e);
            return new HashMap<>();
        }
    }

    /**
     * 提取输出数据
     */
    private Map<String, Object> extractOutputData(DefaultContext context) {
        try {
            Map<String, Object> outputData = new HashMap<>();
            // 获取用户设置的输出数据
            Object output = context.getData("outputData");
            if (output != null) {
                if (output instanceof Map) {
                    outputData.putAll((Map<String, Object>) output);
                } else {
                    outputData.put("result", output);
                }
            }
            return outputData;
        } catch (Exception e) {
            log.warn("Failed to extract output data", e);
            return new HashMap<>();
        }
    }

    /**
     * 提取错误码
     */
    private String extractErrorCode(Exception exception) {
        // 尝试从异常中提取错误码
        String className = exception.getClass().getSimpleName();
        if (exception.getMessage() != null) {
            String message = exception.getMessage();
            // 检查是否包含错误码（如 "INSUFFICIENT_FUNDS"）
            if (message.matches(".*[A-Z_]{2,}.*")) {
                return message.toUpperCase();
            }
        }
        return className.toUpperCase();
    }

    /**
     * 获取异常堆栈
     */
    private String getStackTrace(Exception exception) {
        try {
            return objectMapper.writeValueAsString(exception);
        } catch (JsonProcessingException e) {
            return exception.toString();
        }
    }
}
