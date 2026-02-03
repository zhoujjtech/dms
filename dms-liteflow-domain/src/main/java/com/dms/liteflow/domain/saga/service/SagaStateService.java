package com.dms.liteflow.domain.saga.service;

import com.dms.liteflow.domain.saga.aggregate.SagaExecution;
import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.saga.valueobject.StepId;

import java.util.List;
import java.util.Map;

/**
 * Saga 状态管理服务接口
 *
 * @author DMS
 * @since 2026-02-03
 */
public interface SagaStateService {

    /**
     * 记录节点开始执行
     *
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param componentName 组件名称
     * @param inputData 输入数据
     */
    void recordStepStart(SagaExecutionId executionId, StepId stepId, String componentName, Map<String, Object> inputData);

    /**
     * 记录节点成功完成
     *
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param outputData 输出数据
     */
    void recordStepSuccess(SagaExecutionId executionId, StepId stepId, Map<String, Object> outputData);

    /**
     * 记录节点失败
     *
     * @param executionId 执行ID
     * @param stepId 步骤ID
     * @param errorCode 错误码
     * @param errorMessage 错误消息
     * @param stackTrace 异常堆栈
     */
    void recordStepFailure(SagaExecutionId executionId, StepId stepId, String errorCode, String errorMessage, String stackTrace);

    /**
     * 获取执行栈（用于补偿）
     *
     * @param executionId 执行ID
     * @return 执行栈（按相反顺序）
     */
    List<StepExecution> getExecutionStack(SagaExecutionId executionId);

    /**
     * 更新 Saga 状态
     *
     * @param executionId 执行ID
     * @param currentStatus 当前状态
     * @param newStatus 新状态
     * @return 是否更新成功
     */
    boolean updateStatus(SagaExecutionId executionId, SagaStatus currentStatus, SagaStatus newStatus);

    /**
     * 获取 SagaExecution
     *
     * @param executionId 执行ID
     * @return SagaExecution
     */
    SagaExecution getExecution(SagaExecutionId executionId);

    /**
     * 保存 SagaExecution
     *
     * @param sagaExecution SagaExecution
     * @return 保存后的 SagaExecution
     */
    SagaExecution saveExecution(SagaExecution sagaExecution);

    /**
     * 删除 SagaExecution
     *
     * @param executionId 执行ID
     */
    void deleteExecution(SagaExecutionId executionId);

    /**
     * 将节点加入执行栈
     *
     * @param executionId 执行ID
     * @param stepExecution 步骤执行记录
     */
    void pushToStack(SagaExecutionId executionId, StepExecution stepExecution);

    /**
     * 从执行栈弹出节点
     *
     * @param executionId 执行ID
     * @return 步骤执行记录
     */
    StepExecution popFromStack(SagaExecutionId executionId);
}
