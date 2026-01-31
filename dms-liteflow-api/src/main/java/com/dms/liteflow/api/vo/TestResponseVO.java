package com.dms.liteflow.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 测试响应 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestResponseVO {
    private Boolean success;
    private Object outputData;
    private Long executeTime;
    private String errorMessage;
    private String message;
    private String executionPath;
    private Map<String, Object> executionSteps;
}
