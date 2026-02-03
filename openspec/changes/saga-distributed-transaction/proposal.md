# Proposal: Saga 分布式事务支持

## Why

当前 DMS LiteFlow 流程编排系统缺乏分布式事务支持。在跨服务业务场景中（如订单处理涉及订单服务、库存服务、支付服务），当某个环节调用失败时，已执行的操作无法自动回滚，导致数据不一致。

随着业务流程的复杂度增加，特别是涉及多个外部服务调用的场景，系统急需一种机制来保证：
- 失败时能够自动补偿已执行的操作
- 支持人工介入处理异常情况
- 提供完整的执行状态追踪和可视化能力

引入 Saga 模式可以在保持流程编排灵活性的同时，通过补偿机制实现最终一致性。

## What Changes

### 核心功能

- **Saga 编排引擎**
  - 基于 LiteFlow 事件机制（BEFORE_NODE, AFTER_SUCCESS, AFTER_FAILURE）
  - 自动追踪每个节点的执行状态和输出数据
  - 失败时自动触发补偿流程（按相反顺序执行补偿组件）
  - 支持混合失败策略（自动补偿、重试、人工介入）

- **补偿机制**
  - 组件级补偿：每个业务组件可声明对应的补偿组件
  - 补偿组件能够访问原始节点的输出数据
  - 支持补偿失败处理和告警
  - 保证补偿操作的幂等性

- **状态持久化**
  - Redis 存储热数据（执行中的 Saga），支持快速访问
  - MySQL 持久化完整执行历史，支持审计和查询
  - 执行栈记录：追踪已执行的节点顺序和输出
  - 补偿日志：记录补偿操作的结果

- **管理界面和 API**
  - Saga 执行列表：筛选、搜索、分页查询
  - 执行详情可视化：时间线展示、步骤状态、错误信息
  - 人工操作：手动补偿、重试失败节点、跳过节点继续执行
  - 人工决策处理：需要人工审核的异常场景

### 技术变更

- **新增领域模型**
  - `SagaExecution` - 流程执行实例
  - `StepExecution` - 节点执行状态
  - `SagaComponentMetadata` - 组件 Saga 元数据
  - `FailureStrategy` - 失败处理策略

- **新增应用服务**
  - `SagaExecutionService` - Saga 执行服务
  - `SagaManagementService` - Saga 管理服务
  - `SagaStateService` - 状态管理服务

- **新增基础设施**
  - `SagaEventListener` - LiteFlow 事件监听器
  - `CompensationOrchestrator` - 补偿编排器
  - Saga 相关 Repository 实现

- **数据库变更**
  - `saga_execution` - Saga 执行记录表
  - `saga_step_execution` - 节点执行状态表
  - `saga_compensation_log` - 补偿日志表
  - `saga_component_metadata` - 组件元数据表

- **API 变更**
  - 新增 `/api/saga/*` 端点（执行、查询、补偿、重试等）
  - 保持现有 `/api/execute/*` 端点向后兼容

- **集成 XXL-JOB**
  - 定时清理已完成的 Saga 记录（Redis 过期清理）
  - 补偿失败告警和重试任务

## Capabilities

### New Capabilities

- **saga-orchestration**: Saga 编排和执行
  - 在流程执行时自动启用 Saga 模式
  - 监听节点执行事件并记录状态
  - 失败时自动触发补偿流程
  - 支持同步和异步执行

- **saga-compensation**: 补偿机制
  - 组件声明补偿关系（compensateComponent）
  - 按相反顺序执行补偿操作
  - 补偿组件可访问原始节点的输出数据
  - 支持部分补偿（跳过无需补偿的只读节点）
  - 处理补偿失败场景

- **saga-state-management**: 状态持久化和查询
  - 执行状态实时更新到 Redis
  - 完整执行历史持久化到 MySQL
  - 支持按租户、流程、状态、时间范围查询
  - 执行栈追踪（executionStack）

- **saga-monitoring**: 可视化和管理界面
  - 执行列表和详情查询
  - 执行流程可视化（时间线）
  - 人工操作入口（补偿、重试、跳过、决策）
  - 补偿日志查看

### Modified Capabilities

_无现有需求变更。 Saga 功能是全新能力，不修改现有流程编排的核心行为。_

## Impact

### 代码影响

- **新增包结构**
  - `com.dms.liteflow.domain.saga.*` - 领域层
  - `com.dms.liteflow.application.saga.*` - 应用服务层
  - `com.dms.liteflow.infrastructure.saga.*` - 基础设施层
  - `com.dms.liteflow.api.saga.*` - API 控制器

- **现有代码适配**
  - 可选：在 `FlowChain` 中扩展 `transactional` 字段的使用
  - 业务组件可选择性地声明补偿组件
  - `ExecutionService` 保持不变，或作为 Saga 执行的底层实现

### API 影响

- **新增 API**
  - `POST /api/saga/execute` - 执行 Saga 流程
  - `GET /api/saga/executions/{id}` - 查询执行详情
  - `GET /api/saga/executions` - 查询执行列表
  - `POST /api/saga/executions/{id}/compensate` - 手动补偿
  - `POST /api/saga/executions/{id}/retry` - 重试失败节点
  - `POST /api/saga/executions/{id}/skip` - 跳过失败节点
  - `POST /api/saga/executions/{id}/manual-decision` - 人工决策
  - `GET /api/saga/executions/{id}/logs` - 查询执行日志

- **向后兼容**
  - 现有 `/api/execute/*` 端点保持不变
  - 流程配置可选择启用 Saga（非强制）

### 数据库影响

- **新增表**（4 张）
  - `saga_execution`
  - `saga_step_execution`
  - `saga_compensation_log`
  - `saga_component_metadata`

- **数据迁移**
  - 无需迁移现有数据
  - 元数据表初始化时扫描现有组件并推断补偿关系

### 依赖影响

- **无新增外部依赖**
  - 使用现有的 LiteFlow、Redis、MySQL、XXL-JOB
  - 补偿组件使用现有的 NodeComponent 机制

### 系统影响

- **性能**
  - 每个节点执行时有额外的状态持久化开销
  - Redis 写入延迟约 1-5ms，MySQL 异步写入
  - 补偿流程会增加总执行时间（仅在失败时）

- **可靠性**
  - Redis 故障时降级为纯 MySQL 存储
  - 补偿失败时记录日志并告警，支持人工介入

- **运维**
  - 需要定期清理 MySQL 历史数据（XXL-JOB 任务）
  - 监控 Saga 成功率、补偿率、人工介入率

### 用户影响

- **流程配置者**
  - 需要为需要补偿的组件编写对应的补偿组件
  - 在组件元数据中声明补偿关系和失败策略

- **流程执行者**
  - API 调用时可选择启用 Saga 模式
  - 失败时可获得详细的执行状态和错误信息

- **运维人员**
  - 新增 Saga 管理界面，可查看和处理异常流程
  - 介入处理需要人工决策的场景
