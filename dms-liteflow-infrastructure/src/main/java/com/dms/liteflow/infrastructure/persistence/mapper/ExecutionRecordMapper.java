package com.dms.liteflow.infrastructure.persistence.mapper;

import com.dms.liteflow.infrastructure.persistence.entity.ExecutionRecordEntity;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 执行记录 Mapper 接口
 */
public interface ExecutionRecordMapper {

    /**
     * 插入执行记录
     */
    int insert(ExecutionRecordEntity entity);

    /**
     * 根据ID更新
     */
    int updateById(ExecutionRecordEntity entity);

    /**
     * 批量插入执行记录
     */
    int insertBatch(@Param("entities") List<ExecutionRecordEntity> entities);

    /**
     * 根据执行ID查询
     */
    List<ExecutionRecordEntity> selectByExecutionId(@Param("executionId") String executionId);

    /**
     * 根据流程链ID和时间段查询
     */
    List<ExecutionRecordEntity> selectByChainIdAndTimeRange(
            @Param("chainId") Long chainId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据租户ID和时间段查询
     */
    List<ExecutionRecordEntity> selectByTenantIdAndTimeRange(
            @Param("tenantId") Long tenantId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 统计执行次数
     */
    long countExecutions(
            @Param("tenantId") Long tenantId,
            @Param("chainId") Long chainId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 删除过期记录
     */
    int deleteRecordsBefore(@Param("beforeTime") LocalDateTime beforeTime);
}
