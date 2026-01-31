package com.dms.liteflow.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行统计 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionStatsVO {
    private Long totalExecutions;
    private Long successExecutions;
    private Long failureExecutions;
    private Double successRate;
    private Double averageExecuteTime;
}
