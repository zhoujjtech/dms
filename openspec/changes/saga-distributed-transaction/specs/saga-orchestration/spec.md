# Saga 编排和执行规范

## ADDED Requirements

### Requirement: 启用 Saga 模式执行流程
系统 SHALL 支持在流程执行时启用 Saga 模式，自动追踪节点执行状态并在失败时触发补偿流程。

#### Scenario: 成功执行完整流程（Saga 模式）
- **WHEN** 客户端调用 `/api/saga/execute` 并指定 `sagaMode=true`
- **AND** 流程包含 5 个节点且全部执行成功
- **THEN** 系统为每个节点记录执行状态到 Redis
- **AND** 系统持久化执行历史到 MySQL
- **AND** 所有节点的输出数据被保存
- **AND** 执行状态为 `COMPLETED`
- **AND** 不触发任何补偿操作

#### Scenario: 中间节点失败触发自动补偿
- **WHEN** 客户端执行 Saga 流程且第 3 个节点失败
- **AND** 前两个节点已成功执行
- **AND** 失败策略为 `auto_compensate`
- **THEN** 系统记录失败节点的错误信息
- **AND** 系统按相反顺序执行前两个节点的补偿组件
- **AND** 执行状态变更为 `COMPENSATING`
- **AND** 补偿完成后状态为 `COMPENSATED`

#### Scenario: 只读节点失败不触发补偿
- **WHEN** 流程执行到只读节点（如 validateOrder）并失败
- **AND** 该节点标记为 `needsCompensation=false`
- **THEN** 系统记录失败但不触发补偿
- **AND** 执行状态为 `FAILED`
- **AND** executionStack 为空或仅包含该节点

### Requirement: 监听 LiteFlow 节点事件
系统 SHALL 通过 LiteFlow 事件监听机制自动追踪节点执行状态。

#### Scenario: BEFORE_NODE 事件记录节点开始
- **WHEN** LiteFlow 触发 `BEFORE_NODE` 事件
- **THEN** 系统创建新的 `StepExecution` 记录
- **AND** 状态为 `RUNNING`
- **AND** 记录执行开始时间戳
- **AND** 保存到 Redis 执行栈

#### Scenario: AFTER_NODE_SUCCESS 事件记录节点成功
- **WHEN** LiteFlow 触发 `AFTER_NODE_SUCCESS` 事件
- **THEN** 系统更新对应 `StepExecution` 状态为 `COMPLETED`
- **AND** 从上下文提取并保存输出数据
- **AND** 如果节点需要补偿，将节点加入 executionStack
- **AND** 更新 Redis 和 MySQL

#### Scenario: AFTER_NODE_FAILURE 事件触发补偿流程
- **WHEN** LiteFlow 触发 `AFTER_NODE_FAILURE` 事件
- **THEN** 系统更新对应 `StepExecution` 状态为 `FAILED`
- **AND** 保存错误信息（code、message）
- **THEN** 根据失败策略决定下一步操作
- **AND** 如果是 `auto_compensate`，立即启动补偿流程
- **AND** 如果是 `retry`，执行重试逻辑
- **AND** 如果是 `manual`，标记为 `MANUAL_INTERVENTION`

### Requirement: 同步和异步执行支持
系统 SHALL 支持 Saga 流程的同步和异步执行模式。

#### Scenario: 同步执行 Saga 流程
- **WHEN** 客户端调用 `/api/saga/execute` 并指定 `async=false`
- **THEN** 系统阻塞等待流程完成（成功、失败或补偿完成）
- **AND** 返回完整的 `ExecutionResponseVO` 包含最终状态
- **AND** `executionId` 在响应中返回

#### Scenario: 异步执行 Saga 流程
- **WHEN** 客户端调用 `/api/saga/execute` 并指定 `async=true`
- **THEN** 系统立即返回 `executionId` 和状态 `PENDING`
- **AND** 流程在后台异步执行
- **AND** 客户端可通过 `/api/saga/executions/{id}` 查询状态

#### Scenario: 异步执行状态查询
- **WHEN** 客户端查询正在执行的异步 Saga
- **THEN** 返回当前执行状态（RUNNING/COMPENSATING等）
- **AND** 返回当前步骤索引
- **AND** 返回已完成和总步骤数
- **AND** 返回预计剩余时间（如果可计算）

### Requirement: 失败策略配置
系统 SHALL 支持为每个节点配置不同的失败策略。

#### Scenario: 自动补偿策略
- **WHEN** 节点配置失败策略为 `auto_compensate` 且执行失败
- **THEN** 系统自动触发补偿流程
- **AND** 不需要人工介入

#### Scenario: 重试策略
- **WHEN** 节点配置失败策略为 `retry` 且执行失败
- **AND** 错误类型标记为 `retryable=true`
- **AND** 已重试次数 < 配置的最大重试次数
- **THEN** 系统自动重新执行该节点
- **AND** 重试次数累加
- **AND** 如果超过最大重试次数，触发补偿

#### Scenario: 人工介入策略
- **WHEN** 节点配置失败策略为 `manual` 且执行失败
- **OR** 错误类型为 `RISK_CHECK_FAILED` 等需要人工审核的类型
- **THEN** 系统暂停流程执行
- **AND** 状态标记为 `MANUAL_INTERVENTION`
- **AND** 发送告警通知
- **AND** 等待通过 API 人工决策

#### Scenario: 条件式失败策略
- **WHEN** 节点配置多个失败策略条件
- **AND** 执行失败时错误码为 `INSUFFICIENT_FUNDS`
- **THEN** 系统匹配到对应的 `auto_compensate` 策略
- **AND** 按该策略执行（而非默认策略）

### Requirement: 执行上下文传递
系统 SHALL 确保补偿组件能够访问原始节点的输出数据。

#### Scenario: 补偿组件访问原始数据
- **WHEN** `createOrder` 节点成功执行并输出 `orderId`
- **AND** 后续节点失败触发补偿
- **THEN** 补偿组件 `cancelOrder` 从上下文获取 `orderId`
- **AND** 使用该 ID 执行取消订单操作

#### Scenario: 多个节点的数据隔离
- **WHEN** `createOrder` 输出 `orderId`
- **AND** `reserveStock` 输出 `reservedQty`
- **THEN** 补偿时 `cancelOrder` 只访问 `orderId`
- **AND** `releaseStock` 只访问 `reservedQty`
- **AND** 数据不互相污染

### Requirement: 超时处理
系统 SHALL 支持节点级和流程级的超时配置。

#### Scenario: 节点执行超时
- **WHEN** 节点执行时间超过配置的 `timeoutMs`
- **THEN** 系统中断节点执行
- **AND** 标记节点状态为 `FAILED`
- **AND** 错误码为 `EXECUTION_TIMEOUT`
- **AND** 触发对应的失败策略

#### Scenario: 整体流程超时
- **WHEN** Saga 流程总执行时间超过配置的 `sagaTimeoutMs`
- **THEN** 系统中断流程执行
- **AND** 触发补偿流程
- **AND** 记录超时原因

### Requirement: 租户隔离
系统 SHALL 确保多租户环境下 Saga 执行的隔离性。

#### Scenario: 租户 A 的 Saga 失败不影响租户 B
- **WHEN** 租户 A 的 Saga 流程失败并触发补偿
- **THEN** 补偿操作仅影响租户 A 的数据
- **AND** 租户 B 的并发 Saga 不受影响
- **AND** Redis key 包含租户 ID 前缀

#### Scenario: 租户级别的失败策略配置
- **WHEN** 租户 A 配置了自定义失败策略
- **AND** 租户 B 使用默认策略
- **THEN** 系统根据当前租户应用对应的策略
- **AND** 不相互干扰
