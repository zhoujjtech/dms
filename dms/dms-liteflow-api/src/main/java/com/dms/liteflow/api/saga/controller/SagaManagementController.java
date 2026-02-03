package com.dms.liteflow.api.saga.controller;

import com.dms.liteflow.application.saga.SagaExecutionService;
import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.repository.SagaExecutionRepository;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Saga 管理 API 控制器
 * 提供 Saga 执行实例的管理、查询、人工操作等功能
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
    private final SagaExecutionRepository sagaExecutionRepository;

    /**
     * 查询执行详情
     *
     * GET /api/saga/executions/{executionId}
     *
     * @param executionId 执行ID
     * @return 执行详情
     */
    @GetMapping("/{executionId}")
    public ResponseEntity<SagaExecutionDetailVO> getExecutionDetail(@PathVariable String executionId) {
        log.info("Querying saga execution detail: executionId={}", executionId);

        try {
            TenantId tenantId = TenantContext.getTenantId();

            SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(
                    SagaExecutionId.of(executionId)).orElse(null);

            if (sagaExecution == null) {
                return ResponseEntity.notFound().build();
            }

            SagaExecutionDetailVO detail = SagaExecutionDetailVO.fromDomain(sagaExecution);
            return ResponseEntity.ok(detail);

        } catch (Exception e) {
            log.error("Failed to get execution detail: executionId={}", executionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 查询执行列表
     *
     * GET /api/saga/executions?tenantId={tenantId}&status={status}&page={page}&size={size}
     *
     * @param tenantId 租户ID
     * @param status 状态（可选）
     * @param page 页码（从 0 开始）
     * @param size 每页大小
     * @return 执行列表
     */
    @GetMapping
    public ResponseEntity<SagaExecutionListVO> queryExecutions(
            @RequestParam Long tenantId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        log.info("Querying saga executions: tenantId={}, status={}, page={}",
                tenantId, status, page);

        try {
            List<SagaExecution> executions;

            if (status != null && !status.isEmpty()) {
                executions = sagaExecutionRepository.findByTenantIdAndStatus(
                        TenantId.of(tenantId),
                        com.dms.liteflow.domain.saga.valueobject.SagaStatus.valueOf(status)
                );
            } else {
                executions = sagaExecutionRepository.findByTenantId(TenantId.of(tenantId));
            }

            // 分页
            int start = page * size;
            int end = Math.min(start + size, executions.size());
            List<SagaExecution> pagedExecutions = executions.subList(start, end);

            List<SagaExecutionListItemVO> items = pagedExecutions.stream()
                    .map(SagaExecutionListItemVO::fromDomain)
                    .collect(Collectors.toList());

            SagaExecutionListVO response = SagaExecutionListVO.builder()
                    .total((long) executions.size())
                    .page((long) page)
                    .size((long) size)
                    .items(items)
                    .build();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to query executions: tenantId={}", tenantId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 手动触发补偿
     *
     * POST /api/saga/executions/{executionId}/compensate
     *
     * @param executionId 执行ID
     * @param request 补偿请求
     * @return 操作结果
     */
    @PostMapping("/{executionId}/compensate")
    public ResponseEntity<OperationResultVO> manualCompensate(
            @PathVariable String executionId,
            @RequestBody ManualCompensateRequest request) {

        log.info("Manual compensate requested: executionId={}, operator={}",
                executionId, request.getOperator());

        try {
            boolean success = sagaExecutionService.compensate(executionId, request.getOperator());

            return ResponseEntity.ok(OperationResultVO.builder()
                    .success(success)
                    .message(success ? "补偿成功" : "补偿失败")
                    .executionId(executionId)
                    .build());

        } catch (Exception e) {
            log.error("Failed to compensate: executionId={}", executionId, e);
            return ResponseEntity.ok(OperationResultVO.builder()
                    .success(false)
                    .message("补偿失败: " + e.getMessage())
                    .executionId(executionId)
                    .build());
        }
    }

    /**
     * 重试失败节点
     *
     * POST /api/saga/executions/{executionId}/retry
     *
     * @param executionId 执行ID
     * @param request 重试请求
     * @return 操作结果
     */
    @PostMapping("/{executionId}/retry")
    public ResponseEntity<OperationResultVO> retry(
            @PathVariable String executionId,
            @RequestBody RetryRequest request) {

        log.info("Retry requested: executionId={}, stepId={}",
                executionId, request.getStepId());

        try {
            boolean success = sagaExecutionService.retry(
                    executionId,
                    request.getStepId(),
                    request.getNewInputData()
            );

            return ResponseEntity.ok(OperationResultVO.builder()
                    .success(success)
                    .message(success ? "重试成功" : "重试失败")
                    .executionId(executionId)
                    .stepId(request.getStepId())
                    .build());

        } catch (Exception e) {
            log.error("Failed to retry: executionId={}, stepId={}",
                    executionId, request.getStepId(), e);
            return ResponseEntity.ok(OperationResultVO.builder()
                    .success(false)
                    .message("重试失败: " + e.getMessage())
                    .executionId(executionId)
                    .stepId(request.getStepId())
                    .build());
        }
    }

    /**
     * 跳过失败节点
     *
     * POST /api/saga/executions/{executionId}/skip
     *
     * @param executionId 执行ID
     * @param request 跳过请求
     * @return 操作结果
     */
    @PostMapping("/{executionId}/skip")
    public ResponseEntity<OperationResultVO> skip(
            @PathVariable String executionId,
            @RequestBody SkipRequest request) {

        log.info("Skip requested: executionId={}, stepId={}, reason={}",
                executionId, request.getStepId(), request.getReason());

        try {
            boolean success = sagaExecutionService.skip(
                    executionId,
                    request.getStepId(),
                    request.getReason()
            );

            return ResponseEntity.ok(OperationResultVO.builder()
                    .success(success)
                    .message(success ? "跳过成功" : "跳过失败")
                    .executionId(executionId)
                    .stepId(request.getStepId())
                    .build());

        } catch (Exception e) {
            log.error("Failed to skip: executionId={}, stepId={}",
                    executionId, request.getStepId(), e);
            return ResponseEntity.ok(OperationResultVO.builder()
                    .success(false)
                    .message("跳过失败: " + e.getMessage())
                    .executionId(executionId)
                    .stepId(request.getStepId())
                    .build());
        }
    }

    /**
     * 人工决策
     *
     * POST /api/saga/executions/{executionId}/manual-decision
     *
     * @param executionId 执行ID
     * @param request 决策请求
     * @return 操作结果
     */
    @PostMapping("/{executionId}/manual-decision")
    public ResponseEntity<OperationResultVO> manualDecision(
            @PathVariable String executionId,
            @RequestBody ManualDecisionRequest request) {

        log.info("Manual decision requested: executionId={}, decision={}, operator={}",
                executionId, request.getDecision(), request.getOperator());

        try {
            boolean success = sagaExecutionService.manualDecision(
                    executionId,
                    request.getDecision(),
                    request.getReason(),
                    request.getOperator()
            );

            return ResponseEntity.ok(OperationResultVO.builder()
                    .success(success)
                    .message(success ? "决策处理成功" : "决策处理失败")
                    .executionId(executionId)
                    .build());

        } catch (Exception e) {
            log.error("Failed to process manual decision: executionId={}", executionId, e);
            return ResponseEntity.ok(OperationResultVO.builder()
                    .success(false)
                    .message("决策处理失败: " + e.getMessage())
                    .executionId(executionId)
                    .build());
        }
    }

    /**
     * 获取执行时间线
     *
     * GET /api/saga/executions/{executionId}/timeline
     *
     * @param executionId 执行ID
     * @return 时间线数据
     */
    @GetMapping("/{executionId}/timeline")
    public ResponseEntity<ExecutionTimelineVO> getTimeline(@PathVariable String executionId) {
        log.info("Querying execution timeline: executionId={}", executionId);

        try {
            TenantId tenantId = TenantContext.getTenantId();

            SagaExecution sagaExecution = sagaExecutionRepository.findByExecutionId(
                    SagaExecutionId.of(executionId)).orElse(null);

            if (sagaExecution == null) {
                return ResponseEntity.notFound().build();
            }

            ExecutionTimelineVO timeline = ExecutionTimelineVO.fromDomain(sagaExecution);
            return ResponseEntity.ok(timeline);

        } catch (Exception e) {
            log.error("Failed to get timeline: executionId={}", executionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
