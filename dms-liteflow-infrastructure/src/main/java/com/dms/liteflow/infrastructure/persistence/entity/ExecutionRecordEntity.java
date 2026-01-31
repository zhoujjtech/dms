package com.dms.liteflow.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行记录实体（MyBatis）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecordEntity {

    private Long id;
    private Long tenantId;
    private Long chainId;
    private String componentId;
    private String chainExecutionId;
    private Long executeTime;
    private String status;
    private String errorMessage;
    private LocalDateTime createdAt;
}
