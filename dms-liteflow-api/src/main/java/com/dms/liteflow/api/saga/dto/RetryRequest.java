package com.dms.liteflow.api.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 重试请求
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryRequest {

    /**
     * 步骤ID
     */
    @NotBlank(message = "步骤ID不能为空")
    private String stepId;

    /**
     * 新的输入数据（可选）
     */
    private Map<String, Object> newInputData;
}
