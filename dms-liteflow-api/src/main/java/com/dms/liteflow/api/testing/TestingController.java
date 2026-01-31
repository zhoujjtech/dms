package com.dms.liteflow.api.testing;

import com.dms.liteflow.application.testing.ChainTestService;
import com.dms.liteflow.application.testing.ComponentTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 测试控制器
 * <p>
 * 提供测试相关的API端点
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/testing")
@RequiredArgsConstructor
public class TestingController {

    private final ComponentTestService componentTestService;
    private final ChainTestService chainTestService;

    /**
     * 测试组件
     * POST /api/testing/components/{componentId}
     */
    @PostMapping("/components/{componentId}")
    public ResponseEntity<ComponentTestService.TestResult> testComponent(
            @PathVariable String componentId,
            @RequestParam Long tenantId,
            @RequestBody Map<String, Object> inputData
    ) {
        log.info("POST /api/testing/components/{} - tenantId: {}", componentId, tenantId);

        ComponentTestService.TestResult result = componentTestService.testComponent(
                tenantId, componentId, inputData
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 测试流程链
     * POST /api/testing/chains/{chainName}
     */
    @PostMapping("/chains/{chainName}")
    public ResponseEntity<ChainTestService.ChainTestResult> testChain(
            @PathVariable String chainName,
            @RequestParam Long tenantId,
            @RequestBody Map<String, Object> inputData
    ) {
        log.info("POST /api/testing/chains/{} - tenantId: {}", chainName, tenantId);

        ChainTestService.ChainTestResult result = chainTestService.testChain(
                tenantId, chainName, inputData
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 测试流程链并返回执行路径
     * POST /api/testing/chains/{chainName}/with-path
     */
    @PostMapping("/chains/{chainName}/with-path")
    public ResponseEntity<ChainTestService.ChainTestResult> testChainWithPath(
            @PathVariable String chainName,
            @RequestParam Long tenantId,
            @RequestBody Map<String, Object> inputData
    ) {
        log.info("POST /api/testing/chains/{}/with-path - tenantId: {}", chainName, tenantId);

        ChainTestService.ChainTestResult result = chainTestService.testChainWithExecutionPath(
                tenantId, chainName, inputData
        );

        return ResponseEntity.ok(result);
    }

    /**
     * 批量测试组件
     * POST /api/testing/components/{componentId}/batch
     */
    @PostMapping("/components/{componentId}/batch")
    public ResponseEntity<List<ComponentTestService.TestResult>> batchTestComponents(
            @PathVariable String componentId,
            @RequestParam Long tenantId
    ) {
        log.info("POST /api/testing/components/{}/batch - tenantId: {}", componentId, tenantId);

        List<ComponentTestService.TestResult> results = componentTestService.batchTestComponents(
                tenantId, componentId
        );

        return ResponseEntity.ok(results);
    }

    /**
     * 保存测试用例
     * POST /api/testing/test-cases
     */
    @PostMapping("/test-cases")
    public ResponseEntity<?> saveTestCase(
            @RequestParam Long tenantId,
            @RequestParam String componentId,
            @RequestParam String name,
            @RequestParam String inputData,
            @RequestParam String expectedResult
    ) {
        log.info("POST /api/testing/test-cases - componentId: {}, name: {}", componentId, name);

        var testCase = componentTestService.saveTestCase(
                tenantId, componentId, name, inputData, expectedResult
        );

        return ResponseEntity.ok(testCase);
    }

    /**
     * 执行测试用例
     * POST /api/testing/test-cases/{testCaseId}/execute
     */
    @PostMapping("/test-cases/{testCaseId}/execute")
    public ResponseEntity<ComponentTestService.TestResult> executeTestCase(
            @PathVariable Long testCaseId
    ) {
        log.info("POST /api/testing/test-cases/{}/execute", testCaseId);

        ComponentTestService.TestResult result = componentTestService.executeTestCase(testCaseId);

        return ResponseEntity.ok(result);
    }
}
