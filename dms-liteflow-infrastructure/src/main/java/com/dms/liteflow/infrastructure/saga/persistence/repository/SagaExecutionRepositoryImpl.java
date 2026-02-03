package com.dms.liteflow.infrastructure.saga.persistence.repository;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.repository.SagaExecutionRepository;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.saga.persistence.entity.SagaExecutionEntity;
import com.dms.liteflow.infrastructure.saga.persistence.mapper.SagaExecutionMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Saga 执行实例 Repository 实现
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class SagaExecutionRepositoryImpl implements SagaExecutionRepository {

    private final SagaExecutionMapper sagaExecutionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public SagaExecution save(SagaExecution sagaExecution) {
        try {
            SagaExecutionEntity entity = toEntity(sagaExecution);

            if (entity.getId() == null) {
                // 新增
                sagaExecutionMapper.insert(entity);
                sagaExecution.setId(entity.getId());
            } else {
                // 更新
                int updated = sagaExecutionMapper.update(entity);
                if (updated == 0) {
                    throw new RuntimeException("Failed to update SagaExecution, version mismatch");
                }
            }

            return toAggregate(entity);
        } catch (Exception e) {
            log.error("Failed to save SagaExecution: {}", sagaExecution.getExecutionId(), e);
            throw new RuntimeException("Failed to save SagaExecution", e);
        }
    }

    @Override
    public Optional<SagaExecution> findById(Long id) {
        SagaExecutionEntity entity = sagaExecutionMapper.selectById(id);
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public Optional<SagaExecution> findByExecutionId(SagaExecutionId executionId) {
        SagaExecutionEntity entity = sagaExecutionMapper.selectByExecutionId(executionId.getValue());
        return Optional.ofNullable(entity).map(this::toAggregate);
    }

    @Override
    public List<SagaExecution> findByTenantId(TenantId tenantId) {
        List<SagaExecutionEntity> entities = sagaExecutionMapper.selectByTenantId(tenantId.getValue());
        return entities.stream()
                .map(this::toAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<SagaExecution> findByTenantIdAndStatus(TenantId tenantId, SagaStatus status) {
        List<SagaExecutionEntity> entities = sagaExecutionMapper.selectByTenantIdAndStatus(
                tenantId.getValue(), status.name());
        return entities.stream()
                .map(this::toAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public List<SagaExecution> findByStatus(SagaStatus status) {
        List<SagaExecutionEntity> entities = sagaExecutionMapper.selectByStatus(status.name());
        return entities.stream()
                .map(this::toAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateStatus(SagaExecutionId executionId, SagaStatus currentStatus, SagaStatus newStatus, Integer version) {
        int updated = sagaExecutionMapper.updateStatus(
                executionId.getValue(),
                currentStatus.name(),
                newStatus.name(),
                version
        );
        return updated > 0;
    }

    @Override
    public void delete(SagaExecution sagaExecution) {
        if (sagaExecution.getId() != null) {
            sagaExecutionMapper.deleteById(sagaExecution.getId());
        } else {
            sagaExecutionMapper.deleteByExecutionId(sagaExecution.getExecutionId().getValue());
        }
    }

    @Override
    public long countByTenantId(TenantId tenantId) {
        return sagaExecutionMapper.countByTenantId(tenantId.getValue());
    }

    @Override
    public long countByStatus(SagaStatus status) {
        return sagaExecutionMapper.countByStatus(status.name());
    }

    @Override
    public List<SagaExecution> findByTenantId(TenantId tenantId, SagaStatus status,
                                              String chainName, LocalDateTime startTime,
                                              LocalDateTime endTime, int offset, int size) {
        List<SagaExecutionEntity> entities = sagaExecutionMapper.selectByTenantIdWithFilters(
                tenantId.getValue(),
                status != null ? status.name() : null,
                chainName,
                startTime,
                endTime,
                offset,
                size
        );
        return entities.stream()
                .map(this::toAggregate)
                .collect(Collectors.toList());
    }

    @Override
    public int countByTenantId(TenantId tenantId, SagaStatus status,
                              String chainName, LocalDateTime startTime,
                              LocalDateTime endTime) {
        return sagaExecutionMapper.countByTenantIdWithFilters(
                tenantId.getValue(),
                status != null ? status.name() : null,
                chainName,
                startTime,
                endTime
        );
    }

    /**
     * Entity 转换为 Aggregate
     */
    private SagaExecution toAggregate(SagaExecutionEntity entity) {
        if (entity == null) {
            return null;
        }

        try {
            Map<String, Object> inputData = parseJson(entity.getInputData());
            Map<String, Object> outputData = parseJson(entity.getOutputData());
            List<StepExecution> executionStack = parseExecutionStack(entity.getExecutionStack());

            return SagaExecution.builder()
                    .id(entity.getId())
                    .executionId(SagaExecutionId.of(entity.getExecutionId()))
                    .tenantId(TenantId.of(entity.getTenantId()))
                    .chainName(entity.getChainName())
                    .status(SagaStatus.valueOf(entity.getStatus()))
                    .currentStepIndex(entity.getCurrentStepIndex())
                    .failureReason(entity.getFailureReason())
                    .inputData(inputData)
                    .outputData(outputData)
                    .executionStack(executionStack)
                    .startedAt(entity.getStartedAt())
                    .completedAt(entity.getCompletedAt())
                    .createdAt(entity.getCreatedAt())
                    .updatedAt(entity.getUpdatedAt())
                    .version(entity.getVersion())
                    .build();
        } catch (Exception e) {
            log.error("Failed to convert entity to aggregate: {}", entity.getExecutionId(), e);
            throw new RuntimeException("Failed to convert entity to aggregate", e);
        }
    }

    /**
     * Aggregate 转换为 Entity
     */
    private SagaExecutionEntity toEntity(SagaExecution sagaExecution) {
        if (sagaExecution == null) {
            return null;
        }

        try {
            return SagaExecutionEntity.builder()
                    .id(sagaExecution.getId())
                    .executionId(sagaExecution.getExecutionId().getValue())
                    .tenantId(sagaExecution.getTenantId().getValue())
                    .chainName(sagaExecution.getChainName())
                    .status(sagaExecution.getStatus().name())
                    .currentStepIndex(sagaExecution.getCurrentStepIndex())
                    .failureReason(sagaExecution.getFailureReason())
                    .inputData(toJson(sagaExecution.getInputData()))
                    .outputData(toJson(sagaExecution.getOutputData()))
                    .executionStack(toJson(sagaExecution.getExecutionStack()))
                    .startedAt(sagaExecution.getStartedAt())
                    .completedAt(sagaExecution.getCompletedAt())
                    .createdAt(sagaExecution.getCreatedAt())
                    .updatedAt(LocalDateTime.now())
                    .version(sagaExecution.getVersion())
                    .build();
        } catch (Exception e) {
            log.error("Failed to convert aggregate to entity: {}", sagaExecution.getExecutionId(), e);
            throw new RuntimeException("Failed to convert aggregate to entity", e);
        }
    }

    /**
     * 解析 JSON 字符串为 Map
     */
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isEmpty()) {
            return new java.util.HashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON: {}", json, e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * 将 Map 转换为 JSON 字符串
     */
    private String toJson(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize map to JSON: {}", map, e);
            return null;
        }
    }

    /**
     * 解析执行栈 JSON
     */
    private List<StepExecution> parseExecutionStack(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            // 简化处理：这里需要根据实际 JSON 结构解析
            // 实际实现中应该使用 proper JSON 反序列化
            return new ArrayList<>();
        } catch (Exception e) {
            log.warn("Failed to parse execution stack JSON: {}", json, e);
            return new ArrayList<>();
        }
    }
}
