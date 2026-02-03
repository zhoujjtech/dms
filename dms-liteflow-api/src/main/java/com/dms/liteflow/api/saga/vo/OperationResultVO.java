package com.dms.liteflow.api.saga.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 操作结果 VO
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationResultVO {

    /**
     * 是否成功
     */
    private Boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 执行ID
     */
    private String executionId;

    /**
     * 步骤ID（可选）
     */
    private String stepId;

    /**
     * 操作时间
     */
    private String operatedAt;
}
