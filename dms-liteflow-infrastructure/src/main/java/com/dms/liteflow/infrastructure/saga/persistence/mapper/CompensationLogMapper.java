package com.dms.liteflow.infrastructure.saga.persistence.mapper;

import com.dms.liteflow.infrastructure.saga.persistence.entity.CompensationLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 补偿日志 Mapper
 *
 * @author DMS
 * @since 2026-02-03
 */
@Mapper
public interface CompensationLogMapper {

    /**
     * 插入
     */
    int insert(CompensationLogEntity entity);

    /**
     * 批量插入
     */
    int insertBatch(@Param("list") List<CompensationLogEntity> list);

    /**
     * 根据 ID 查询
     */
    CompensationLogEntity selectById(@Param("id") Long id);

    /**
     * 根据执行ID 查询补偿日志
     */
    List<CompensationLogEntity> selectByExecutionId(@Param("executionId") String executionId);

    /**
     * 根据执行ID 和步骤ID 查询
     */
    List<CompensationLogEntity> selectByExecutionIdAndStepId(
            @Param("executionId") String executionId,
            @Param("stepId") String stepId
    );

    /**
     * 根据时间范围查找补偿日志
     */
    List<CompensationLogEntity> selectByCompensatedAtBetween(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 根据操作人查找
     */
    List<CompensationLogEntity> selectByOperator(@Param("operator") String operator);

    /**
     * 统计执行失败的数量
     */
    long countFailedByExecutionId(@Param("executionId") String executionId);

    /**
     * 删除执行ID的所有补偿日志
     */
    int deleteByExecutionId(@Param("executionId") String executionId);

    /**
     * 删除过期日志
     */
    int deleteExpired(@Param("beforeTime") LocalDateTime beforeTime);
}
