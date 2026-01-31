package com.dms.liteflow.application.monitoring;

import com.dms.liteflow.domain.monitoring.aggregate.ExecutionRecord;
import com.dms.liteflow.domain.monitoring.repository.ExecutionRecordRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.yomahub.liteflow.flow.entity.CmpStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 监控数据采集服务
 * <p>
 * 采集流程和组件的执行监控数据
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MonitoringCollectorService {

    private final ExecutionRecordRepository executionRecordRepository;

    /**
     * 记录流程执行开始
     *
     * @param tenantId       租户ID
     * @param chainId        流程链ID
     * @param chainExecutionId 执行ID
     */
    public void recordChainExecutionStart(Long tenantId, Long chainId, String chainExecutionId) {
        log.debug("Recording chain execution start: {} for tenant: {}", chainExecutionId, tenantId);

        ExecutionRecord record = ExecutionRecord.builder()
                .tenantId(TenantId.of(tenantId))
                .chainId(ChainId.of(chainId))
                .chainExecutionId(chainExecutionId)
                .status("RUNNING")
                .executeTime(0L)
                .createdAt(LocalDateTime.now())
                .build();

        executionRecordRepository.save(record);
    }

    /**
     * 记录流程执行完成
     *
     * @param tenantId       租户ID
     * @param chainId        流程链ID
     * @param chainExecutionId 执行ID
     * @param executeTime    执行耗时
     * @param success        是否成功
     * @param errorMessage   错误信息
     */
    @Transactional
    public void recordChainExecutionEnd(
            Long tenantId,
            Long chainId,
            String chainExecutionId,
            long executeTime,
            boolean success,
            String errorMessage
    ) {
        log.debug("Recording chain execution end: {} for tenant: {}", chainExecutionId, tenantId);

        List<ExecutionRecord> records = executionRecordRepository.findByExecutionId(chainExecutionId);

        for (ExecutionRecord record : records) {
            if (record.getChainId().getValue().equals(chainId)) {
                record.setExecuteTime(executeTime);
                record.setStatus(success ? "SUCCESS" : "FAILURE");
                record.setErrorMessage(errorMessage);
                executionRecordRepository.save(record);
            }
        }
    }

    /**
     * 记录组件执行
     *
     * @param tenantId       租户ID
     * @param chainId        流程链ID
     * @param componentId    组件ID
     * @param chainExecutionId 执行ID
     * @param executeTime    执行耗时
     * @param success        是否成功
     * @param errorMessage   错误信息
     */
    @Transactional
    public void recordComponentExecution(
            Long tenantId,
            Long chainId,
            String componentId,
            String chainExecutionId,
            long executeTime,
            boolean success,
            String errorMessage
    ) {
        log.debug("Recording component execution: {} for chain: {}", componentId, chainExecutionId);

        ExecutionRecord record = ExecutionRecord.builder()
                .tenantId(TenantId.of(tenantId))
                .chainId(ChainId.of(chainId))
                .componentId(componentId)
                .chainExecutionId(chainExecutionId)
                .executeTime(executeTime)
                .status(success ? "SUCCESS" : "FAILURE")
                .errorMessage(errorMessage)
                .createdAt(LocalDateTime.now())
                .build();

        executionRecordRepository.save(record);
    }

    /**
     * 批量记录执行步骤
     *
     * @param tenantId       租户ID
     * @param chainId        流程链ID
     * @param chainExecutionId 执行ID
     * @param steps          执行步骤
     */
    @Transactional
    public void recordExecutionSteps(
            Long tenantId,
            Long chainId,
            String chainExecutionId,
            List<CmpStep> steps
    ) {
        log.debug("Recording execution steps for chain: {}", chainExecutionId);

        for (CmpStep step : steps) {
            ExecutionRecord record = ExecutionRecord.builder()
                    .tenantId(TenantId.of(tenantId))
                    .chainId(ChainId.of(chainId))
                    .componentId(step.getNodeId())
                    .chainExecutionId(chainExecutionId)
                    .executeTime(step.getTimeSpent())
                    .status(step.isSuccess() ? "SUCCESS" : "FAILURE")
                    .errorMessage(step.getException() != null ? step.getException().getMessage() : null)
                    .createdAt(LocalDateTime.now())
                    .build();

            executionRecordRepository.save(record);
        }
    }

    /**
     * 生成执行ID
     *
     * @return 执行ID
     */
    public String generateExecutionId() {
        return UUID.randomUUID().toString();
    }
}
