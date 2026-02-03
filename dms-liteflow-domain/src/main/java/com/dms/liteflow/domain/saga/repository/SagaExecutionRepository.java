package com.dms.liteflow.domain.saga.repository;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Saga 执行实例 Repository 接口
 *
 * @author DMS
 * @since 2026-02-03
 */
public interface SagaExecutionRepository {

    /**
     * 保存或更新 SagaExecution
     */
    SagaExecution save(SagaExecution sagaExecution);

    /**
     * 根据 ID 查找
     */
    Optional<SagaExecution> findById(Long id);

    /**
     * 根据执行ID 查找
     */
    Optional<SagaExecution> findByExecutionId(SagaExecutionId executionId);

    /**
     * 根据租户ID 查找
     */
    List<SagaExecution> findByTenantId(TenantId tenantId);

    /**
     * 根据租户ID 和状态查找
     */
    List<SagaExecution> findByTenantIdAndStatus(TenantId tenantId, SagaStatus status);

    /**
     * 根据状态查找
     */
    List<SagaExecution> findByStatus(SagaStatus status);

    /**
     * 更新状态（乐观锁）
     */
    boolean updateStatus(SagaExecutionId executionId, SagaStatus currentStatus, SagaStatus newStatus, Integer version);

    /**
     * 删除
     */
    void delete(SagaExecution sagaExecution);

    /**
     * 统计租户下的执行数量
     */
    long countByTenantId(TenantId tenantId);

    /**
     * 统计指定状态的执行数量
     */
    long countByStatus(SagaStatus status);

    /**
     * 根据租户ID和条件查找（分页）
     */
    List<SagaExecution> findByTenantId(TenantId tenantId, SagaStatus status,
                                       String chainName, LocalDateTime startTime,
                                       LocalDateTime endTime, int offset, int size);

    /**
     * 统计租户下的执行数量（带条件）
     */
    int countByTenantId(TenantId tenantId, SagaStatus status,
                       String chainName, LocalDateTime startTime,
                       LocalDateTime endTime);
}
