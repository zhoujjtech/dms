package com.dms.liteflow.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 测试用例实体
 */
@Data
@Entity
@Table(name = "config_test_case")
public class ConfigTestCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_type", nullable = false, length = 50)
    private String configType;

    @Column(name = "config_id", nullable = false)
    private Long configId;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "input_data", columnDefinition = "TEXT")
    private String inputData;

    @Column(name = "expected_result", columnDefinition = "TEXT")
    private String expectedResult;

    @Column(name = "version_id")
    private Long versionId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
