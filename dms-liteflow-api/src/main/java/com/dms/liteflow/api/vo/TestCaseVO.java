package com.dms.liteflow.api.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 测试用例 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseVO {
    private Long id;
    private Long tenantId;
    private String configType;
    private Long configId;
    private String name;
    private String inputData;
    private String expectedResult;
    private String createdAt;
}
