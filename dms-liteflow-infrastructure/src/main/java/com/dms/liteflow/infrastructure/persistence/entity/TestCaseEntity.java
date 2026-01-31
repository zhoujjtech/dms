package com.dms.liteflow.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试用例实体（MyBatis）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseEntity {

    private Long id;
    private Long tenantId;
    private String configType;
    private Long configId;
    private String name;
    private String inputData;
    private String expectedResult;
    private LocalDateTime createdAt;
}
