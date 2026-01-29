package com.dms.liteflow.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 规则组件实体
 */
@Data
@Entity
@Table(name = "rule_component")
public class RuleComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "component_id", nullable = false, unique = true, length = 100)
    private String componentId;

    @Column(name = "component_name", nullable = false, length = 200)
    private String componentName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "component_type", nullable = false, length = 50)
    private String componentType;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "transactional_type", length = 20)
    private String transactionalType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
