package com.dms.liteflow.api.saga.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 补偿日志 VO
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationLogVO {

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 步骤ID
     */
    private String stepId;

    /**
     * 补偿组件名称
     */
    private String compensateComponent;

    /**
     * 补偿状态
     */
    private String status;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 补偿时间
     */
    private String compensatedAt;

    /**
     * 操作人
     */
    private String operator;

    /**
     * 操作类型
     */
    private String operationType;
}
