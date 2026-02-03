package com.dms.liteflow.api.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

/**
 * 跳过请求
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkipRequest {

    /**
     * 步骤ID
     */
    @NotBlank(message = "步骤ID不能为空")
    private String stepId;

    /**
     * 操作人
     */
    @NotBlank(message = "操作人不能为空")
    private String operator;

    /**
     * 原因
     */
    @NotBlank(message = "原因不能为空")
    private String reason;
}
