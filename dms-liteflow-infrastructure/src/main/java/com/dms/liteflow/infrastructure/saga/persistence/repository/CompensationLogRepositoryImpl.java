package com.dms.liteflow.infrastructure.saga.persistence.repository;

import com.dms.liteflow.domain.saga.repository.CompensationLogRepository;
import com.dms.liteflow.domain.saga.valueobject.CompensationLog;
import com.dms.liteflow.infrastructure.saga.persistence.entity.CompensationLogEntity;
import com.dms.liteflow.infrastructure.saga.persistence.mapper.CompensationLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Saga 补偿日志 Repository 实现
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CompensationLogRepositoryImpl implements CompensationLogRepository {

    private final CompensationLogMapper compensationLogMapper;

    @Override
    public CompensationLog save(CompensationLog compensationLog) {
        CompensationLogEntity entity = toEntity(compensationLog);
        compensationLogMapper.insert(entity);
        return compensationLog;
    }

    @Override
    public List<CompensationLog> saveAll(List<CompensationLog> compensationLogs) {
        List<CompensationLogEntity> entities = compensationLogs.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        compensationLogMapper.insertBatch(entities);
        return compensationLogs;
    }

    @Override
    public List<CompensationLog> findByExecutionId(String executionId) {
        List<CompensationLogEntity> entities = compensationLogMapper.selectByExecutionId(executionId);
        return entities.stream().map(this::toValueObject).collect(Collectors.toList());
    }

    @Override
    public List<CompensationLog> findByExecutionIdAndStepId(String executionId, String stepId) {
        List<CompensationLogEntity> entities = compensationLogMapper.selectByExecutionIdAndStepId(executionId, stepId);
        return entities.stream().map(this::toValueObject).collect(Collectors.toList());
    }

    @Override
    public List<CompensationLog> findByCompensatedAtBetween(LocalDateTime startTime, LocalDateTime endTime) {
        List<CompensationLogEntity> entities = compensationLogMapper.selectByCompensatedAtBetween(startTime, endTime);
        return entities.stream().map(this::toValueObject).collect(Collectors.toList());
    }

    @Override
    public List<CompensationLog> findByOperator(String operator) {
        List<CompensationLogEntity> entities = compensationLogMapper.selectByOperator(operator);
        return entities.stream().map(this::toValueObject).collect(Collectors.toList());
    }

    @Override
    public void deleteByExecutionId(String executionId) {
        compensationLogMapper.deleteByExecutionId(executionId);
    }

    private CompensationLogEntity toEntity(CompensationLog log) {
        return CompensationLogEntity.builder()
                .executionId(log.getExecutionId())
                .stepId(log.getStepId())
                .compensateComponent(log.getCompensateComponent())
                .status(log.getStatus())
                .errorMessage(log.getErrorMessage())
                .compensatedAt(log.getCompensatedAt())
                .operator(log.getOperator())
                .operationType(log.getOperationType())
                .createdAt(log.getCompensatedAt())
                .build();
    }

    private CompensationLog toValueObject(CompensationLogEntity entity) {
        return CompensationLog.builder()
                .executionId(entity.getExecutionId())
                .stepId(entity.getStepId())
                .compensateComponent(entity.getCompensateComponent())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .compensatedAt(entity.getCompensatedAt())
                .operator(entity.getOperator())
                .operationType(entity.getOperationType())
                .build();
    }
}
