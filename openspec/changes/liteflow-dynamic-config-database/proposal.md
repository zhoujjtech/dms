# Proposal: LiteFlow Database Dynamic Configuration

## Why

当前项目使用静态 XML 文件配置 LiteFlow 流程规则，配置变更需要重启服务，无法支持多租户场景下的动态配置管理。随着业务发展，需要支持租户级别的配置隔离、配置热更新以及分布式环境下的配置同步能力。

## What Changes

- 集成 LiteFlow SQL 插件 (`liteflow-rule-sql`)，实现数据库作为配置源
- 配置轮询自动刷新机制，支持配置变更热更新（无需重启）
- 数据库表结构扩展，支持多租户配置隔离（`tenant_id` 字段）
- 保留现有的版本管理和状态发布机制（`PUBLISHED` 状态）
- **BREAKING**: 弃用静态 XML 配置文件（可作为降级方案保留）
- 新增配置变更监听和同步机制

## Capabilities

### New Capabilities
- `database-rule-source`: 数据库规则配置源，支持从数据库动态加载流程链和组件配置
- `config-hot-reload`: 配置热更新机制，支持轮询刷新和实时变更推送
- `multi-tenant-config`: 多租户配置隔离，不同租户使用独立的流程配置
- `distributed-config-sync`: 分布式配置同步，确保多实例配置一致性

### Modified Capabilities
(无现有 spec 需要修改)

## Impact

### Dependencies
- 新增: `liteflow-rule-sql` (2.15.0+) 插件依赖

### Database Schema
- 扩展 `flow_chain` 表：增加 `application_name`, `chain_enable`, `namespace` 字段
- 扩展 `rule_component` 表：增加 `application_name`, `script_enable`, `language` 字段

### Configuration Files
- 修改 `application.yml`：移除静态 XML 配置，添加 SQL 插件配置

### Code Changes
- 保留 `FlowConfigService` 作为辅助服务（用于配置查询和缓存管理）
- `FlowConfigLoader` 调整为适配新的表结构
- 可选移除 `classpath:flow/` 下的 XML 文件

### API Impact
- 无 API 变更，对外接口保持兼容

### Deployment
- 需要数据库迁移脚本来扩展表结构
- 建议在低峰期执行配置迁移
