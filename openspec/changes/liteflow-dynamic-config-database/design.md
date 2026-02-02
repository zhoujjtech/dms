# Design: LiteFlow Database Dynamic Configuration

## Context

### Current State

当前项目使用 **LiteFlow 2.12.4** 引擎，配置存储在 `classpath:flow/` 目录下的静态 XML 文件中。应用架构采用 Spring Boot 3.2.0 + Spring Cloud Alibaba，支持多租户（通过 `TenantContext` 传递租户ID）。

现有数据库表结构：
- `flow_chain`: 存储流程链配置，包含 `chain_name`, `chain_code`, `status` 等字段
- `rule_component`: 存储组件配置，包含 `component_id`, `component_code`, `component_type` 等字段

### Problem Constraints

1. **静态配置限制**: 配置变更需要重启应用，影响业务连续性
2. **多租户隔离**: LiteFlow SQL 插件原生不支持多租户过滤（需自定义 SQL）
3. **缓存一致性**: 多实例部署时需要确保配置同步
4. **向后兼容**: 需要保留现有的版本管理和状态发布机制

### Stakeholders

- **开发团队**: 需要清晰的实现方案和迁移路径
- **运维团队**: 需要监控配置加载状态和刷新事件
- **租户管理员**: 需要独立管理各自租户的流程配置

## Goals / Non-Goals

**Goals:**

1. 支持从数据库动态加载 LiteFlow 配置（流程链 + 组件）
2. 实现配置自动轮询刷新（60秒间隔，可配置）
3. 支持多租户配置隔离（通过 `tenant_id` 过滤）
4. 确保分布式环境下配置一致性
5. 保持向后兼容，API 接口不变

**Non-Goals:**

1. 不实现 Nacos/Zookeeper 配置源（专注于数据库方案）
2. 不修改现有的 FlowConfigService API（保留作为辅助服务）
3. 不实现配置的实时推送（轮询机制足够）
4. 不实现组件的热部署/热编译（仅配置加载）

## Decisions

### Decision 1: 使用 LiteFlow SQL 插件而非自定义实现

**选择**: 集成官方 `liteflow-rule-sql` 插件 (2.15.0+)

**理由**:
- 官方维护，与 LiteFlow 引擎版本兼容性好
- 内置轮询刷新机制和 SHA 变更检测
- 减少自定义代码和维护成本

**替代方案**:
- 自定义实现 `RuleDataSource` 接口 → 开发成本高，需自己实现刷新逻辑
- 使用 Zookeeper/Nacos 配置源 → 增加外部依赖，运维复杂度高

**实现**:
```xml
<dependency>
    <groupId>com.yomahub</groupId>
    <artifactId>liteflow-rule-sql</artifactId>
    <version>2.15.0.2</version>
</dependency>
```

### Decision 2: 多租户通过自定义 SQL 过滤实现

**选择**: 自定义 SQL 查询，在 `WHERE` 子句中添加 `tenant_id` 条件

**理由**:
- LiteFlow SQL 插件支持自定义 SQL 配置
- 租户ID从 `TenantContext` 获取（现有机制）
- 避免修改 LiteFlow 插件源码

**实现方案**:

```yaml
liteflow:
  rule-source-ext-data-map:
    # 自定义 SQL 过滤（需配置）
    chainSql: |
      SELECT chain_name, chain_code, application_name, namespace
      FROM flow_chain
      WHERE application_name = #{applicationName}
        AND tenant_id = #{tenantId}
        AND status = 'PUBLISHED'
        AND chain_enable = 1
```

**挑战**: LiteFlow SQL 插件不直接支持动态参数（tenantId）
**解决方案**: 使用 `ThreadLocal` 传递租户ID，在 SQL 执行前注入参数

### Decision 3: 租户上下文传递机制

**选择**: 使用现有的 `TenantContext` (ThreadLocal) + 自定义 `RuleDataSource`

**理由**:
- 项目已实现 `TenantContextInterceptor` 拦截器
- 保持租户上下文传递机制的一致性
- 避免大规模重构

**实现流程**:

```
Request → TenantContextInterceptor → 设置 TenantContext
                                    ↓
                         FlowExecutor.execute()
                                    ↓
              CustomRuleDataSource → 获取 TenantContext.getTenantId()
                                    ↓
                         执行 SQL (WHERE tenant_id = ?)
```

**替代方案**:
- 通过请求参数传递租户ID → 需修改 FlowExecutor 调用链
- 使用组件隐式参数 → 无法在 SQL 查询阶段获取

### Decision 4: 配置刷新策略

**选择**: 轮询刷新 + SHA 变更检测（LiteFlow SQL 插件内置）

**配置**:
```yaml
liteflow:
  rule-source-ext-data-map:
    pollingEnabled: true
    pollingIntervalSeconds: 60
    pollingStartSeconds: 60
```

**理由**:
- 实现简单，无需引入消息队列
- 60秒延迟可接受（非实时配置场景）
- SHA 比对避免不必要的刷新

**替代方案**:
- CDC (Change Data Capture) → 复杂度高，需要额外组件（Debezium）
- 数据库触发器 + 消息队列 → 实时性高但复杂度高

### Decision 5: 数据库表结构扩展

**选择**: 在现有表基础上增加字段，保持向后兼容

**flow_chain 表扩展**:
```sql
ALTER TABLE flow_chain ADD COLUMN application_name VARCHAR(100) DEFAULT 'dms-liteflow';
ALTER TABLE flow_chain ADD COLUMN chain_enable TINYINT DEFAULT 1;
ALTER TABLE flow_chain ADD COLUMN namespace VARCHAR(100) DEFAULT 'default';
ALTER TABLE flow_chain ADD COLUMN update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;
```

**rule_component 表扩展**:
```sql
ALTER TABLE rule_component ADD COLUMN application_name VARCHAR(100) DEFAULT 'dms-liteflow';
ALTER TABLE rule_component ADD COLUMN script_enable TINYINT DEFAULT 1;
ALTER TABLE rule_component ADD COLUMN language VARCHAR(50) DEFAULT 'java';
```

**理由**:
- 最小化变更，使用 `DEFAULT` 值确保现有数据兼容
- `application_name` 支持多应用场景（未来扩展）
- `update_time` 用于 SHA 计算和版本管理

### Decision 6: 分布式配置同步策略

**选择**: 最终一致性模型，无需分布式锁

**理由**:
- 所有实例从同一数据库加载，天然一致
- 轮询间隔内短暂不一致可接受（配置非实时场景）
- 避免引入 Redis 等外部依赖

**降级策略**:
```
数据库可用 → 正常轮询刷新
数据库不可用 → 使用缓存配置，记录 ERROR 日志
```

**可选增强**: 支持分布式锁（通过 `--with-redis` 参数启用）
```
尝试获取锁 → 成功则刷新，失败则跳过
锁超时: 30秒（小于轮询间隔60秒）
```

### Decision 7: 保留 FlowConfigService 作为辅助服务

**选择**: 保留 `FlowConfigService` 和 `FlowConfigLoader`，调整用途

**新职责**:
- 提供配置查询 API（用于管理界面）
- 提供配置缓存清理 API（手动刷新）
- 不再负责向 LiteFlow 注入配置（由 SQL 插件接管）

**理由**:
- 保持现有 API 兼容（外部调用方无需修改）
- 支持配置管理和查询场景
- 代码复用（Repository 层共享）

## Architecture

### 组件交互图

```
┌─────────────────────────────────────────────────────────────────┐
│                         API Layer                                │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │        ExecutionController (执行API)                      │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Application Layer                            │
│  ┌──────────────────┐        ┌──────────────────────────────┐   │
│  │ ExecutionService │        │ FlowConfigService (辅助)      │   │
│  └──────────────────┘        │ - 配置查询 API                │   │
│                              │ - 缓存清理 API                 │   │
│                              └──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                          │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              TenantContextInterceptor                     │  │
│  │   (提取 X-Tenant-Id 请求头 → 设置 TenantContext)          │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              ↓                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │           CustomRuleDataSource (新建)                     │  │
│  │   - 从 TenantContext 获取租户ID                           │  │
│  │   - 构建带租户过滤的 SQL                                  │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              ↓                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │         LiteFlow SQL Plugin (liteflow-rule-sql)           │  │
│  │   - 执行 SQL 查询                                         │  │
│  │   - 轮询刷新 (60s)                                        │  │
│  │   - SHA 变更检测                                          │  │
│  └───────────────────────────────────────────────────────────┘  │
│                              ↓                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │                FlowExecutor (LiteFlow)                    │  │
│  └───────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Database Layer                              │
│  ┌──────────────────┐        ┌──────────────────────────────┐   │
│  │   flow_chain     │        │   rule_component             │   │
│  │ - tenant_id      │        │ - tenant_id                  │   │
│  │ - application... │        │ - application...             │   │
│  └──────────────────┘        └──────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### 配置加载时序图

```
应用启动 → LiteFlow 初始化
            ↓
    LiteFlow SQL Plugin 初始化
            ↓
    执行初始 SQL 查询 (无租户上下文，加载所有配置)
            ↓
    FlowExecutor 构建流程引擎
            ↓
    启动轮询调度器 (60s 后首次执行)

---

请求执行 → TenantContextInterceptor 设置租户ID
            ↓
    FlowExecutor.execute(chainName)
            ↓
    CustomRuleDataSource 获取租户ID
            ↓
    执行 SQL (WHERE tenant_id = ?)
            ↓
    返回租户专属配置
```

### 配置刷新时序图

```
60秒轮询触发 → LiteFlow SQL Plugin 检查配置
                ↓
        计算 SQL 结果的 SHA 值
                ↓
        SHA 值变化? ─No→ 结束（无需刷新）
          |
         Yes
          ↓
        重新加载配置
                ↓
        更新 FlowExecutor 规则
                ↓
        记录刷新日志
                ↓
        正在执行的任务继续使用旧配置
        新请求使用新配置
```

## Database Schema Changes

### flow_chain 表扩展

```sql
-- 新增字段
ALTER TABLE flow_chain
ADD COLUMN application_name VARCHAR(100) NOT NULL DEFAULT 'dms-liteflow' COMMENT '应用名称';

ALTER TABLE flow_chain
ADD COLUMN chain_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用';

ALTER TABLE flow_chain
ADD COLUMN namespace VARCHAR(100) DEFAULT 'default' COMMENT '命名空间';

-- 索引优化
CREATE INDEX idx_app_tenant ON flow_chain(application_name, tenant_id, status);
CREATE INDEX idx_chain_enable ON flow_chain(chain_enable);
```

### rule_component 表扩展

```sql
-- 新增字段
ALTER TABLE rule_component
ADD COLUMN application_name VARCHAR(100) NOT NULL DEFAULT 'dms-liteflow' COMMENT '应用名称';

ALTER TABLE rule_component
ADD COLUMN script_enable TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用: 1=启用, 0=禁用';

ALTER TABLE rule_component
ADD COLUMN language VARCHAR(50) DEFAULT 'java' COMMENT '脚本语言: java, groovy, javascript, qlexpress';

-- 索引优化
CREATE INDEX idx_app_tenant ON rule_component(application_name, tenant_id, status);
CREATE INDEX idx_script_enable ON rule_component(script_enable);
```

### 数据迁移脚本

```sql
-- 迁移现有数据
UPDATE flow_chain
SET application_name = 'dms-liteflow',
    chain_enable = CASE WHEN status = 'PUBLISHED' THEN 1 ELSE 0 END,
    namespace = 'default'
WHERE application_name IS NULL;

UPDATE rule_component
SET application_name = 'dms-liteflow',
    script_enable = CASE WHEN status = 'PUBLISHED' THEN 1 ELSE 0 END,
    language = 'java'
WHERE application_name IS NULL;
```

## Configuration Changes

### application.yml 配置

```yaml
liteflow:
  # 移除静态配置
  # rule-source: configData
  # configData: classpath:flow/

  # 启用 SQL 插件配置
  rule-source-ext-data-map:
    # 应用标识
    applicationName: dms-liteflow

    # 轮询配置
    pollingEnabled: true
    pollingIntervalSeconds: 60
    pollingStartSeconds: 60

    # SQL 日志
    sqlLogEnabled: true

    # flow_chain 表配置
    chainTableName: flow_chain
    chainApplicationNameField: application_name
    chainNameField: chain_name
    chainDescField: chain_desc
    elDataField: chain_code
    chainEnableField: chain_enable
    namespaceField: namespace

    # rule_component 表配置（脚本组件）
    scriptTableName: rule_component
    scriptApplicationNameField: application_name
    scriptIdField: component_id
    scriptNameField: component_name
    scriptDataField: component_code
    scriptTypeField: component_type
    scriptLanguageField: language
    scriptEnableField: script_enable
```

### 多租户 SQL 过滤配置（可选增强）

```yaml
liteflow:
  rule-source-ext-data-map:
    # 自定义 SQL（需要实现 CustomRuleDataSource）
    customChainSql: |
      SELECT chain_name, chain_code, namespace
      FROM flow_chain
      WHERE application_name = :applicationName
        AND tenant_id = :tenantId
        AND status = 'PUBLISHED'
        AND chain_enable = 1
```

## Migration Plan

### 阶段 1: 准备工作 (1-2天)

1. **依赖升级**
   - 升级 `liteflow-rule-sql` 到 2.15.0.2
   - 验证与现有 LiteFlow 2.12.4 的兼容性

2. **数据库迁移**
   - 在测试环境执行表结构扩展脚本
   - 运行数据迁移脚本
   - 验证现有数据完整性

3. **静态配置导出**
   - 从 `classpath:flow/` 导出现有 XML 配置
   - 转换为数据库 INSERT 语句

### 阶段 2: 开发实现 (3-5天)

1. **实现 CustomRuleDataSource**
   - 继承 `JdbcRuleSource`
   - 重写 `getSqlConnection` 方法注入租户ID
   - 从 `TenantContext` 获取当前租户ID

2. **配置调整**
   - 修改 `application.yml`
   - 移除 `rule-source: configData`
   - 添加 SQL 插件配置

3. **辅助服务调整**
   - 保留 `FlowConfigService` 用于配置查询
   - 新增手动刷新 API

### 阶段 3: 测试验证 (2-3天)

1. **单元测试**
   - CustomRuleDataSource 租户ID注入测试
   - 配置加载和刷新测试

2. **集成测试**
   - 多租户配置隔离测试
   - 配置刷新测试
   - 分布式多实例测试

3. **性能测试**
   - 配置加载性能测试
   - 轮询刷新对性能的影响

### 阶段 4: 灰度发布 (1周)

1. **灰度策略**
   - 先发布 1 个实例，观察 24 小时
   - 逐步发布所有实例

2. **监控指标**
   - 配置加载成功率
   - 配置刷新次数和失败率
   - FlowExecutor 执行性能

3. **回滚预案**
   - 保留静态 XML 配置文件
   - 快速回滚：修改 `application.yml` 恢复 `rule-source: configData`
   - 重启应用

### 阶段 5: 全量发布

1. **全量发布**
   - 所有实例切换到 SQL 配置源
   - 移除静态 XML 文件（可选）

2. **文档更新**
   - 更新部署文档
   - 更新运维手册
   - 更新配置管理指南

## Risks / Trade-offs

### Risk 1: LiteFlow 版本兼容性

**风险**: `liteflow-rule-sql` 2.15.0.2 与现有 LiteFlow 2.12.4 可能不兼容

**缓解措施**:
1. 在测试环境充分验证
2. 准备版本回滚方案
3. 考虑统一升级到 LiteFlow 2.15.x

### Risk 2: 多租户 SQL 性能问题

**风险**: 每次请求都执行带 `tenant_id` 过滤的 SQL，可能影响性能

**缓解措施**:
1. 添加索引 `(application_name, tenant_id, status)`
2. 使用 Caffeine 缓存查询结果（已有）
3. 监控慢查询，优化 SQL

### Risk 3: 配置刷新延迟

**风险**: 60秒轮询间隔，配置变更生效有延迟

**缓解措施**:
1. 提供手动刷新 API（立即生效）
2. 调整轮询间隔（最小 10 秒）
3. 在管理界面提示"配置将在 60 秒内生效"

### Risk 4: 数据库不可用

**风险**: 数据库不可用时，新租户无法加载配置

**缓解措施**:
1. 使用 Caffeine 缓存已加载的配置
2. 提供"降级模式"提示
3. 数据库恢复后自动刷新

### Risk 5: 迁移过程中断

**风险**: 数据库迁移脚本执行失败，影响线上服务

**缓解措施**:
1. 在低峰期执行迁移
2. 使用事务确保原子性
3. 准备回滚脚本（ALTER TABLE ... DROP COLUMN ...）
4. 先在备库执行迁移，验证后切换

## Trade-offs

| 决策 | 优势 | 劣势 | 选择 |
|------|------|------|------|
| 配置源 | 轮询刷新简单 | 60秒延迟 | **轮询** (足够) |
| 多租户实现 | CustomRuleDataSource 灵活 | 需自定义代码 | **自定义 SQL** |
| 分布式锁 | 确保一致性 | 增加 Redis 依赖 | **可选增强** |
| 配置中心 (Nacos/ZK) | 实时推送 | 增加运维复杂度 | **不使用** |

## Open Questions

1. **租户ID传递**: 如果请求中没有 `X-Tenant-Id` 请求头，是否返回错误？
   - **建议**: 使用默认租户ID（1），记录 WARN 日志

2. **轮询间隔**: 60秒是否合适？
   - **建议**: 可配置，默认 60 秒，最小 10 秒

3. **分布式锁**: 是否需要实现 Redis 分布式锁？
   - **建议**: 第一版不实现，作为可选增强

4. **配置版本管理**: 是否需要保留完整的配置变更历史？
   - **建议**: 使用现有的 `config_version` 表，无需额外开发

5. **回滚策略**: 是否需要支持一键回滚到历史配置版本？
   - **建议**: 提供回滚 API，由管理员手动触发
