package com.dms.liteflow.application.saga;

import com.dms.liteflow.api.saga.vo.CompensationLogVO;
import com.dms.liteflow.api.saga.vo.ExecutionTimelineVO;
import com.dms.liteflow.api.saga.vo.SagaExecutionDetailVO;
import com.dms.liteflow.api.saga.vo.SagaExecutionListVO;
import com.dms.liteflow.api.saga.vo.SagaExecutionListItemVO;
import com.dms.liteflow.api.saga.vo.StepExecutionDetailVO;
import com.dms.liteflow.api.saga.vo.TimelineNodeVO;
import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.CompensationLog;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.repository.CompensationLogRepository;
import com.dms.liteflow.domain.saga.repository.SagaExecutionRepository;
import com.dms.liteflow.domain.saga.service.SagaStateService;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Saga 管理服务
 * 负责 Saga 执行实例的查询、统计、监控等操作
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SagaManagementService {

    private final SagaStateService sagaStateService;
    private final SagaExecutionRepository sagaExecutionRepository;
    private final CompensationLogRepository compensationLogRepository;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 查询执行详情
     */
    public SagaExecutionDetailVO getExecutionDetail(String executionId) {
        try {
            TenantId tenantId = TenantContext.getTenantId();
            TenantContext.setTenantId(tenantId);

            SagaExecution sagaExecution = sagaStateService.getExecution(SagaExecutionId.of(executionId));
            if (sagaExecution == null) {
                return null;
            }

            // 转换步骤信息
            List<StepExecutionDetailVO> stepVOs = sagaExecution.getExecutionStack().stream()
                    .map(this::convertToStepVO)
                    .collect(Collectors.toList());

            return SagaExecutionDetailVO.builder()
                    .executionId(sagaExecution.getId().getValue())
                    .tenantId(sagaExecution.getTenantId().getValue())
                    .chainName(sagaExecution.getChainName())
                    .status(sagaExecution.getStatus().name())
                    .currentStepIndex(sagaExecution.getCurrentStepIndex())
                    .inputData(sagaExecution.getInputData())
                    .outputData(sagaExecution.getOutputData())
                    .failureReason(sagaExecution.getFailureReason())
                    .startTime(sagaExecution.getStartedAt() != null ?
                            sagaExecution.getStartedAt().format(FORMATTER) : null)
                    .endTime(sagaExecution.getCompletedAt() != null ?
                            sagaExecution.getCompletedAt().format(FORMATTER) : null)
                    .steps(stepVOs)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get execution detail: executionId={}", executionId, e);
            return null;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 查询执行列表（分页）
     */
    public SagaExecutionListVO listExecutions(int page, int size, String status,
                                              String chainName, LocalDateTime startTime,
                                              LocalDateTime endTime) {
        try {
            TenantId tenantId = TenantContext.getTenantId();
            TenantContext.setTenantId(tenantId);

            // 计算偏移量
            int offset = page * size;

            // 查询列表
            List<SagaExecution> executions = sagaExecutionRepository.findByTenantId(
                    tenantId,
                    status != null ? SagaStatus.valueOf(status) : null,
                    chainName,
                    startTime,
                    endTime,
                    offset,
                    size
            );

            // 查询总数
            int total = sagaExecutionRepository.countByTenantId(
                    tenantId,
                    status != null ? SagaStatus.valueOf(status) : null,
                    chainName,
                    startTime,
                    endTime
            );

            // 转换为 VO
            List<SagaExecutionListItemVO> items = executions.stream()
                    .map(this::convertToListItemVO)
                    .collect(Collectors.toList());

            return SagaExecutionListVO.builder()
                    .items(items)
                    .total((long) total)
                    .page((long) page)
                    .size((long) size)
                    .build();

        } catch (Exception e) {
            log.error("Failed to list executions: page={}, size={}", page, size, e);
            return SagaExecutionListVO.builder()
                    .items(List.of())
                    .total(0L)
                    .page((long) page)
                    .size((long) size)
                    .build();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 查询执行时间线
     */
    public ExecutionTimelineVO getExecutionTimeline(String executionId) {
        try {
            TenantId tenantId = TenantContext.getTenantId();
            TenantContext.setTenantId(tenantId);

            SagaExecution sagaExecution = sagaStateService.getExecution(SagaExecutionId.of(executionId));
            if (sagaExecution == null) {
                return null;
            }

            // 构建时间线节点
            List<TimelineNodeVO> nodes = sagaExecution.getExecutionStack().stream()
                    .map(step -> TimelineNodeVO.builder()
                            .stepId(step.getId().getValue())
                            .componentName(step.getComponentName())
                            .status(step.getStatus().name())
                            .timestamp(step.getStartedAt() != null ?
                                    step.getStartedAt().format(FORMATTER) : null)
                            .endTime(step.getCompletedAt() != null ?
                                    step.getCompletedAt().format(FORMATTER) : null)
                            .inputData(step.getInputData())
                            .outputData(step.getOutputData())
                            .errorMessage(step.getErrorMessage())
                            .build())
                    .collect(Collectors.toList());

            return ExecutionTimelineVO.builder()
                    .executionId(executionId)
                    .chainName(sagaExecution.getChainName())
                    .status(sagaExecution.getStatus().name())
                    .startTime(sagaExecution.getStartedAt() != null ?
                            sagaExecution.getStartedAt().format(FORMATTER) : null)
                    .endTime(sagaExecution.getCompletedAt() != null ?
                            sagaExecution.getCompletedAt().format(FORMATTER) : null)
                    .nodes(nodes)
                    .build();

        } catch (Exception e) {
            log.error("Failed to get execution timeline: executionId={}", executionId, e);
            return null;
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 查询补偿日志
     */
    public List<CompensationLogVO> getCompensationLogs(String executionId) {
        try {
            TenantId tenantId = TenantContext.getTenantId();
            TenantContext.setTenantId(tenantId);

            List<CompensationLog> logs = compensationLogRepository.findByExecutionId(executionId);

            return logs.stream()
                    .map(this::convertToCompensationLogVO)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Failed to get compensation logs: executionId={}", executionId, e);
            return List.of();
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 转换为列表项 VO
     */
    private SagaExecutionListItemVO convertToListItemVO(SagaExecution execution) {
        return SagaExecutionListItemVO.builder()
                .executionId(execution.getId().getValue())
                .chainName(execution.getChainName())
                .status(execution.getStatus().name())
                .startTime(execution.getStartedAt() != null ?
                        execution.getStartedAt().format(FORMATTER) : null)
                .endTime(execution.getCompletedAt() != null ?
                        execution.getCompletedAt().format(FORMATTER) : null)
                .failureReason(execution.getFailureReason())
                .build();
    }

    /**
     * 转换为步骤详情 VO
     */
    private StepExecutionDetailVO convertToStepVO(StepExecution step) {
        return StepExecutionDetailVO.builder()
                .stepId(step.getId().getValue())
                .componentName(step.getComponentName())
                .status(step.getStatus().name())
                .inputData(step.getInputData())
                .outputData(step.getOutputData())
                .errorMessage(step.getErrorMessage())
                .startTime(step.getStartedAt() != null ?
                        step.getStartedAt().format(FORMATTER) : null)
                .endTime(step.getCompletedAt() != null ?
                        step.getCompletedAt().format(FORMATTER) : null)
                .retryCount(step.getRetryCount())
                .needsCompensation(step.getNeedsCompensation())
                .build();
    }

    /**
     * 转换为补偿日志 VO
     */
    private CompensationLogVO convertToCompensationLogVO(CompensationLog log) {
        return CompensationLogVO.builder()
                .executionId(log.getExecutionId().getValue())
                .stepId(log.getStepId() != null ? log.getStepId().getValue() : null)
                .compensateComponent(log.getCompensateComponent())
                .status(log.getStatus().name())
                .errorMessage(log.getErrorMessage())
                .compensatedAt(log.getCompensatedAt() != null ?
                        log.getCompensatedAt().format(FORMATTER) : null)
                .operator(log.getOperator())
                .operationType(log.getOperationType().name())
                .build();
    }
}
