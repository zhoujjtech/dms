package com.dms.liteflow.api.saga.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行时间线 VO
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionTimelineVO {

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
     * 时间线节点
     */
    private List<TimelineNodeVO> nodes;

    /**
     * 从领域模型转换
     */
    public static ExecutionTimelineVO fromDomain(com.dms.liteflow.domain.saga.aggregate.SagaExecution sagaExecution) {
        // TODO: 构建时间线节点
        return ExecutionTimelineVO.builder()
                .executionId(sagaExecution.getExecutionId().getValue())
                .chainName(sagaExecution.getChainName())
                .status(sagaExecution.getStatus().name())
                .startTime(sagaExecution.getStartedAt())
                .endTime(sagaExecution.getCompletedAt())
                .durationMs(sagaExecution.getDurationMs())
                .nodes(new ArrayList<>())
                .build();
    }
}
