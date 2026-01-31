package com.dms.liteflow.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试请求 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestRequestDTO {
    private String inputData;
    private String expectedResult;
}
