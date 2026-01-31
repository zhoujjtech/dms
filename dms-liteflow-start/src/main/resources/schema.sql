-- ============================================
-- DMS LiteFlow 数据库初始化脚本
-- DDD 多租户架构
-- ============================================

CREATE DATABASE IF NOT EXISTS dms_liteflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE dms_liteflow;

-- ============================================
-- 1. 租户信息表 (Tenant Bounded Context)
-- ============================================
CREATE TABLE IF NOT EXISTS tenant_info (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_code VARCHAR(50) UNIQUE NOT NULL COMMENT '租户编码',
    tenant_name VARCHAR(100) NOT NULL COMMENT '租户名称',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED/DELETED',
    max_chains INT DEFAULT 100 COMMENT '最大流程链数量',
    max_components INT DEFAULT 500 COMMENT '最大组件数量',
    executor_cached TINYINT DEFAULT 0 COMMENT 'Executor是否已缓存',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间',
    INDEX idx_tenant_code(tenant_code),
    INDEX idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租户信息表';

-- ============================================
-- 2. 规则组件表 (Rule Config Bounded Context)
-- ============================================
CREATE TABLE IF NOT EXISTS rule_component (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    component_id VARCHAR(50) NOT NULL COMMENT '组件ID',
    component_name VARCHAR(100) NOT NULL COMMENT '组件名称',
    description TEXT COMMENT '组件描述',
    component_type VARCHAR(20) NOT NULL COMMENT '组件类型: BUSINESS/CONDITION/LOOP',
    content TEXT NOT NULL COMMENT '组件内容（Java代码/脚本）',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/ARCHIVED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_component_id(component_id),
    INDEX idx_tenant_component(tenant_id, component_id),
    INDEX idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='规则组件表';

-- ============================================
-- 3. 流程链表 (Flow Exec Bounded Context)
-- ============================================
CREATE TABLE IF NOT EXISTS flow_chain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    chain_name VARCHAR(100) NOT NULL COMMENT '流程链名称',
    chain_code TEXT NOT NULL COMMENT 'EL表达式',
    description TEXT COMMENT '流程链描述',
    config_type VARCHAR(20) NOT NULL COMMENT '配置类型: XML/DATABASE',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/ARCHIVED',
    current_version INT DEFAULT 1 COMMENT '当前版本号',
    transactional TINYINT DEFAULT 0 COMMENT '是否启用流程级事务: 0-否 1-是',
    transaction_timeout INT DEFAULT 30 COMMENT '事务超时时间(秒)',
    transaction_propagation VARCHAR(20) DEFAULT 'REQUIRED' COMMENT '事务传播行为',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted_at TIMESTAMP NULL COMMENT '删除时间（软删除）',
    UNIQUE KEY uk_tenant_chain(tenant_id, chain_name),
    INDEX idx_tenant_id(tenant_id),
    INDEX idx_status(status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='流程链表';

-- ============================================
-- 4. 子流程表 (Flow Exec Bounded Context)
-- ============================================
CREATE TABLE IF NOT EXISTS flow_sub_chain (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    sub_chain_name VARCHAR(100) NOT NULL COMMENT '子流程名称',
    chain_code TEXT NOT NULL COMMENT 'EL表达式',
    description TEXT COMMENT '子流程描述',
    parent_chain_id BIGINT COMMENT '父流程链ID',
    status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/INACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_tenant_subchain(tenant_id, sub_chain_name),
    INDEX idx_parent_chain(parent_chain_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='子流程表';

-- ============================================
-- 5. 配置版本表 (Version Bounded Context)
-- ============================================
CREATE TABLE IF NOT EXISTS config_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    config_type VARCHAR(20) NOT NULL COMMENT '配置类型: COMPONENT/CHAIN/SUB_CHAIN',
    config_id BIGINT NOT NULL COMMENT '配置ID',
    version INT NOT NULL COMMENT '版本号',
    content TEXT NOT NULL COMMENT '配置内容',
    status VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/ARCHIVED',
    created_by VARCHAR(50) COMMENT '创建人',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_tenant_config_version(tenant_id, config_type, config_id, version),
    INDEX idx_tenant_config(tenant_id, config_type, config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='配置版本表';

-- ============================================
-- 6. 测试用例表 (Testing Bounded Context)
-- ============================================
CREATE TABLE IF NOT EXISTS config_test_case (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    config_type VARCHAR(20) NOT NULL COMMENT '配置类型: COMPONENT/CHAIN',
    config_id BIGINT NOT NULL COMMENT '配置ID',
    name VARCHAR(100) NOT NULL COMMENT '测试用例名称',
    input_data TEXT NOT NULL COMMENT '输入数据（JSON格式）',
    expected_result TEXT NOT NULL COMMENT '期望结果（JSON格式）',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_tenant_config(tenant_id, config_type, config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='测试用例表';

-- ============================================
-- 7. 执行监控表 (Monitoring Bounded Context)
-- ============================================
CREATE TABLE IF NOT EXISTS execution_monitoring (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tenant_id BIGINT NOT NULL COMMENT '租户ID',
    chain_id BIGINT NOT NULL COMMENT '流程链ID',
    component_id VARCHAR(50) COMMENT '组件ID（可为空）',
    chain_execution_id VARCHAR(50) NOT NULL COMMENT '执行ID',
    execute_time BIGINT NOT NULL COMMENT '执行耗时(ms)',
    status VARCHAR(20) NOT NULL COMMENT '状态: SUCCESS/FAILURE',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_tenant_chain(tenant_id, chain_id, created_at),
    INDEX idx_execution_time(created_at),
    INDEX idx_chain_execution_id(chain_execution_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='执行监控表';

-- ============================================
-- 初始化默认租户数据
-- ============================================
INSERT INTO tenant_info (tenant_code, tenant_name, status, max_chains, max_components)
VALUES ('DEFAULT', '默认租户', 'ACTIVE', 100, 500)
ON DUPLICATE KEY UPDATE tenant_name = '默认租户';

-- ============================================
-- 初始化演示数据
-- ============================================

-- 插入演示规则组件
INSERT INTO rule_component (tenant_id, component_id, component_name, description, component_type, content, status)
VALUES (1, 'validateOrder', '订单验证', '验证订单信息的有效性', 'BUSINESS',
'public class ValidateOrderComponent extends NodeComponent {
    @Override
    public void process() {
        // 验证订单逻辑
        System.out.println("Validating order...");
    }
}', 'PUBLISHED');

INSERT INTO rule_component (tenant_id, component_id, component_name, description, component_type, content, status)
VALUES (1, 'checkStock', '库存检查', '检查商品库存是否充足', 'BUSINESS',
'public class CheckStockComponent extends NodeComponent {
    @Override
    public void process() {
        // 库存检查逻辑
        System.out.println("Checking stock...");
    }
}', 'PUBLISHED');

INSERT INTO rule_component (tenant_id, component_id, component_name, description, component_type, content, status)
VALUES (1, 'isVIPUser', 'VIP用户判断', '判断当前用户是否为VIP用户', 'CONDITION',
'public class IsVIPUserComponent extends NodeBooleanComponent {
    @Override
    public boolean processBoolean() throws Exception {
        // VIP判断逻辑
        return false;
    }
}', 'PUBLISHED');

-- 插入演示流程链
INSERT INTO flow_chain (tenant_id, chain_name, chain_code, description, config_type, status, current_version)
VALUES (1, '订单处理流程', 'THEN(validateOrder, checkStock)', '订单处理基础流程', 'DATABASE', 'PUBLISHED', 1);

-- 完成
SELECT 'DMS LiteFlow database initialized successfully!' AS message;
