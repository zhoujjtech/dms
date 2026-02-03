package com.dms.liteflow.infrastructure.saga.persistence.mapper;

import com.dms.liteflow.infrastructure.saga.persistence.entity.StepExecutionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Saga 步骤执行 Mapper
 *
 * @author DMS
 * @since 2026-02-03
 */
@Mapper
public interface StepExecutionMapper {

    /**
     * 插入
     */
    int insert(StepExecutionEntity entity);

    /**
     * 批量插入
     */
    int insertBatch(@Param("list") List<StepExecutionEntity> list);

    /**
     * 根据 ID 查询
     */
    StepExecutionEntity selectById(@Param("id") Long id);

    /**
     * 根据执行ID 查询所有步骤
     */
    List<StepExecutionEntity> selectByExecutionId(@Param("executionId") String executionId);

    /**
     * 根据执行ID 和状态查询
     */
    List<StepExecutionEntity> selectByExecutionIdAndStatus(
            @Param("executionId") String executionId,
            @Param("status") String status
    );

    /**
     * 根据步骤ID 查询
     */
    StepExecutionEntity selectByStepId(@Param("stepId") String stepId);

    /**
     * 更新
     */
    int update(StepExecutionEntity entity);

    /**
     * 更新步骤状态
     */
    int updateStatus(
            @Param("stepId") String stepId,
            @Param("status") String status
    );

    /**
     * 更新补偿时间
     */
    int updateCompensatedAt(
            @Param("stepId") String stepId,
            @Param("compensatedAt") java.time.LocalDateTime compensatedAt
    );

    /**
     * 根据执行ID 删除所有步骤
     */
    int deleteByExecutionId(@Param("executionId") String executionId);

    /**
     * 根据步骤ID 删除
     */
    int deleteByStepId(@Param("stepId") String stepId);

    /**
     * 统计执行ID下的步骤数量
     */
    long countByExecutionId(@Param("executionId") String executionId);

    /**
     * 查询需要补偿的步骤
     */
    List<StepExecutionEntity> selectCompensatableSteps(@Param("executionId") String executionId);
}
