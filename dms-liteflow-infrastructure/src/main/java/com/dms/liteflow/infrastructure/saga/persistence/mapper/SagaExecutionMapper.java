package com.dms.liteflow.infrastructure.saga.persistence.mapper;

import com.dms.liteflow.infrastructure.saga.persistence.entity.SagaExecutionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 执行实例 Mapper
 *
 * @author DMS
 * @since 2026-02-03
 */
@Mapper
public interface SagaExecutionMapper {

    /**
     * 插入
     */
    int insert(SagaExecutionEntity entity);

    /**
     * 根据 ID 查询
     */
    SagaExecutionEntity selectById(@Param("id") Long id);

    /**
     * 根据执行ID 查询
     */
    SagaExecutionEntity selectByExecutionId(@Param("executionId") String executionId);

    /**
     * 根据租户ID 查询
     */
    List<SagaExecutionEntity> selectByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 根据租户ID 和状态查询
     */
    List<SagaExecutionEntity> selectByTenantIdAndStatus(
            @Param("tenantId") Long tenantId,
            @Param("status") String status
    );

    /**
     * 根据状态查询
     */
    List<SagaExecutionEntity> selectByStatus(@Param("status") String status);

    /**
     * 更新状态（乐观锁）
     */
    int updateStatus(
            @Param("executionId") String executionId,
            @Param("currentStatus") String currentStatus,
            @Param("newStatus") String newStatus,
            @Param("version") Integer version
    );

    /**
     * 更新
     */
    int update(SagaExecutionEntity entity);

    /**
     * 根据 ID 删除
     */
    int deleteById(@Param("id") Long id);

    /**
     * 根据执行ID 删除
     */
    int deleteByExecutionId(@Param("executionId") String executionId);

    /**
     * 统计租户下的执行数量
     */
    long countByTenantId(@Param("tenantId") Long tenantId);

    /**
     * 统计指定状态的执行数量
     */
    long countByStatus(@Param("status") String status);

    /**
     * 查询需要清理的过期记录
     */
    List<SagaExecutionEntity> selectExpiredForCleanup(
            @Param("status") String status,
            @Param("beforeTime") LocalDateTime beforeTime
    );

    /**
     * 分页查询
     */
    List<SagaExecutionEntity> selectByPage(
            @Param("tenantId") Long tenantId,
            @Param("chainName") String chainName,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("offset") Integer offset,
            @Param("limit") Integer limit
    );

    /**
     * 根据租户ID和条件查询（分页）
     */
    List<SagaExecutionEntity> selectByTenantIdWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("status") String status,
            @Param("chainName") String chainName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("offset") Integer offset,
            @Param("size") Integer size
    );

    /**
     * 统计租户下的执行数量（带条件）
     */
    int countByTenantIdWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("status") String status,
            @Param("chainName") String chainName,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
