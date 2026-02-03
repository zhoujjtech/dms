package com.dms.liteflow.domain.saga.repository;

import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.saga.valueobject.StepStatus;

import java.util.List;
import java.util.Optional;

/**
 * Saga 步骤执行 Repository 接口
 *
 * @author DMS
 * @since 2026-02-03
 */
public interface StepExecutionRepository {

    /**
     * 保存或更新 StepExecution
     */
    StepExecution save(StepExecution stepExecution);

    /**
     * 批量保存
     */
    List<StepExecution> saveAll(List<StepExecution> stepExecutions);

    /**
     * 根据 ID 查找
     */
    Optional<StepExecution> findById(Long id);

    /**
     * 根据执行ID 查找所有步骤
     */
    List<StepExecution> findByExecutionId(String executionId);

    /**
     * 根据执行ID 和状态查找
     */
    List<StepExecution> findByExecutionIdAndStatus(String executionId, StepStatus status);

    /**
     * 根据步骤ID 查找
     */
    Optional<StepExecution> findByStepId(StepId stepId);

    /**
     * 根据执行ID 删除所有步骤
     */
    void deleteByExecutionId(String executionId);

    /**
     * 统计执行ID下的步骤数量
     */
    long countByExecutionId(String executionId);
}
