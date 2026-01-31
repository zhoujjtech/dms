package com.dms.liteflow.infrastructure.persistence.repository;

import com.dms.liteflow.domain.monitoring.aggregate.ExecutionRecord;
import com.dms.liteflow.domain.monitoring.repository.ExecutionRecordRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.persistence.entity.ExecutionRecordEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.ExecutionRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 执行记录仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ExecutionRecordRepositoryImpl implements ExecutionRecordRepository {

    private final ExecutionRecordMapper executionRecordMapper;

    @Override
    public ExecutionRecord save(ExecutionRecord record) {
        ExecutionRecordEntity entity = toEntity(record);
        if (entity.getId() == null) {
            executionRecordMapper.insert(entity);
        } else {
            executionRecordMapper.updateById(entity);
        }
        return toDomain(entity);
    }

    @Override
    public void saveAll(List<ExecutionRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        List<ExecutionRecordEntity> entities = records.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        executionRecordMapper.insertBatch(entities);
    }

    @Override
    public List<ExecutionRecord> findByExecutionId(String executionId) {
        List<ExecutionRecordEntity> entities = executionRecordMapper.selectByExecutionId(executionId);
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionRecord> findByChainIdAndTimeRange(
            ChainId chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        List<ExecutionRecordEntity> entities = executionRecordMapper.selectByChainIdAndTimeRange(
                chainId.getValue(),
                startTime,
                endTime
        );
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<ExecutionRecord> findByTenantIdAndTimeRange(
            TenantId tenantId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        List<ExecutionRecordEntity> entities = executionRecordMapper.selectByTenantIdAndTimeRange(
                tenantId.getValue(),
                startTime,
                endTime
        );
        return entities.stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public long countExecutions(
            TenantId tenantId,
            ChainId chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    ) {
        return executionRecordMapper.countExecutions(
                tenantId.getValue(),
                chainId.getValue(),
                startTime,
                endTime
        );
    }

    @Override
    public int deleteRecordsBefore(LocalDateTime beforeTime) {
        return executionRecordMapper.deleteRecordsBefore(beforeTime);
    }

    /**
     * 领域对象转实体
     */
    private ExecutionRecordEntity toEntity(ExecutionRecord domain) {
        return ExecutionRecordEntity.builder()
                .id(domain.getId())
                .tenantId(domain.getTenantId().getValue())
                .chainId(domain.getChainId().getValue())
                .componentId(domain.getComponentId())
                .chainExecutionId(domain.getChainExecutionId())
                .executeTime(domain.getExecuteTime())
                .status(domain.getStatus())
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .build();
    }

    /**
     * 实体转领域对象
     */
    private ExecutionRecord toDomain(ExecutionRecordEntity entity) {
        return ExecutionRecord.builder()
                .id(entity.getId())
                .tenantId(TenantId.of(entity.getTenantId()))
                .chainId(ChainId.of(entity.getChainId()))
                .componentId(entity.getComponentId())
                .chainExecutionId(entity.getChainExecutionId())
                .executeTime(entity.getExecuteTime())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
