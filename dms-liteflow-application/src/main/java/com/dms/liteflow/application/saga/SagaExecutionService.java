package com.dms.liteflow.application.saga;

import com.dms.liteflow.domain.dto.ExecutionRequestDTO;
import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.repository.SagaExecutionRepository;
import com.dms.liteflow.domain.saga.service.CompensationOrchestrator;
import com.dms.liteflow.domain.saga.service.SagaStateService;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Saga 执行服务
 * 负责 Saga 流程的执行、补偿、重试等操作
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaExecutionService {

    private final FlowExecutor flowExecutor;
    private final SagaStateService sagaStateService;
    private final CompensationOrchestrator compensationOrchestrator;
    private final SagaExecutionRepository sagaExecutionRepository;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 执行 Saga 流程（同步）
     */
    @Transactional
    public SagaExecutionResponse executeSaga(SagaExecutionRequest request) {
        log.info("Executing Saga: chainName={}, tenantId={}, sagaMode={}",
                request.getChainName(), request.getTenantId(), request.getSagaMode());

        // 设置租户上下文
        TenantContext.setTenantId(TenantId.of(request.getTenantId()));

        SagaExecutionId executionId = SagaExecutionId.generate();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 创建 SagaExecution 实例
            SagaExecution sagaExecution = SagaExecution.create(
                    TenantId.of(request.getTenantId()),
                    request.getChainName(),
                    request.getInputData()
            );
            sagaExecution.start();

            // 保存初始状态
            sagaExecution = sagaStateService.saveExecution(sagaExecution);

            // 创建 LiteFlow 上下文
            DefaultContext context = new DefaultContext();
            context.setData("executionId", executionId.getValue());
            context.setData("sagaMode", request.getSagaMode());
            context.setData("compensating", false);

            // 设置输入数据
            if (request.getInputData() != null) {
                for (Map.Entry<String, Object> entry : request.getInputData().entrySet()) {
                    context.setData(entry.getKey(), entry.getValue());
                }
            }

            // 执行流程
            long start = System.currentTimeMillis();

            if (request.getTimeoutMs() != null && request.getTimeoutMs() > 0) {
                flowExecutor.execute2Resp(request.getChainName(),
                        request.getTimeoutMs(), TimeUnit.MILLISECONDS, context);
            } else {
                flowExecutor.execute2Resp(request.getChainName(), context);
            }

            long executeTime = System.currentTimeMillis() - start;
            LocalDateTime endTime = LocalDateTime.now();

            // 获取执行结果
            Object outputData = context.getData("outputData");

            // 更新最终状态
            sagaExecution.setOutputData(Map.of("result", outputData != null ? outputData : "completed"));
            sagaExecution.complete();
            sagaExecution = sagaStateService.saveExecution(sagaExecution);

            // 构建响应
            return SagaExecutionResponse.builder()
                    .executionId(executionId.getValue())
                    .chainName(request.getChainName())
                    .status("COMPLETED")
                    .outputData(outputData)
                    .executeTime(executeTime)
                    .startTime(startTime.format(FORMATTER))
                    .endTime(endTime.format(FORMATTER))
                    .build();

        } catch (Exception e) {
            log.error("Saga execution failed: executionId={}, chainName={}",
                    executionId.getValue(), request.getChainName(), e);

            LocalDateTime endTime = LocalDateTime.now();

            // 如果是 Saga 模式，补偿流程已经由 SagaEventListener 自动触发
            // 这里只需要记录最终状态
            try {
                SagaExecution sagaExecution = sagaStateService.getExecution(executionId);
                if (sagaExecution != null) {
                    sagaExecution.fail(e.getMessage());
                    sagaStateService.saveExecution(sagaExecution);
                }
            } catch (Exception ex) {
                log.error("Failed to update failure status: executionId={}", executionId.getValue(), ex);
            }

            // 构建失败响应
            return SagaExecutionResponse.builder()
                    .executionId(executionId.getValue())
                    .chainName(request.getChainName())
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .startTime(startTime.format(FORMATTER))
                    .endTime(endTime.format(FORMATTER))
                    .build();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 执行 Saga 流程（异步）
     */
    @Async
    public CompletableFuture<SagaExecutionResponse> executeSagaAsync(SagaExecutionRequest request) {
        log.info("Executing Saga async: chainName={}, tenantId={}",
                request.getChainName(), request.getTenantId());

        SagaExecutionId executionId = SagaExecutionId.generate();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 设置租户上下文
            TenantContext.setTenantId(TenantId.of(request.getTenantId()));

            // 创建 SagaExecution 实例
            SagaExecution sagaExecution = SagaExecution.create(
                    TenantId.of(request.getTenantId()),
                    request.getChainName(),
                    request.getInputData()
            );

            // 保存初始状态
            sagaExecution = sagaStateService.saveExecution(sagaExecution);

            // 立即返回初始响应
            SagaExecutionResponse initialResponse = SagaExecutionResponse.builder()
                    .executionId(executionId.getValue())
                    .chainName(request.getChainName())
                    .status("PENDING")
                    .startTime(startTime.format(FORMATTER))
                    .build();

            // 异步执行
            return CompletableFuture.supplyAsync(() -> {
                try {
                    // 创建 LiteFlow 上下文
                    DefaultContext context = new DefaultContext();
                    context.setData("executionId", executionId.getValue());
                    context.setData("sagaMode", request.getSagaMode());

                    // 设置输入数据
                    if (request.getInputData() != null) {
                        for (Map.Entry<String, Object> entry : request.getInputData().entrySet()) {
                            context.setData(entry.getKey(), entry.getValue());
                        }
                    }

                    // 执行流程
                    long start = System.currentTimeMillis();
                    flowExecutor.execute2Resp(request.getChainName(), context);
                    long executeTime = System.currentTimeMillis() - start;

                    LocalDateTime endTime = LocalDateTime.now();

                    // 获取执行结果
                    Object outputData = context.getData("outputData");

                    // 更新状态
                    sagaExecution.complete();
                    sagaExecution.setOutputData(Map.of("result", outputData));
                    sagaStateService.saveExecution(sagaExecution);

                    return SagaExecutionResponse.builder()
                            .executionId(executionId.getValue())
                            .chainName(request.getChainName())
                            .status("COMPLETED")
                            .outputData(outputData)
                            .executeTime(executeTime)
                            .startTime(startTime.format(FORMATTER))
                            .endTime(endTime.format(FORMATTER))
                            .build();

                } catch (Exception e) {
                    log.error("Async Saga execution failed: executionId={}", executionId.getValue(), e);

                    LocalDateTime endTime = LocalDateTime.now();

                    try {
                        sagaExecution.fail(e.getMessage());
                        sagaStateService.saveExecution(sagaExecution);
                    } catch (Exception ex) {
                        log.error("Failed to update failure status: executionId={}", executionId.getValue(), ex);
                    }

                    return SagaExecutionResponse.builder()
                            .executionId(executionId.getValue())
                            .chainName(request.getChainName())
                            .status("FAILED")
                            .errorMessage(e.getMessage())
                            .startTime(startTime.format(FORMATTER))
                            .endTime(endTime.format(FORMATTER))
                            .build();
                } finally {
                    TenantContext.clear();
                }
            });

        } catch (Exception e) {
            log.error("Failed to start async Saga execution: executionId={}", executionId.getValue(), e);

            return CompletableFuture.completedFuture(
                    SagaExecutionResponse.builder()
                            .executionId(executionId.getValue())
                            .chainName(request.getChainName())
                            .status("FAILED")
                            .errorMessage(e.getMessage())
                            .startTime(startTime.format(FORMATTER))
                            .build()
            );
        }
    }

    /**
     * 手动触发补偿
     */
    public boolean compensate(String executionId, String operator) {
        log.info("Manual compensation triggered: executionId={}, operator={}", executionId, operator);

        try {
            TenantId tenantId = TenantContext.getTenantId();
            TenantContext.setTenantId(tenantId);

            boolean success = compensationOrchestrator.manualCompensate(
                    SagaExecutionId.of(executionId),
                    operator
            );

            if (success) {
                log.info("Manual compensation completed: executionId={}", executionId);
            } else {
                log.warn("Manual compensation partially failed: executionId={}", executionId);
            }

            return success;

        } catch (Exception e) {
            log.error("Failed to compensate: executionId={}", executionId, e);
            return false;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 重试失败的步骤
     */
    public boolean retry(String executionId, String stepId, Map<String, Object> newInputData) {
        log.info("Retrying step: executionId={}, stepId={}", executionId, stepId);

        try {
            TenantId tenantId = TenantContext.getTenantId();
            TenantContext.setTenantId(tenantId);

            SagaExecution sagaExecution = sagaStateService.getExecution(SagaExecutionId.of(executionId));
            if (sagaExecution == null) {
                log.error("SagaExecution not found: {}", executionId);
                return false;
            }

            StepExecution step = sagaExecution.getStep(com.dms.liteflow.domain.saga.valueobject.StepId.of(stepId));
            if (step == null) {
                log.error("Step not found: {}", stepId);
                return false;
            }

            // 重置步骤状态
            step.start();
            if (newInputData != null) {
                step.setInputData(newInputData);
            }

            // 重新执行该步骤对应的组件
            DefaultContext context = new DefaultContext();
            context.setData("executionId", executionId);
            context.setData("sagaMode", true);
            context.setData("compensating", false);

            if (step.getInputData() != null) {
                for (Map.Entry<String, Object> entry : step.getInputData().entrySet()) {
                    context.setData(entry.getKey(), entry.getValue());
                }
            }

            // 执行组件
            flowExecutor.execute2Resp(step.getComponentName(), context);

            // 更新状态
            step.complete(context.getData("outputData"));
            sagaStateService.saveExecution(sagaExecution);

            log.info("Step retry completed: stepId={}", stepId);
            return true;

        } catch (Exception e) {
            log.error("Failed to retry step: executionId={}, stepId={}", executionId, stepId, e);
            return false;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 跳过失败步骤继续执行
     */
    public boolean skip(String executionId, String stepId, String reason) {
        log.info("Skipping step: executionId={}, stepId={}, reason={}", executionId, stepId, reason);

        try {
            TenantId tenantId = TenantContext.getTenantId();
            TenantContext.setTenantId(tenantId);

            SagaExecution sagaExecution = sagaStateService.getExecution(SagaExecutionId.of(executionId));
            if (sagaExecution == null) {
                log.error("SagaExecution not found: {}", executionId);
                return false;
            }

            StepExecution step = sagaExecution.getStep(com.dms.liteflow.domain.saga.valueobject.StepId.of(stepId));
            if (step == null) {
                log.error("Step not found: {}", stepId);
                return false;
            }

            // 标记为跳过
            step.skip();
            sagaStateService.saveExecution(sagaExecution);

            log.info("Step skipped: stepId={}", stepId);
            return true;

        } catch (Exception e) {
            log.error("Failed to skip step: executionId={}, stepId={}", executionId, stepId, e);
            return false;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 人工决策
     */
    public boolean manualDecision(String executionId, String decision, String reason, String operator) {
        log.info("Manual decision: executionId={}, decision={}, operator={}", executionId, decision, operator);

        try {
            TenantId tenantId = TenantContext.getTenantId();
            TenantContext.setTenantId(tenantId);

            SagaExecution sagaExecution = sagaStateService.getExecution(SagaExecutionId.of(executionId));
            if (sagaExecution == null) {
                log.error("SagaExecution not found: {}", executionId);
                return false;
            }

            // 根据决策执行相应操作
            switch (decision.toUpperCase()) {
                case "CONTINUE":
                    // 继续执行
                    sagaExecution.start();
                    sagaStateService.saveExecution(sagaExecution);
                    break;

                case "COMPENSATE":
                    // 执行补偿
                    return compensate(executionId, operator);

                case "RETRY":
                    // 重试当前步骤
                    // TODO: 实现重试逻辑
                    break;

                default:
                    log.error("Unknown decision: {}", decision);
                    return false;
            }

            log.info("Manual decision processed: executionId={}, decision={}", executionId, decision);
            return true;

        } catch (Exception e) {
            log.error("Failed to process manual decision: executionId={}", executionId, e);
            return false;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 查询执行状态
     */
    public SagaExecutionStatus getExecutionStatus(String executionId) {
        try {
            TenantId tenantId = TenantContext.getTenantId();

            SagaExecution sagaExecution = sagaStateService.getExecution(SagaExecutionId.of(executionId));

            if (sagaExecution == null) {
                return SagaExecutionStatus.builder()
                        .executionId(executionId)
                        .status("NOT_FOUND")
                        .build();
            }

            return SagaExecutionStatus.builder()
                    .executionId(executionId)
                    .chainName(sagaExecution.getChainName())
                    .status(sagaExecution.getStatus().name())
                    .currentStepIndex(sagaExecution.getCurrentStepIndex())
                    .failureReason(sagaExecution.getFailureReason())
                    .startTime(sagaExecution.getStartedAt() != null ?
                            sagaExecution.getStartedAt().format(FORMATTER) : null)
                    .endTime(sagaExecution.getCompletedAt() != null ?
                            sagaExecution.getCompletedAt().format(FORMATTER) : null)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get execution status: executionId={}", executionId, e);
            return SagaExecutionStatus.builder()
                    .executionId(executionId)
                    .status("ERROR")
                    .errorMessage(e.getMessage())
                    .build();
        }
    }
}
