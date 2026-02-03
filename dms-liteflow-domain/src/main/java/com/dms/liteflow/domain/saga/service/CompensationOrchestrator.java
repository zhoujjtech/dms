package com.dms.liteflow.domain.saga.service;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.StepId;

import java.util.List;

/**
 * Saga 补偿编排器接口
 *
 * @author DMS
 * @since 2026-02-03
 */
public interface CompensationOrchestrator {

    /**
     * 执行补偿流程
     *
     * @param executionId 执行ID
     * @return 是否全部补偿成功
     */
    boolean compensate(SagaExecutionId executionId);

    /**
     * 补偿单个步骤
     *
     * @param executionId 执行ID
     * @param stepExecution 步骤执行记录
     * @return 是否补偿成功
     */
    boolean compensateStep(SagaExecutionId executionId, StepExecution stepExecution);

    /**
     * 检查是否需要补偿
     *
     * @param stepExecution 步骤执行记录
     * @return 是否需要补偿
     */
    boolean checkNeedsCompensation(StepExecution stepExecution);

    /**
     * 获取需要补偿的步骤列表
     *
     * @param executionId 执行ID
     * @return 需要补偿的步骤列表（按相反顺序）
     */
    List<StepExecution> getCompensatableSteps(SagaExecutionId executionId);

    /**
     * 手动触发补偿（人工介入）
     *
     * @param executionId 执行ID
     * @param operator 操作人
     * @return 是否全部补偿成功
     */
    boolean manualCompensate(SagaExecutionId executionId, String operator);

    /**
     * 重试单个步骤的补偿
     *
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @return 是否补偿成功
     */
    boolean retryCompensation(SagaExecutionId executionId, StepId stepId);
}
