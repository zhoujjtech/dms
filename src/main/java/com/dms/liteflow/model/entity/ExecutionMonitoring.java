package com.dms.liteflow.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 执行监控实体
 */
@Data
@Entity
@Table(name = "execution_monitoring")
public class ExecutionMonitoring {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chain_id")
    private Long chainId;

    @Column(name = "component_id", length = 100)
    private String componentId;

    @Column(name = "chain_execution_id", nullable = false, length = 100)
    private String chainExecutionId;

    @Column(name = "parent_chain_execution_id", length = 100)
    private String parentChainExecutionId;

    @Column(name = "execute_time")
    private Long executeTime;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "output_data", columnDefinition = "TEXT")
    private String outputData;

    @Column(name = "transaction_id", length = 100)
    private String transactionId;

    @Column(name = "trace_path", length = 500)
    private String tracePath;

    @Column(name = "component_order")
    private Integer componentOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
