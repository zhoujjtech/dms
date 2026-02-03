-- Saga 分布式事务支持 - 数据库表
-- Version: V5
-- Description: 创建 Saga 执行、步骤、补偿日志、组件元数据和人工介入记录表

-- 1. Saga 执行实例表
CREATE TABLE IF NOT EXISTS saga_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    execution_id VARCHAR(64) NOT NULL COMMENT '执行ID（全局唯一）',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    chain_name VARCHAR(128) NOT NULL COMMENT '流程链名称',
    status VARCHAR(32) NOT NULL COMMENT '状态：PENDING/RUNNING/COMPLETED/FAILED/COMPENSATING/COMPENSATED/MANUAL_INTERVENTION',
    current_step_index INT DEFAULT 0 COMMENT '当前步骤索引',
    failure_reason TEXT COMMENT '失败原因',
    input_data JSON COMMENT '输入数据（JSON格式）',
    output_data JSON COMMENT '输出数据（JSON格式）',
    execution_stack JSON COMMENT '执行栈（JSON格式，用于补偿）',
    started_at DATETIME NOT NULL COMMENT '开始时间',
    completed_at DATETIME COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    version INT DEFAULT 1 COMMENT '乐观锁版本号',
    UNIQUE KEY uk_execution_id (execution_id),
    INDEX idx_tenant_chain (tenant_id, chain_name),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at),
    INDEX idx_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Saga执行实例表';

-- 2. Saga 节点执行状态表
CREATE TABLE IF NOT EXISTS saga_step_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    execution_id VARCHAR(64) NOT NULL COMMENT '执行ID',
    step_id VARCHAR(64) NOT NULL COMMENT '步骤ID',
    component_name VARCHAR(128) NOT NULL COMMENT '组件名称',
    status VARCHAR(32) NOT NULL COMMENT '状态：RUNNING/COMPLETED/FAILED/SKIPPED',
    input_data JSON COMMENT '输入数据（JSON格式）',
    output_data JSON COMMENT '输出数据（JSON格式）',
    compensate_component VARCHAR(128) COMMENT '补偿组件名称',
    needs_compensation BOOLEAN DEFAULT FALSE COMMENT '是否需要补偿',
    error_code VARCHAR(64) COMMENT '错误码',
    error_message TEXT COMMENT '错误消息',
    stack_trace TEXT COMMENT '异常堆栈',
    executed_at DATETIME COMMENT '执行时间',
    compensated_at DATETIME COMMENT '补偿时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_execution (execution_id),
    INDEX idx_step_id (step_id),
    INDEX idx_component (component_name),
    INDEX idx_status (status),
    CONSTRAINT fk_step_execution FOREIGN KEY (execution_id) REFERENCES saga_execution(execution_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Saga节点执行状态表';

-- 3. Saga 补偿日志表
CREATE TABLE IF NOT EXISTS saga_compensation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    execution_id VARCHAR(64) NOT NULL COMMENT '执行ID',
    step_id VARCHAR(64) NOT NULL COMMENT '步骤ID',
    compensate_component VARCHAR(128) NOT NULL COMMENT '补偿组件名称',
    status VARCHAR(32) NOT NULL COMMENT '状态：SUCCESS/FAILED/SKIPPED',
    error_message TEXT COMMENT '错误消息',
    compensated_at DATETIME NOT NULL COMMENT '补偿时间',
    operator VARCHAR(128) COMMENT '操作人（如果是手动触发）',
    operation_type VARCHAR(32) DEFAULT 'AUTO' COMMENT '操作类型：AUTO/MANUAL',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_execution (execution_id),
    INDEX idx_step_id (step_id),
    INDEX idx_compensated_at (compensated_at),
    INDEX idx_operation_type (operation_type),
    CONSTRAINT fk_compensation_log FOREIGN KEY (execution_id) REFERENCES saga_execution(execution_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Saga补偿日志表';

-- 4. Saga 组件元数据表
CREATE TABLE IF NOT EXISTS saga_component_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    component_name VARCHAR(128) NOT NULL COMMENT '组件名称',
    compensate_component VARCHAR(128) COMMENT '补偿组件名称',
    needs_compensation BOOLEAN DEFAULT FALSE COMMENT '是否需要补偿',
    default_failure_strategy VARCHAR(32) DEFAULT 'AUTO_COMPENSATE' COMMENT '默认失败策略：RETRY/AUTO_COMPENSATE/MANUAL',
    timeout_ms INT DEFAULT 30000 COMMENT '超时时间（毫秒）',
    metadata JSON COMMENT '扩展配置（JSON格式）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_tenant_component (tenant_id, component_name),
    INDEX idx_tenant_id (tenant_id),
    INDEX idx_component (component_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Saga组件元数据表';

-- 5. Saga 人工介入记录表
CREATE TABLE IF NOT EXISTS saga_manual_intervention (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    execution_id VARCHAR(64) NOT NULL COMMENT '执行ID',
    intervention_type VARCHAR(32) NOT NULL COMMENT '介入类型：COMPENSATE/RETRY/SKIP/CONTINUE',
    decision VARCHAR(32) NOT NULL COMMENT '决策：COMPENSATE/RETRY/SKIP/CONTINUE',
    reason TEXT COMMENT '原因',
    operator VARCHAR(128) NOT NULL COMMENT '操作人',
    operated_at DATETIME NOT NULL COMMENT '操作时间',
    input_data JSON COMMENT '修改后的输入数据（可选）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_execution (execution_id),
    INDEX idx_operated_at (operated_at),
    INDEX idx_operator (operator),
    CONSTRAINT fk_manual_intervention FOREIGN KEY (execution_id) REFERENCES saga_execution(execution_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Saga人工介入记录表';
