package com.dms.liteflow.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程执行响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResponseVO {
    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 执行状态
     */
    private String status;

    /**
     * 输出数据
     */
    private Object outputData;

    /**
     * 执行耗时（毫秒）
     */
    private Long executeTime;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行开始时间
     */
    private String startTime;

    /**
     * 执行结束时间
     */
    private String endTime;
}
