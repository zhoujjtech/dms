package com.dms.liteflow.domain.monitoring.repository;

import com.dms.liteflow.domain.monitoring.aggregate.ExecutionRecord;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行监控仓储接口
 */
public interface ExecutionRecordRepository {

    /**
     * 保存执行记录
     *
     * @param record 执行记录聚合根
     * @return 保存后的记录
     */
    ExecutionRecord save(ExecutionRecord record);

    /**
     * 批量保存执行记录
     *
     * @param records 执行记录列表
     */
    void saveAll(List<ExecutionRecord> records);

    /**
     * 根据执行ID查找记录
     *
     * @param executionId 执行ID
     * @return 执行记录列表
     */
    List<ExecutionRecord> findByExecutionId(String executionId);

    /**
     * 根据流程链ID和时间段查找记录
     *
     * @param chainId    流程链ID
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 执行记录列表
     */
    List<ExecutionRecord> findByChainIdAndTimeRange(
            ChainId chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 根据租户ID和时间段查找记录
     *
     * @param tenantId  租户ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 执行记录列表
     */
    List<ExecutionRecord> findByTenantIdAndTimeRange(
            TenantId tenantId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 统计流程链的执行次数
     *
     * @param tenantId 租户ID
     * @param chainId  流程链ID
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 执行次数
     */
    long countExecutions(
            TenantId tenantId,
            ChainId chainId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    /**
     * 删除过期记录
     *
     * @param beforeTime 时间阈值
     * @return 删除的记录数
     */
    int deleteRecordsBefore(LocalDateTime beforeTime);
}
