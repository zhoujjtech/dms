package com.dms.liteflow.api.saga.controller;

import com.dms.liteflow.application.saga.SagaExecutionRequest;
import com.dms.liteflow.application.saga.SagaExecutionResponse;
import com.dms.liteflow.application.saga.SagaExecutionService;
import com.dms.liteflow.application.saga.SagaExecutionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * Saga 执行 API 控制器
 * 提供 Saga 流程的执行、查询、补偿等操作
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@RestController
@RequestMapping("/api/saga/execute")
@RequiredArgsConstructor
public class SagaExecutionController {

    private final SagaExecutionService sagaExecutionService;

    /**
     * 同步执行 Saga 流程
     *
     * POST /api/saga/execute
     *
     * @param request 执行请求
     * @return 执行响应
     */
    @PostMapping
    public ResponseEntity<SagaExecutionResponse> executeSaga(@Valid @RequestBody SagaExecutionRequest request) {
        log.info("Sync saga execution requested: chainName={}, tenantId={}, sagaMode={}",
                request.getChainName(), request.getTenantId(), request.getSagaMode());

        SagaExecutionResponse response = sagaExecutionService.executeSaga(request);
        return ResponseEntity.ok(response);
    }

    /**
     * 异步执行 Saga 流程
     *
     * POST /api/saga/execute/async
     *
     * @param request 执行请求
     * @return 执行响应（包含 executionId）
     */
    @PostMapping("/async")
    public ResponseEntity<SagaExecutionResponse> executeSagaAsync(@Valid @RequestBody SagaExecutionRequest request) {
        log.info("Async saga execution requested: chainName={}, tenantId={}",
                request.getChainName(), request.getTenantId());

        return ResponseEntity.accepted()
                .body(sagaExecutionService.executeSagaAsync(request).join());
    }

    /**
     * 查询执行状态
     *
     * GET /api/saga/execute/status/{executionId}
     *
     * @param executionId 执行ID
     * @return 执行状态
     */
    @GetMapping("/status/{executionId}")
    public ResponseEntity<SagaExecutionStatus> getExecutionStatus(@PathVariable String executionId) {
        log.info("Querying saga execution status: executionId={}", executionId);

        SagaExecutionStatus status = sagaExecutionService.getExecutionStatus(executionId);
        return ResponseEntity.ok(status);
    }

    /**
     * 查询执行结果
     *
     * GET /api/saga/execute/result/{executionId}
     *
     * @param executionId 执行ID
     * @return 执行结果
     */
    @GetMapping("/result/{executionId}")
    public ResponseEntity<SagaExecutionStatus> getExecutionResult(@PathVariable String executionId) {
        log.info("Querying saga execution result: executionId={}", executionId);

        SagaExecutionStatus status = sagaExecutionService.getExecutionStatus(executionId);
        return ResponseEntity.ok(status);
    }
}
