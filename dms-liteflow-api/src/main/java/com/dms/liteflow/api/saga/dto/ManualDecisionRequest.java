package com.dms.liteflow.api.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 人工决策请求
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualDecisionRequest {

    /**
     * 决策类型：CONTINUE/COMPENSATE/RETRY
     */
    @NotBlank(message = "决策不能为空")
    private String decision;

    /**
     * 原因
     */
    @NotBlank(message = "原因不能为空")
    private String reason;

    /**
     * 操作人
     */
    @NotBlank(message = "操作人不能为空")
    private String operator;

    /**
     * 修改后的输入数据（可选）
     */
    private Map<String, Object> modifiedData;
}
