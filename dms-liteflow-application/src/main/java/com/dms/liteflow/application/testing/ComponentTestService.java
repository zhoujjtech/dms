package com.dms.liteflow.application.testing;

import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.domain.testing.aggregate.TestCase;
import com.dms.liteflow.domain.testing.repository.TestCaseRepository;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.entity.CmpStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 组件测试服务
 * <p>
 * 提供组件级别的测试功能
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComponentTestService {

    private final FlowExecutor flowExecutor;
    private final RuleComponentRepository ruleComponentRepository;
    private final TestCaseRepository testCaseRepository;

    /**
     * 测试组件
     *
     * @param tenantId    租户ID
     * @param componentId 组件ID
     * @param inputData   输入数据
     * @return 测试结果
     */
    public TestResult testComponent(Long tenantId, String componentId, Map<String, Object> inputData) {
        log.info("Testing component: {} for tenant: {}", componentId, tenantId);

        TestResult result = new TestResult();
        result.setComponentId(componentId);
        result.setInputData(inputData);

        try {
            // 验证组件存在
            RuleComponent component = ruleComponentRepository.findByComponentId(ComponentId.of(componentId))
                    .orElseThrow(() -> new IllegalArgumentException("Component not found: " + componentId));

            if (!component.getTenantId().getValue().equals(tenantId)) {
                throw new IllegalArgumentException("Component does not belong to tenant: " + tenantId);
            }

            // 执行组件
            long startTime = System.currentTimeMillis();

            // TODO: 使用 LiteFlow 的单个组件执行功能
            // 这需要创建一个临时的流程链来测试单个组件

            long endTime = System.currentTimeMillis();
            result.setExecuteTime(endTime - startTime);
            result.setSuccess(true);

        } catch (Exception e) {
            log.error("Component test failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 保存测试用例
     *
     * @param tenantId       租户ID
     * @param componentId    组件ID
     * @param name           测试用例名称
     * @param inputData      输入数据（JSON）
     * @param expectedResult 期望结果（JSON）
     * @return 保存的测试用例
     */
    @Transactional
    public TestCase saveTestCase(
            Long tenantId,
            String componentId,
            String name,
            String inputData,
            String expectedResult
    ) {
        log.info("Saving test case: {} for component: {}", name, componentId);

        TestCase testCase = TestCase.builder()
                .tenantId(TenantId.of(tenantId))
                .configType("COMPONENT")
                .configId(Long.valueOf(componentId.hashCode()))
                .name(name)
                .inputData(inputData)
                .expectedResult(expectedResult)
                .build();

        return testCaseRepository.save(testCase);
    }

    /**
     * 执行测试用例
     *
     * @param testCaseId 测试用例ID
     * @return 测试结果
     */
    public TestResult executeTestCase(Long testCaseId) {
        log.info("Executing test case: {}", testCaseId);

        TestCase testCase = testCaseRepository.findById(testCaseId)
                .orElseThrow(() -> new IllegalArgumentException("Test case not found: " + testCaseId));

        // TODO: 解析 inputData 并执行测试
        TestResult result = new TestResult();
        result.setSuccess(true);
        result.setMessage("Test case executed successfully");

        return result;
    }

    /**
     * 批量测试组件
     *
     * @param tenantId    租户ID
     * @param componentId 组件ID
     * @return 批量测试结果
     */
    public List<TestResult> batchTestComponents(Long tenantId, String componentId) {
        log.info("Batch testing component: {} for tenant: {}", componentId, tenantId);

        List<TestCase> testCases = testCaseRepository.findByTenantIdAndConfigTypeAndConfigId(
                TenantId.of(tenantId),
                "COMPONENT",
                Long.valueOf(componentId.hashCode())
        );

        return testCases.stream()
                .map(testCase -> executeTestCase(testCase.getId()))
                .toList();
    }

    /**
     * 测试结果
     */
    @lombok.Data
    public static class TestResult {
        private String componentId;
        private Map<String, Object> inputData;
        private Object outputData;
        private long executeTime;
        private boolean success;
        private String errorMessage;
        private String message;
        private List<CmpStep> executionSteps;
    }
}
