package com.dms.liteflow.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 流程链实体
 */
@Data
@Entity
@Table(name = "flow_chain")
public class FlowChain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chain_name", nullable = false, length = 200)
    private String chainName;

    @Column(name = "chain_code", nullable = false, unique = true, length = 100)
    private String chainCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "config_type", nullable = false, length = 50)
    private String configType;

    @Column(name = "config_content", columnDefinition = "TEXT")
    private String configContent;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "transactional")
    private Integer transactional;

    @Column(name = "transaction_timeout")
    private Integer transactionTimeout;

    @Column(name = "transaction_propagation", length = 20)
    private String transactionPropagation;

    @Column(name = "current_version", length = 50)
    private String currentVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
