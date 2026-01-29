package com.dms.liteflow.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 子流程实体
 */
@Data
@Entity
@Table(name = "flow_sub_chain")
public class FlowSubChain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_chain_name", nullable = false, length = 200)
    private String subChainName;

    @Column(name = "chain_code", nullable = false, unique = true, length = 100)
    private String chainCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "parent_chain_id")
    private Long parentChainId;

    @Column(name = "config_content", columnDefinition = "TEXT")
    private String configContent;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
