## ADDED Requirements

### Requirement: 系统从数据库动态加载 LiteFlow 规则配置
系统 SHALL 使用 LiteFlow SQL 插件从数据库动态加载流程链和组件配置，替代静态 XML 文件配置方式。

#### Scenario: 应用启动时从数据库加载配置
- **WHEN** 应用启动
- **THEN** 系统从数据库 `flow_chain` 表加载已发布的流程链配置
- **AND** 系统从数据库 `rule_component` 表加载已发布的组件配置
- **AND** LiteFlow 引擎使用加载的配置进行初始化

#### Scenario: 配置加载失败时记录错误
- **WHEN** 数据库配置加载失败（数据库连接失败、表不存在、数据格式错误）
- **THEN** 系统记录 ERROR 级别日志
- **AND** 系统继续运行（如果配置加载失败）
- **AND** 系统提供健康检查接口报告配置加载状态

### Requirement: 支持流程链配置的数据库存储
系统 SHALL 将流程链配置存储在 `flow_chain` 表中，并支持 LiteFlow SQL 插件的查询格式。

#### Scenario: 查询租户的流程链配置
- **WHEN** LiteFlow SQL 插件查询流程链配置
- **THEN** 系统返回 `application_name` 匹配当前应用的记录
- **AND** 系统仅返回 `status` 为 `PUBLISHED` 的流程链
- **AND** 系统返回的记录包含 `chain_name` 和 `chain_code` 字段

#### Scenario: 支持按命名空间过滤配置
- **WHEN** `flow_chain` 表中存在 `namespace` 字段
- **THEN** 系统支持按 `namespace` 过滤流程链配置
- **AND** 系统默认使用 `default` 命名空间

### Requirement: 支持组件配置的数据库存储
系统 SHALL 将组件配置存储在 `rule_component` 表中，并支持 LiteFlow SQL 插件的查询格式。

#### Scenario: 查询租户的组件配置
- **WHEN** LiteFlow SQL 插件查询组件配置
- **THEN** 系统返回 `application_name` 匹配当前应用的记录
- **AND** 系统仅返回 `status` 为 `PUBLISHED` 的组件
- **AND** 系统返回的记录包含 `component_id` 和 `component_code` 字段

#### Scenario: 支持脚本类型组件
- **WHEN** 组件类型为脚本（如 Groovy、JavaScript、QLExpress）
- **THEN** 系统从 `component_code` 字段读取脚本内容
- **AND** 系统根据 `language` 字段识别脚本类型
- **AND** 系统正确解析和执行脚本组件

### Requirement: 配置表结构兼容 LiteFlow SQL 插件
系统 SHALL 扩展现有的数据库表结构，使其兼容 LiteFlow SQL 插件的查询要求。

#### Scenario: flow_chain 表包含必要字段
- **WHEN** 系统 `flow_chain` 表被查询
- **THEN** 表包含 `id` 字段（主键）
- **AND** 表包含 `application_name` 字段（VARCHAR，用于区分应用）
- **AND** 表包含 `chain_name` 字段（VARCHAR，流程链名称）
- **AND** 表包含 `chain_code` 字段（TEXT，流程链 EL 表达式）
- **AND** 表包含 `chain_desc` 字段（VARCHAR，可选，流程链描述）
- **AND** 表包含 `chain_enable` 字段（TINYINT，是否启用，1=启用，0=禁用）
- **AND** 表包含 `namespace` 字段（VARCHAR，可选，命名空间）
- **AND** 表包含 `tenant_id` 字段（BIGINT，租户ID）
- **AND** 表包含 `create_time` 和 `update_time` 字段

#### Scenario: rule_component 表包含必要字段
- **WHEN** 系统 `rule_component` 表被查询
- **THEN** 表包含 `id` 字段（主键）
- **AND** 表包含 `application_name` 字段（VARCHAR，用于区分应用）
- **AND** 表包含 `component_id` 字段（VARCHAR，组件唯一标识）
- **AND** 表包含 `component_name` 字段（VARCHAR，组件名称）
- **AND** 表包含 `component_code` 字段（TEXT，组件代码/脚本内容）
- **AND** 表包含 `component_type` 字段（VARCHAR，组件类型）
- **AND** 表包含 `language` 字段（VARCHAR，脚本语言，如 groovy、javascript）
- **AND** 表包含 `script_enable` 字段（TINYINT，是否启用，1=启用，0=禁用）
- **AND** 表包含 `tenant_id` 字段（BIGINT，租户ID）
- **AND** 表包含 `create_time` 和 `update_time` 字段
