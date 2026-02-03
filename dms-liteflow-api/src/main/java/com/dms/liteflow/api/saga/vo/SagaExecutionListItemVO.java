package com.dms.liteflow.api.saga.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Saga 执行列表项 VO
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecutionListItemVO {

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
     * 从领域模型转换
     */
    public static SagaExecutionListItemVO fromDomain(com.dms.liteflow.domain.saga.aggregate.SagaExecution sagaExecution) {
        return SagaExecutionListItemVO.builder()
                .executionId(sagaExecution.getExecutionId().getValue())
                .chainName(sagaExecution.getChainName())
                .status(sagaExecution.getStatus().name())
                .currentStepIndex(sagaExecution.getCurrentStepIndex())
                .failureReason(sagaExecution.getFailureReason())
                .startTime(sagaExecution.getStartedAt())
                .endTime(sagaExecution.getCompletedAt())
                .durationMs(sagaExecution.getDurationMs())
                .build();
    }
}
