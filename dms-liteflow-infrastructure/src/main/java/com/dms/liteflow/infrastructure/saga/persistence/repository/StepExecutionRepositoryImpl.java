package com.dms.liteflow.infrastructure.saga.persistence.repository;

import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.repository.StepExecutionRepository;
import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.saga.valueobject.StepStatus;
import com.dms.liteflow.infrastructure.saga.persistence.entity.StepExecutionEntity;
import com.dms.liteflow.infrastructure.saga.persistence.mapper.StepExecutionMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Saga 步骤执行 Repository 实现
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class StepExecutionRepositoryImpl implements StepExecutionRepository {

    private final StepExecutionMapper stepExecutionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public StepExecution save(StepExecution stepExecution) {
        StepExecutionEntity entity = toEntity(stepExecution);
        if (entity.getId() == null) {
            stepExecutionMapper.insert(entity);
            stepExecution.setId(entity.getId());
        } else {
            stepExecutionMapper.update(entity);
        }
        return toAggregate(entity);
    }

    @Override
    public List<StepExecution> saveAll(List<StepExecution> stepExecutions) {
        List<StepExecutionEntity> entities = stepExecutions.stream()
                .map(this::toEntity)
                .collect(Collectors.toList());
        stepExecutionMapper.insertBatch(entities);
        return stepExecutions;
    }

    @Override
    public Optional<StepExecution> findById(Long id) {
        StepExecutionEntity entity = stepExecutionMapper.selectById(id);
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public List<StepExecution> findByExecutionId(String executionId) {
        List<StepExecutionEntity> entities = stepExecutionMapper.selectByExecutionId(executionId);
        return entities.stream().map(this::toAggregate).collect(Collectors.toList());
    }

    @Override
    public List<StepExecution> findByExecutionIdAndStatus(String executionId, StepStatus status) {
        List<StepExecutionEntity> entities = stepExecutionMapper.selectByExecutionIdAndStatus(executionId, status.name());
        return entities.stream().map(this::toAggregate).collect(Collectors.toList());
    }

    @Override
    public Optional<StepExecution> findByStepId(StepId stepId) {
        StepExecutionEntity entity = stepExecutionMapper.selectByStepId(stepId.getValue());
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public void deleteByExecutionId(String executionId) {
        stepExecutionMapper.deleteByExecutionId(executionId);
    }

    @Override
    public long countByExecutionId(String executionId) {
        return stepExecutionMapper.countByExecutionId(executionId);
    }

    private StepExecution toAggregate(StepExecutionEntity entity) {
        // 实现转换逻辑
        return null; // 简化版本
    }

    private StepExecutionEntity toEntity(StepExecution stepExecution) {
        // 实现转换逻辑
        return null; // 简化版本
    }
}
