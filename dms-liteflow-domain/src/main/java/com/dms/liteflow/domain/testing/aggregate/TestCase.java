package com.dms.liteflow.domain.testing.aggregate;

import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 测试用例聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCase {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private TenantId tenantId;

    /**
     * 配置类型 (COMPONENT/CHAIN)
     */
    private String configType;

    /**
     * 配置ID
     */
    private Long configId;

    /**
     * 测试用例名称
     */
    private String name;

    /**
     * 输入数据（JSON格式）
     */
    private String inputData;

    /**
     * 期望结果（JSON格式）
     */
    private String expectedResult;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 验证输入数据
     */
    public void validateInputData() {
        if (this.inputData == null || this.inputData.trim().isEmpty()) {
            throw new IllegalArgumentException("Input data cannot be empty");
        }
    }

    /**
     * 验证期望结果
     */
    public void validateExpectedResult() {
        if (this.expectedResult == null || this.expectedResult.trim().isEmpty()) {
            throw new IllegalArgumentException("Expected result cannot be empty");
        }
    }
}
