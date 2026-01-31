package com.dms.liteflow.application.execution;

import com.dms.liteflow.domain.dto.ExecutionRequestDTO;
import com.dms.liteflow.domain.vo.ExecutionResponseVO;
import com.dms.liteflow.domain.vo.ExecutionStatusVO;
import com.dms.liteflow.domain.monitoring.aggregate.ExecutionRecord;
import com.dms.liteflow.domain.monitoring.repository.ExecutionRecordRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 流程执行服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExecutionService {

    private final FlowExecutor flowExecutor;
    private final ExecutionRecordRepository executionRecordRepository;
    private final ObjectMapper objectMapper;

    // 执行状态缓存
    private final Map<String, ExecutionState> executionStateCache = new ConcurrentHashMap<>();

    // 执行结果缓存
    private final Map<String, ExecutionResponseVO> executionResultCache = new ConcurrentHashMap<>();

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 同步执行流程
     */
    public ExecutionResponseVO executeSync(ExecutionRequestDTO request) {
        String executionId = generateExecutionId();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 设置租户上下文
            TenantContext.setTenantId(TenantId.of(request.getTenantId()));

            // 初始化执行状态
            ExecutionState state = new ExecutionState(executionId, "RUNNING", startTime);
            executionStateCache.put(executionId, state);

            // 创建 LiteFlow 上下文
            DefaultContext context = new DefaultContext();
            if (request.getInputData() != null) {
                try {
                    Map<String, Object> inputData = objectMapper.readValue(request.getInputData(), Map.class);
                    for (Map.Entry<String, Object> entry : inputData.entrySet()) {
                        context.setData(entry.getKey(), entry.getValue());
                    }
                } catch (JsonProcessingException e) {
                    log.warn("Failed to parse inputData as JSON, treating as plain text", e);
                    context.setData("inputData", request.getInputData());
                }
            }

            // 设置执行ID到上下文
            context.setData("executionId", executionId);

            // 执行流程
            Integer timeoutMs = request.getTimeoutMs() != null ? request.getTimeoutMs() : 30000;
            long start = System.currentTimeMillis();

            if (timeoutMs > 0) {
                flowExecutor.execute2Resp(request.getChainName(), timeoutMs, TimeUnit.MILLISECONDS, context);
            } else {
                flowExecutor.execute2Resp(request.getChainName(), context);
            }

            long executeTime = System.currentTimeMillis() - start;
            LocalDateTime endTime = LocalDateTime.now();

            // 获取执行结果
            Object outputData = context.getData("outputData");

            // 保存执行记录
            saveExecutionRecord(executionId, TenantId.of(request.getTenantId()),
                    ChainId.of(request.getTenantId()), request.getChainName(),
                    "SUCCESS", executeTime, null);

            // 更新执行状态
            state.setStatus("COMPLETED");
            state.setProgress(100);
            state.setEndTime(endTime);

            // 构建响应
            ExecutionResponseVO response = ExecutionResponseVO.builder()
                    .executionId(executionId)
                    .status("COMPLETED")
                    .outputData(outputData)
                    .executeTime(executeTime)
                    .startTime(startTime.format(FORMATTER))
                    .endTime(endTime.format(FORMATTER))
                    .build();

            executionResultCache.put(executionId, response);
            return response;

        } catch (Exception e) {
            log.error("Sync execution failed: executionId={}, chainName={}",
                    executionId, request.getChainName(), e);

            LocalDateTime endTime = LocalDateTime.now();
            long executeTime = System.currentTimeMillis() - System.currentTimeMillis();

            // 保存失败记录
            saveExecutionRecord(executionId, TenantId.of(request.getTenantId()),
                    ChainId.of(request.getTenantId()), request.getChainName(),
                    "FAILURE", executeTime, e.getMessage());

            // 更新执行状态
            ExecutionState state = executionStateCache.get(executionId);
            if (state != null) {
                state.setStatus("FAILED");
                state.setEndTime(endTime);
            }

            ExecutionResponseVO response = ExecutionResponseVO.builder()
                    .executionId(executionId)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .startTime(startTime.format(FORMATTER))
                    .endTime(endTime.format(FORMATTER))
                    .build();

            executionResultCache.put(executionId, response);
            return response;

        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 异步执行流程
     */
    @Async
    public CompletableFuture<ExecutionResponseVO> executeAsync(ExecutionRequestDTO request) {
        String executionId = generateExecutionId();
        LocalDateTime startTime = LocalDateTime.now();

        try {
            // 设置租户上下文
            TenantContext.setTenantId(TenantId.of(request.getTenantId()));

            // 初始化执行状态
            ExecutionState state = new ExecutionState(executionId, "PENDING", startTime);
            executionStateCache.put(executionId, state);

            // 立即返回执行ID
            ExecutionResponseVO initialResponse = ExecutionResponseVO.builder()
                    .executionId(executionId)
                    .status("PENDING")
                    .startTime(startTime.format(FORMATTER))
                    .build();

            executionResultCache.put(executionId, initialResponse);

            // 异步执行
            return CompletableFuture.supplyAsync(() -> {
                try {
                    state.setStatus("RUNNING");

                    // 创建 LiteFlow 上下文
                    DefaultContext context = new DefaultContext();
                    if (request.getInputData() != null) {
                        try {
                            Map<String, Object> inputData = objectMapper.readValue(request.getInputData(), Map.class);
                            for (Map.Entry<String, Object> entry : inputData.entrySet()) {
                                context.setData(entry.getKey(), entry.getValue());
                            }
                        } catch (JsonProcessingException e) {
                            context.setData("inputData", request.getInputData());
                        }
                    }

                    context.setData("executionId", executionId);

                    // 执行流程
                    long start = System.currentTimeMillis();
                    flowExecutor.execute2Resp(request.getChainName(), context);
                    long executeTime = System.currentTimeMillis() - start;

                    LocalDateTime endTime = LocalDateTime.now();

                    // 获取执行结果
                    Object outputData = context.getData("outputData");

                    // 保存执行记录
                    saveExecutionRecord(executionId, TenantId.of(request.getTenantId()),
                            ChainId.of(request.getTenantId()), request.getChainName(),
                            "SUCCESS", executeTime, null);

                    // 更新执行状态
                    state.setStatus("COMPLETED");
                    state.setProgress(100);
                    state.setEndTime(endTime);

                    ExecutionResponseVO response = ExecutionResponseVO.builder()
                            .executionId(executionId)
                            .status("COMPLETED")
                            .outputData(outputData)
                            .executeTime(executeTime)
                            .startTime(startTime.format(FORMATTER))
                            .endTime(endTime.format(FORMATTER))
                            .build();

                    executionResultCache.put(executionId, response);
                    return response;

                } catch (Exception e) {
                    log.error("Async execution failed: executionId={}", executionId, e);

                    LocalDateTime endTime = LocalDateTime.now();

                    // 保存失败记录
                    saveExecutionRecord(executionId, TenantId.of(request.getTenantId()),
                            ChainId.of(request.getTenantId()), request.getChainName(),
                            "FAILURE", 0L, e.getMessage());

                    // 更新执行状态
                    state.setStatus("FAILED");
                    state.setEndTime(endTime);

                    ExecutionResponseVO response = ExecutionResponseVO.builder()
                            .executionId(executionId)
                            .status("FAILED")
                            .errorMessage(e.getMessage())
                            .startTime(startTime.format(FORMATTER))
                            .endTime(endTime.format(FORMATTER))
                            .build();

                    executionResultCache.put(executionId, response);
                    return response;

                } finally {
                    TenantContext.clear();
                }
            });

        } catch (Exception e) {
            log.error("Failed to start async execution: executionId={}", executionId, e);

            ExecutionResponseVO response = ExecutionResponseVO.builder()
                    .executionId(executionId)
                    .status("FAILED")
                    .errorMessage(e.getMessage())
                    .startTime(startTime.format(FORMATTER))
                    .build();

            executionResultCache.put(executionId, response);
            return CompletableFuture.completedFuture(response);
        }
    }

    /**
     * 查询执行状态
     */
    public ExecutionStatusVO getExecutionStatus(String executionId) {
        ExecutionState state = executionStateCache.get(executionId);
        if (state == null) {
            return ExecutionStatusVO.builder()
                    .executionId(executionId)
                    .status("NOT_FOUND")
                    .build();
        }

        return ExecutionStatusVO.builder()
                .executionId(state.getExecutionId())
                .status(state.getStatus())
                .progress(state.getProgress())
                .currentStep(state.getCurrentStep())
                .completedSteps(state.getCompletedSteps())
                .totalSteps(state.getTotalSteps())
                .startTime(state.getStartTime() != null ? state.getStartTime().format(FORMATTER) : null)
                .estimatedRemainingTimeMs(state.getEstimatedRemainingTimeMs())
                .build();
    }

    /**
     * 查询执行结果
     */
    public ExecutionResponseVO getExecutionResult(String executionId) {
        ExecutionResponseVO result = executionResultCache.get(executionId);
        if (result == null) {
            return ExecutionResponseVO.builder()
                    .executionId(executionId)
                    .status("NOT_FOUND")
                    .errorMessage("Execution result not found or expired")
                    .build();
        }
        return result;
    }

    /**
     * 生成执行ID
     */
    private String generateExecutionId() {
        return "exec-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 保存执行记录
     */
    private void saveExecutionRecord(String executionId, TenantId tenantId, ChainId chainId,
            String chainName, String status, long executeTime, String errorMessage) {
        try {
            ExecutionRecord record = ExecutionRecord.builder()
                    .tenantId(tenantId)
                    .chainId(chainId)
                    .chainExecutionId(executionId)
                    .componentId(chainName)
                    .executeTime(executeTime)
                    .status(status)
                    .errorMessage(errorMessage)
                    .build();

            executionRecordRepository.save(record);
        } catch (Exception e) {
            log.error("Failed to save execution record: executionId={}", executionId, e);
        }
    }

    /**
     * 执行状态内部类
     */
    private static class ExecutionState {
        private final String executionId;
        private String status;
        private int progress;
        private String currentStep;
        private int completedSteps;
        private int totalSteps;
        private final LocalDateTime startTime;
        private LocalDateTime endTime;
        private long estimatedRemainingTimeMs;

        public ExecutionState(String executionId, String status, LocalDateTime startTime) {
            this.executionId = executionId;
            this.status = status;
            this.startTime = startTime;
            this.progress = 0;
        }

        public String getExecutionId() { return executionId; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getProgress() { return progress; }
        public void setProgress(int progress) { this.progress = progress; }
        public String getCurrentStep() { return currentStep; }
        public void setCurrentStep(String currentStep) { this.currentStep = currentStep; }
        public int getCompletedSteps() { return completedSteps; }
        public void setCompletedSteps(int completedSteps) { this.completedSteps = completedSteps; }
        public int getTotalSteps() { return totalSteps; }
        public void setTotalSteps(int totalSteps) { this.totalSteps = totalSteps; }
        public LocalDateTime getStartTime() { return startTime; }
        public LocalDateTime getEndTime() { return endTime; }
        public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
        public long getEstimatedRemainingTimeMs() { return estimatedRemainingTimeMs; }
        public void setEstimatedRemainingTimeMs(long estimatedRemainingTimeMs) {
            this.estimatedRemainingTimeMs = estimatedRemainingTimeMs;
        }
    }
}
