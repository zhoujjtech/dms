package com.dms.liteflow.domain.saga.repository;

import com.dms.liteflow.domain.saga.valueobject.CompensationLog;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 补偿日志 Repository 接口
 *
 * @author DMS
 * @since 2026-02-03
 */
public interface CompensationLogRepository {

    /**
     * 保存补偿日志
     */
    CompensationLog save(CompensationLog compensationLog);

    /**
     * 批量保存
     */
    List<CompensationLog> saveAll(List<CompensationLog> compensationLogs);

    /**
     * 根据执行ID 查找补偿日志
     */
    List<CompensationLog> findByExecutionId(String executionId);

    /**
     * 根据执行ID 和步骤ID 查找
     */
    List<CompensationLog> findByExecutionIdAndStepId(String executionId, String stepId);

    /**
     * 根据时间范围查找补偿日志
     */
    List<CompensationLog> findByCompensatedAtBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据操作人查找
     */
    List<CompensationLog> findByOperator(String operator);

    /**
     * 删除执行ID的所有补偿日志
     */
    void deleteByExecutionId(String executionId);
}
