-- ============================================
-- DMS LiteFlow 数据库表结构
-- ============================================

-- 1. 规则组件表
CREATE TABLE IF NOT EXISTS `rule_component` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `component_id` VARCHAR(100) NOT NULL UNIQUE COMMENT '组件ID（唯一标识）',
    `component_name` VARCHAR(200) NOT NULL COMMENT '组件名称',
    `description` TEXT COMMENT '组件描述',
    `component_type` VARCHAR(50) NOT NULL DEFAULT 'BUSINESS' COMMENT '组件类型：BUSINESS/CONDITION/LOOP',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `transactional_type` VARCHAR(20) DEFAULT 'REQUIRED' COMMENT '事务类型：REQUIRED/REQUIRES_NEW/NOT_SUPPORTED/SUPPORTS',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_component_id` (`component_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_component_type` (`component_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则组件表';

-- 2. 流程链表
CREATE TABLE IF NOT EXISTS `flow_chain` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `chain_name` VARCHAR(200) NOT NULL COMMENT '流程链名称',
    `chain_code` VARCHAR(100) NOT NULL UNIQUE COMMENT '流程链代码（唯一标识）',
    `description` TEXT COMMENT '流程链描述',
    `config_type` VARCHAR(50) NOT NULL DEFAULT 'DYNAMIC' COMMENT '配置类型：XML/DYNAMIC',
    `config_content` TEXT COMMENT '流程链配置内容（EL表达式或XML）',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `transactional` TINYINT DEFAULT 0 COMMENT '是否启用流程级事务：0-否 1-是',
    `transaction_timeout` INT DEFAULT 30 COMMENT '事务超时时间（秒）',
    `transaction_propagation` VARCHAR(20) DEFAULT 'REQUIRED' COMMENT '事务传播行为',
    `current_version` VARCHAR(50) COMMENT '当前版本号',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
    INDEX `idx_chain_code` (`chain_code`),
    INDEX `idx_status` (`status`),
    INDEX `idx_config_type` (`config_type`),
    INDEX `idx_deleted_at` (`deleted_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程链表';

-- 3. 子流程表
CREATE TABLE IF NOT EXISTS `flow_sub_chain` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `sub_chain_name` VARCHAR(200) NOT NULL COMMENT '子流程名称',
    `chain_code` VARCHAR(100) NOT NULL UNIQUE COMMENT '子流程代码（唯一标识）',
    `description` TEXT COMMENT '子流程描述',
    `parent_chain_id` BIGINT COMMENT '父流程链ID',
    `config_content` TEXT COMMENT '子流程配置内容',
    `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-启用',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted_at` DATETIME DEFAULT NULL COMMENT '软删除时间',
    INDEX `idx_chain_code` (`chain_code`),
    INDEX `idx_parent_chain_id` (`parent_chain_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_deleted_at` (`deleted_at`),
    FOREIGN KEY (`parent_chain_id`) REFERENCES `flow_chain`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='子流程表';

-- 4. 配置版本表
CREATE TABLE IF NOT EXISTS `config_version` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型：COMPONENT/CHAIN/SUB_CHAIN',
    `config_id` BIGINT NOT NULL COMMENT '配置ID（关联到对应表的ID）',
    `version` VARCHAR(50) NOT NULL COMMENT '版本号（语义化版本）',
    `content` TEXT NOT NULL COMMENT '版本内容',
    `status` VARCHAR(20) NOT NULL DEFAULT 'DRAFT' COMMENT '版本状态：DRAFT/PUBLISHED/DEPRECATED',
    `change_log` TEXT COMMENT '变更日志',
    `created_by` VARCHAR(100) COMMENT '创建人',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_config_type_id` (`config_type`, `config_id`),
    INDEX `idx_version` (`version`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置版本表';

-- 5. 测试用例表
CREATE TABLE IF NOT EXISTS `config_test_case` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `config_type` VARCHAR(50) NOT NULL COMMENT '配置类型：COMPONENT/CHAIN/SUB_CHAIN',
    `config_id` BIGINT NOT NULL COMMENT '配置ID',
    `name` VARCHAR(200) NOT NULL COMMENT '测试用例名称',
    `input_data` TEXT COMMENT '输入数据（JSON格式）',
    `expected_result` TEXT COMMENT '预期结果（JSON格式）',
    `version_id` BIGINT COMMENT '关联的版本ID',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX `idx_config_type_id` (`config_type`, `config_id`),
    INDEX `idx_version_id` (`version_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';

-- 6. 执行监控表
CREATE TABLE IF NOT EXISTS `execution_monitoring` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `chain_id` BIGINT COMMENT '流程链ID',
    `component_id` VARCHAR(100) COMMENT '组件ID',
    `chain_execution_id` VARCHAR(100) NOT NULL COMMENT '流程执行ID',
    `parent_chain_execution_id` VARCHAR(100) COMMENT '父流程执行ID',
    `execute_time` BIGINT COMMENT '执行耗时（毫秒）',
    `status` VARCHAR(20) NOT NULL COMMENT '执行状态：SUCCESS/FAILED/TIMEOUT',
    `error_message` TEXT COMMENT '错误信息',
    `input_data` TEXT COMMENT '输入数据',
    `output_data` TEXT COMMENT '输出数据',
    `transaction_id` VARCHAR(100) COMMENT '事务ID',
    `trace_path` VARCHAR(500) COMMENT '执行路径',
    `component_order` INT COMMENT '组件执行顺序',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    INDEX `idx_chain_execution_id` (`chain_execution_id`),
    INDEX `idx_chain_id` (`chain_id`),
    INDEX `idx_component_id` (`component_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行监控表';

-- 7. 小时级统计表
CREATE TABLE IF NOT EXISTS `monitoring_hourly_stats` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `chain_id` BIGINT COMMENT '流程链ID',
    `component_id` VARCHAR(100) COMMENT '组件ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `stat_hour` TINYINT NOT NULL COMMENT '统计小时（0-23）',
    `execute_count` INT DEFAULT 0 COMMENT '执行次数',
    `success_count` INT DEFAULT 0 COMMENT '成功次数',
    `failure_count` INT DEFAULT 0 COMMENT '失败次数',
    `total_execute_time` BIGINT DEFAULT 0 COMMENT '总执行耗时（毫秒）',
    `avg_execute_time` BIGINT DEFAULT 0 COMMENT '平均执行耗时（毫秒）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_chain_id` (`chain_id`),
    INDEX `idx_component_id` (`component_id`),
    INDEX `idx_stat_date_hour` (`stat_date`, `stat_hour`),
    UNIQUE KEY `uk_chain_component_hour` (`chain_id`, `component_id`, `stat_date`, `stat_hour`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小时级统计表';

-- 8. 日级统计表
CREATE TABLE IF NOT EXISTS `monitoring_daily_stats` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `chain_id` BIGINT COMMENT '流程链ID',
    `component_id` VARCHAR(100) COMMENT '组件ID',
    `stat_date` DATE NOT NULL COMMENT '统计日期',
    `execute_count` INT DEFAULT 0 COMMENT '执行次数',
    `success_count` INT DEFAULT 0 COMMENT '成功次数',
    `failure_count` INT DEFAULT 0 COMMENT '失败次数',
    `success_rate` DECIMAL(5,2) DEFAULT 0.00 COMMENT '成功率（百分比）',
    `total_execute_time` BIGINT DEFAULT 0 COMMENT '总执行耗时（毫秒）',
    `avg_execute_time` BIGINT DEFAULT 0 COMMENT '平均执行耗时（毫秒）',
    `min_execute_time` BIGINT COMMENT '最小执行耗时（毫秒）',
    `max_execute_time` BIGINT COMMENT '最大执行耗时（毫秒）',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_chain_id` (`chain_id`),
    INDEX `idx_component_id` (`component_id`),
    INDEX `idx_stat_date` (`stat_date`),
    UNIQUE KEY `uk_chain_component_date` (`chain_id`, `component_id`, `stat_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日级统计表';

-- 9. 告警记录表
CREATE TABLE IF NOT EXISTS `alert_log` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `alert_type` VARCHAR(50) NOT NULL COMMENT '告警类型：FAILURE_RATE/SLOW_TRANSACTION/COMPONENT_ERROR',
    `chain_id` BIGINT COMMENT '流程链ID',
    `component_id` VARCHAR(100) COMMENT '组件ID',
    `alert_level` VARCHAR(20) NOT NULL COMMENT '告警级别：INFO/WARN/ERROR/CRITICAL',
    `alert_message` TEXT COMMENT '告警消息',
    `alert_value` DECIMAL(10,2) COMMENT '告警值',
    `threshold_value` DECIMAL(10,2) COMMENT '阈值',
    `notification_sent` TINYINT DEFAULT 0 COMMENT '是否已发送通知：0-否 1-是',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_chain_id` (`chain_id`),
    INDEX `idx_component_id` (`component_id`),
    INDEX `idx_alert_type` (`alert_type`),
    INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';
