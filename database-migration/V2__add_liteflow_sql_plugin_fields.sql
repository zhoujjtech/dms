-- ============================================================================
-- LiteFlow SQL 插件表结构扩展
-- 版本: V2
-- 描述: 扩展 flow_chain 和 rule_component 表，支持 LiteFlow SQL 插件
-- ============================================================================

-- ----------------------------------------------------------------------------
-- flow_chain 表扩展
-- ----------------------------------------------------------------------------

-- 添加应用名称字段
ALTER TABLE flow_chain
ADD COLUMN application_name VARCHAR(100) NOT NULL DEFAULT 'dms-liteflow' COMMENT '应用名称';

-- 添加流程启用状态字段
ALTER TABLE flow_chain
ADD COLUMN chain_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用';

-- 添加命名空间字段
ALTER TABLE flow_chain
ADD COLUMN namespace VARCHAR(100) DEFAULT 'default' COMMENT '命名空间';

-- 确保 update_time 字段存在（如果不存在则添加）
SET @exist = (SELECT COUNT(*) FROM information_schema.columns
              WHERE table_schema = DATABASE()
              AND table_name = 'flow_chain'
              AND column_name = 'update_time');

SET @sql = IF(@exist = 0,
    'ALTER TABLE flow_chain ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT "Column update_time already exists"');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引优化查询性能
CREATE INDEX idx_flow_chain_app_tenant ON flow_chain(application_name, tenant_id, status);
CREATE INDEX idx_flow_chain_enable ON flow_chain(chain_enable);
CREATE INDEX idx_flow_chain_namespace ON flow_chain(namespace);

-- ----------------------------------------------------------------------------
-- rule_component 表扩展
-- ----------------------------------------------------------------------------

-- 添加应用名称字段
ALTER TABLE rule_component
ADD COLUMN application_name VARCHAR(100) NOT NULL DEFAULT 'dms-liteflow' COMMENT '应用名称';

-- 添加脚本启用状态字段
ALTER TABLE rule_component
ADD COLUMN script_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用';

-- 添加脚本语言字段
ALTER TABLE rule_component
ADD COLUMN language VARCHAR(50) DEFAULT 'java' COMMENT '脚本语言: java, groovy, javascript, qlexpress';

-- 确保 update_time 字段存在（如果不存在则添加）
SET @exist = (SELECT COUNT(*) FROM information_schema.columns
              WHERE table_schema = DATABASE()
              AND table_name = 'rule_component'
              AND column_name = 'update_time');

SET @sql = IF(@exist = 0,
    'ALTER TABLE rule_component ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP',
    'SELECT "Column update_time already exists"');

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 添加索引优化查询性能
CREATE INDEX idx_rule_component_app_tenant ON rule_component(application_name, tenant_id, status);
CREATE INDEX idx_rule_component_enable ON rule_component(script_enable);
CREATE INDEX idx_rule_component_language ON rule_component(language);

-- ----------------------------------------------------------------------------
-- 数据迁移：更新现有数据
-- ----------------------------------------------------------------------------

-- 更新 flow_chain 表现有数据
UPDATE flow_chain
SET application_name = 'dms-liteflow',
    chain_enable = CASE WHEN status = 'PUBLISHED' THEN 1 ELSE 0 END,
    namespace = 'default'
WHERE application_name IS NULL OR application_name = '';

-- 更新 rule_component 表现有数据
UPDATE rule_component
SET application_name = 'dms-liteflow',
    script_enable = CASE WHEN status = 'PUBLISHED' THEN 1 ELSE 0 END,
    language = 'java'
WHERE application_name IS NULL OR application_name = '';

-- ============================================================================
-- 回滚脚本 (仅在需要时执行)
-- ============================================================================
/*
-- 回滚 flow_chain 表
ALTER TABLE flow_chain DROP COLUMN application_name;
ALTER TABLE flow_chain DROP COLUMN chain_enable;
ALTER TABLE flow_chain DROP COLUMN namespace;
DROP INDEX idx_flow_chain_app_tenant ON flow_chain;
DROP INDEX idx_flow_chain_enable ON flow_chain;
DROP INDEX idx_flow_chain_namespace ON flow_chain;

-- 回滚 rule_component 表
ALTER TABLE rule_component DROP COLUMN application_name;
ALTER TABLE rule_component DROP COLUMN script_enable;
ALTER TABLE rule_component DROP COLUMN language;
DROP INDEX idx_rule_component_app_tenant ON rule_component;
DROP INDEX idx_rule_component_enable ON rule_component;
DROP INDEX idx_rule_component_language ON rule_component;
*/
