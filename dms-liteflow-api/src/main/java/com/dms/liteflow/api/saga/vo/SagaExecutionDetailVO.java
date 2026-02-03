package com.dms.liteflow.api.saga.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 执行详情 VO
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecutionDetailVO {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 流程链名称
     */
    private String chainName;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 当前步骤索引
     */
    private Integer currentStepIndex;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 输入数据
     */
    private Object inputData;

    /**
     * 输出数据
     */
    private Object outputData;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 执行时长（毫秒）
     */
    private Long durationMs;

    /**
     * 执行栈（用于补偿）
     */
    private List<StepExecutionDetailVO> executionStack;

    /**
     * 所有步骤
     */
    private List<StepExecutionDetailVO> steps;

    /**
     * 补偿日志
     */
    private List<CompensationLogVO> compensationLogs;

    /**
     * 从领域模型转换
     */
    public static SagaExecutionDetailVO fromDomain(com.dms.liteflow.domain.saga.aggregate.SagaExecution sagaExecution) {
        List<StepExecutionDetailVO> steps = sagaExecution.getSteps().stream()
                .map(StepExecutionDetailVO::fromDomain)
                .collect(java.util.stream.Collectors.toList());

        List<StepExecutionDetailVO> executionStack = sagaExecution.getExecutionStack().stream()
                .map(StepExecutionDetailVO::fromDomain)
                .collect(java.util.stream.Collectors.toList());

        return SagaExecutionDetailVO.builder()
                .executionId(sagaExecution.getExecutionId().getValue())
                .chainName(sagaExecution.getChainName())
                .status(sagaExecution.getStatus().name())
                .currentStepIndex(sagaExecution.getCurrentStepIndex())
                .failureReason(sagaExecution.getFailureReason())
                .inputData(sagaExecution.getInputData())
                .outputData(sagaExecution.getOutputData())
                .startTime(sagaExecution.getStartedAt())
                .endTime(sagaExecution.getCompletedAt())
                .durationMs(sagaExecution.getDurationMs())
                .executionStack(executionStack)
                .steps(steps)
                .compensationLogs(new ArrayList<>())  // TODO: 加载补偿日志
                .build();
    }
}
