package com.dms.liteflow.api.saga.controller;

import com.dms.liteflow.api.saga.dto.ManualCompensateRequest;
import com.dms.liteflow.api.saga.dto.ManualDecisionRequest;
import com.dms.liteflow.api.saga.dto.RetryRequest;
import com.dms.liteflow.api.saga.dto.SkipRequest;
import com.dms.liteflow.api.saga.vo.CompensationLogVO;
import com.dms.liteflow.api.saga.vo.ExecutionTimelineVO;
import com.dms.liteflow.api.saga.vo.OperationResultVO;
import com.dms.liteflow.api.saga.vo.SagaExecutionDetailVO;
import com.dms.liteflow.api.saga.vo.SagaExecutionListVO;
import com.dms.liteflow.application.saga.SagaExecutionService;
import com.dms.liteflow.application.saga.SagaManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 管理 API 控制器
 * 提供 Saga 执行实例的查询、管理、人工干预等操作
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@RestController
@RequestMapping("/api/saga/executions")
@RequiredArgsConstructor
public class SagaManagementController {

    private final SagaExecutionService sagaExecutionService;
    private final SagaManagementService sagaManagementService;

    /**
     * 查询执行详情
     *
     * GET /api/saga/executions/{id}
     *
     * @param id 执行ID
     * @return 执行详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<SagaExecutionDetailVO> getExecutionDetail(@PathVariable String id) {
        log.info("Querying saga execution detail: executionId={}", id);

        SagaExecutionDetailVO detail = sagaManagementService.getExecutionDetail(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(detail);
    }

    /**
     * 查询执行列表（分页）
     *
     * GET /api/saga/executions?page=1&size=20&status=FAILED
     *
     * @param page 页码（从0开始）
     * @param size 每页大小
     * @param status 状态过滤（可选）
     * @param chainName 链名称过滤（可选）
     * @param startTime 开始时间过滤（可选）
     * @param endTime 结束时间过滤（可选）
     * @return 执行列表
     */
    @GetMapping
    public ResponseEntity<SagaExecutionListVO> listExecutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String chainName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        log.info("Listing saga executions: page={}, size={}, status={}, chainName={}",
                page, size, status, chainName);

        SagaExecutionListVO result = sagaManagementService.listExecutions(
                page, size, status, chainName, startTime, endTime
        );
        return ResponseEntity.ok(result);
    }

    /**
     * 手动触发补偿
     *
     * POST /api/saga/executions/{id}/compensate
     *
     * @param id 执行ID
     * @param request 补偿请求
     * @return 操作结果
     */
    @PostMapping("/{id}/compensate")
    public ResponseEntity<OperationResultVO> manualCompensate(
            @PathVariable String id,
            @Valid @RequestBody ManualCompensateRequest request) {

        log.info("Manual compensation requested: executionId={}, operator={}",
                id, request.getOperator());

        boolean success = sagaExecutionService.compensate(id, request.getOperator());

        OperationResultVO result = OperationResultVO.builder()
                .success(success)
                .message(success ? "补偿执行成功" : "补偿执行失败")
                .executionId(id)
                .operatedAt(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.ok(result);
    }

    /**
     * 重试失败的步骤
     *
     * POST /api/saga/executions/{id}/retry
     *
     * @param id 执行ID
     * @param request 重试请求
     * @return 操作结果
     */
    @PostMapping("/{id}/retry")
    public ResponseEntity<OperationResultVO> retryStep(
            @PathVariable String id,
            @Valid @RequestBody RetryRequest request) {

        log.info("Retry step requested: executionId={}, stepId={}",
                id, request.getStepId());

        boolean success = sagaExecutionService.retry(
                id,
                request.getStepId(),
                request.getNewInputData()
        );

        OperationResultVO result = OperationResultVO.builder()
                .success(success)
                .message(success ? "重试成功" : "重试失败")
                .executionId(id)
                .stepId(request.getStepId())
                .operatedAt(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.ok(result);
    }

    /**
     * 跳过失败步骤
     *
     * POST /api/saga/executions/{id}/skip
     *
     * @param id 执行ID
     * @param request 跳过请求
     * @return 操作结果
     */
    @PostMapping("/{id}/skip")
    public ResponseEntity<OperationResultVO> skipStep(
            @PathVariable String id,
            @Valid @RequestBody SkipRequest request) {

        log.info("Skip step requested: executionId={}, stepId={}, reason={}",
                id, request.getStepId(), request.getReason());

        boolean success = sagaExecutionService.skip(
                id,
                request.getStepId(),
                request.getReason()
        );

        OperationResultVO result = OperationResultVO.builder()
                .success(success)
                .message(success ? "跳过成功" : "跳过失败")
                .executionId(id)
                .stepId(request.getStepId())
                .operatedAt(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.ok(result);
    }

    /**
     * 人工决策
     *
     * POST /api/saga/executions/{id}/manual-decision
     *
     * @param id 执行ID
     * @param request 决策请求
     * @return 操作结果
     */
    @PostMapping("/{id}/manual-decision")
    public ResponseEntity<OperationResultVO> manualDecision(
            @PathVariable String id,
            @Valid @RequestBody ManualDecisionRequest request) {

        log.info("Manual decision requested: executionId={}, decision={}, operator={}",
                id, request.getDecision(), request.getOperator());

        boolean success = sagaExecutionService.manualDecision(
                id,
                request.getDecision(),
                request.getReason(),
                request.getOperator()
        );

        OperationResultVO result = OperationResultVO.builder()
                .success(success)
                .message(success ? "决策已执行" : "决策执行失败")
                .executionId(id)
                .operatedAt(LocalDateTime.now().toString())
                .build();

        return ResponseEntity.ok(result);
    }

    /**
     * 查询执行时间线
     *
     * GET /api/saga/executions/{id}/timeline
     *
     * @param id 执行ID
     * @return 执行时间线
     */
    @GetMapping("/{id}/timeline")
    public ResponseEntity<ExecutionTimelineVO> getExecutionTimeline(@PathVariable String id) {
        log.info("Querying execution timeline: executionId={}", id);

        ExecutionTimelineVO timeline = sagaManagementService.getExecutionTimeline(id);
        if (timeline == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(timeline);
    }

    /**
     * 查询补偿日志
     *
     * GET /api/saga/executions/{id}/compensation-logs
     *
     * @param id 执行ID
     * @return 补偿日志列表
     */
    @GetMapping("/{id}/compensation-logs")
    public ResponseEntity<List<CompensationLogVO>> getCompensationLogs(@PathVariable String id) {
        log.info("Querying compensation logs: executionId={}", id);

        List<CompensationLogVO> logs = sagaManagementService.getCompensationLogs(id);
        return ResponseEntity.ok(logs);
    }
}
