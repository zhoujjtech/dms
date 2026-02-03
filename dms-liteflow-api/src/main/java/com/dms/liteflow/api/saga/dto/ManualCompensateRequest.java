package com.dms.liteflow.api.saga.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 手动补偿请求
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualCompensateRequest {

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
