# 状态管理规范

## ADDED Requirements

### Requirement: 执行状态实时持久化
系统 SHALL 在每个节点执行时实时更新执行状态到 Redis 和 MySQL。

#### Scenario: 节点开始执行时保存状态
- **WHEN** 节点开始执行（BEFORE_NODE 事件）
- **THEN** 系统立即保存 `StepExecution` 状态到 Redis
- **AND** 状态为 `RUNNING`
- **AND** 记录 `executedAt` 时间戳
- **AND** 异步写入 MySQL

#### Scenario: 节点成功完成时更新状态
- **WHEN** 节点成功执行（AFTER_NODE_SUCCESS 事件）
- **THEN** 系统更新 Redis 中的 `StepExecution` 状态为 `COMPLETED`
- **AND** 保存节点输出数据到 Redis
- **AND** 异步更新 MySQL

#### Scenario: 节点失败时记录错误
- **WHEN** 节点执行失败（AFTER_NODE_FAILURE 事件）
- **THEN** 系统更新状态为 `FAILED`
- **AND** 保存错误信息（errorCode、errorMessage）
- **AND** 保存异常堆栈（如果可获取）
- **AND** 同步写入 MySQL（失败数据必须持久化）

### Requirement: 执行栈管理
系统 SHALL 维护 executionStack 记录已执行节点的顺序和输出数据。

#### Scenario: 执行栈追加成功节点
- **WHEN** 节点成功执行且需要补偿
- **THEN** 系统将节点追加到 executionStack
- **AND** 保存节点元数据（stepId、componentName、outputData）
- **AND** 保存到 Redis key: `saga:execution:{executionId}:stack`

#### Scenario: 执行栈不追加只读节点
- **WHEN** 只读节点成功执行
- **AND** `needsCompensation=false`
- **THEN** 不将该节点加入 executionStack

#### Scenario: 补偿时读取执行栈
- **WHEN** 触发补偿流程
- **THEN** 系统从 Redis 读取 executionStack
- **AND** 按相反顺序遍历
- **AND** 对每个需要补偿的节点执行补偿操作

### Requirement: Redis 热数据存储
系统 SHALL 使用 Redis 存储执行中的 Saga 状态以支持快速访问。

#### Scenario: Redis 存储完整执行状态
- **WHEN** Saga 开始执行
- **THEN** 系统在 Redis 创建 key: `saga:execution:{executionId}`
- **AND** 存储完整的 `SagaExecution` JSON
- **AND** 包含 executionId, chainName, status, currentStepIndex, executionStack
- **AND** 设置 TTL 为 24 小时

#### Scenario: Redis 快速查询执行状态
- **WHEN** 客户端查询执行状态
- **THEN** 系统优先从 Redis 读取
- **AND** 响应时间 < 10ms
- **AND** 如果 Redis key 不存在，从 MySQL 加载并回写 Redis

#### Scenario: Redis 故障降级
- **WHEN** Redis 不可用
- **THEN** 系统降级为直接从 MySQL 读写
- **AND** 记录降级日志
- **AND** 发送告警通知

### Requirement: MySQL 持久化存储
系统 SHALL 将执行历史持久化到 MySQL 用于审计和历史查询。

#### Scenario: MySQL 异步写入成功状态
- **WHEN** 节点成功执行
- **THEN** 系统同步更新 Redis
- **AND** 异步写入 MySQL（使用消息队列或线程池）
- **AND** MySQL 写入失败不影响主流程

#### Scenario: MySQL 同步写入失败状态
- **WHEN** 节点执行失败
- **THEN** 系统同步写入 MySQL
- **AND** 确保失败数据不丢失
- **AND** 如果 MySQL 写入失败，记录到错误日志并重试

#### Scenario: MySQL 历史数据查询
- **WHEN** 用户查询最近 7 天的执行记录
- **THEN** 系统从 MySQL 查询
- **AND** 支持按租户、流程名称、状态、时间范围筛选
- **AND** 支持分页（默认 20 条/页）

### Requirement: 状态转换管理
系统 SHALL 定义清晰的状态转换规则并强制执行。

#### Scenario: 正常执行状态转换
- **WHEN** Saga 开始执行
- **THEN** 状态从 `PENDING` → `RUNNING`
- **AND** 所有节点成功后 → `COMPLETED`
- **AND** 任一节点失败 → `COMPENSATING` 或 `MANUAL_INTERVENTION`
- **AND** 补偿完成后 → `COMPENSATED`

#### Scenario: 非法状态转换拒绝
- **WHEN** Saga 当前状态为 `COMPLETED`
- **AND** 系统收到转换为 `RUNNING` 的请求
- **THEN** 拒绝状态转换
- **AND** 抛出 `IllegalStateTransitionException`
- **AND** 记录审计日志

#### Scenario: 状态转换审计
- **WHEN** Saga 状态发生转换
- **THEN** 记录状态转换历史
- **AND** 包含：fromStatus, toStatus, timestamp, reason
- **AND** 存储到 MySQL（单独的状态转换表）

### Requirement: 执行数据版本控制
系统 SHALL 支持执行数据的快照和版本控制。

#### Scenario: 节点执行前数据快照
- **WHEN** 节点开始执行
- **AND** 节点配置 `snapshotBeforeExecution=true`
- **THEN** 系统创建执行前的数据快照
- **AND** 存储到 `executionSnapshots` 表
- **AND** 用于补偿时恢复原始状态

#### Scenario: 补偿时使用快照数据
- **WHEN** 补偿组件需要原始数据
- **THEN** 系统从快照恢复数据
- **AND** 将快照数据传递给补偿组件
- **AND** 确保补偿操作的准确性

### Requirement: 过期数据清理
系统 SHALL 定期清理过期的执行数据。

#### Scenario: XXL-JOB 定时清理 Redis 过期数据
- **WHEN** XXL-JOB 定时任务触发（每天凌晨 2 点）
- **THEN** 扫描 Redis 中已完成的 Saga 记录
- **AND** 删除超过 24 小时的已完成记录
- **AND** 删除超过 7 天的失败记录

#### Scenario: XXL-JOB 归档 MySQL 历史数据
- **WHEN** XXL-JOB 定时任务触发（每周日凌晨 3 点）
- **THEN** 将 90 天前的执行记录移动到归档表
- **AND** 从主表删除已归档的记录
- **AND** 记录归档统计信息

#### Scenario: 手动触发清理
- **WHEN** 管理员调用清理 API
- **AND** 指定清理条件（时间范围、状态）
- **THEN** 系统异步执行清理任务
- **AND** 返回任务 ID 用于跟踪进度

### Requirement: 租户数据隔离
系统 SHALL 确保多租户环境下执行状态的隔离性。

#### Scenario: 租户隔离的 Redis Key
- **WHEN** 存储租户 A 的执行状态
- **THEN** Redis key 格式为 `saga:execution:{tenantId}:{executionId}`
- **AND** 查询时自动添加租户前缀
- **AND** 租户 A 无法访问租户 B 的数据

#### Scenario: 租户隔离的 MySQL 查询
- **WHEN** 租户 A 查询执行记录
- **THEN** MySQL 查询自动添加 `WHERE tenant_id = ?`
- **AND** 租户 A 只能看到自己的数据

### Requirement: 并发控制
系统 SHALL 处理同一执行实例的并发更新。

#### Scenario: 乐观锁防止并发冲突
- **WHEN** 多个线程尝试更新同一个 SagaExecution
- **THEN** 使用乐观锁（version 字段）控制
- **AND** 更新时检查 version 是否匹配
- **AND** 如果不匹配，抛出 `OptimisticLockException`
- **AND** 自动重试或返回错误

#### Scenario: Redis 分布式锁
- **WHEN** 关键操作需要互斥（如触发补偿）
- **THEN** 使用 Redis 分布式锁
- **AND** Key 格式：`saga:lock:{executionId}`
- **AND** 锁定时间 30 秒
- **AND** 操作完成后释放锁

### Requirement: 执行统计和指标
系统 SHALL 收集和暴露执行统计指标。

#### Scenario: 实时统计指标
- **WHEN** 系统运行时
- **THEN** 收集以下指标：
  - 总执行次数（按租户、流程）
  - 成功率
  - 失败率
  - 补偿率
  - 平均执行时间
  - P50/P95/P99 延迟

#### Scenario: 查询统计指标
- **WHEN** 运维人员查询统计指标
- **THEN** 提供 API: `/api/saga/statistics`
- **AND** 支持按时间范围、租户、流程聚合
- **AND** 返回格式化的统计结果
