package com.dms.liteflow.api.execution;

import com.dms.liteflow.application.execution.ExecutionService;
import com.dms.liteflow.domain.dto.ExecutionRequestDTO;
import com.dms.liteflow.domain.vo.ExecutionResponseVO;
import com.dms.liteflow.domain.vo.ExecutionStatusVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 流程执行 API 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/execute")
@RequiredArgsConstructor
public class ExecutionController {

    private final ExecutionService executionService;

    /**
     * 同步执行流程
     */
    @PostMapping("/sync")
    public ResponseEntity<ExecutionResponseVO> executeSync(@RequestBody ExecutionRequestDTO request) {
        log.info("Sync execution request: chainName={}, tenantId={}", request.getChainName(), request.getTenantId());
        ExecutionResponseVO response = executionService.executeSync(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 异步执行流程
     */
    @PostMapping("/async")
    public ResponseEntity<ExecutionResponseVO> executeAsync(@RequestBody ExecutionRequestDTO request) {
        log.info("Async execution request: chainName={}, tenantId={}", request.getChainName(), request.getTenantId());
        ExecutionResponseVO response = executionService.executeAsync(request).join();
        return ResponseEntity.accepted().body(response);
    }

    /**
     * 查询执行状态
     */
    @GetMapping("/status/{executionId}")
    public ResponseEntity<ExecutionStatusVO> getExecutionStatus(@PathVariable String executionId) {
        log.info("Query execution status: executionId={}", executionId);
        ExecutionStatusVO status = executionService.getExecutionStatus(executionId);
        return ResponseEntity.ok(status);
    }

    /**
     * 查询执行结果
     */
    @GetMapping("/result/{executionId}")
    public ResponseEntity<ExecutionResponseVO> getExecutionResult(@PathVariable String executionId) {
        log.info("Query execution result: executionId={}", executionId);
        ExecutionResponseVO result = executionService.getExecutionResult(executionId);
        return ResponseEntity.ok(result);
    }
}
