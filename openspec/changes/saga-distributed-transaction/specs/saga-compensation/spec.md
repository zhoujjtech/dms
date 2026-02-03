# 补偿机制规范

## ADDED Requirements

### Requirement: 组件补偿关系声明
系统 SHALL 支持为业务组件声明对应的补偿组件。

#### Scenario: 声明补偿组件
- **WHEN** 业务组件 `createOrder` 配置 `compensateComponent="cancelOrder"`
- **THEN** 系统在 `createOrder` 执行失败需要补偿时
- **AND** 自动调用 `cancelOrder` 组件
- **AND** 将 `createOrder` 的输出数据传递给 `cancelOrder`

#### Scenario: 无需补偿的只读组件
- **WHEN** 组件 `validateOrder` 配置 `needsCompensation=false`
- **THEN** 该组件失败或后续节点失败时
- **AND** 不调用任何补偿组件
- **AND** 从 executionStack 中跳过该组件

### Requirement: 按相反顺序执行补偿
系统 SHALL 在触发补偿时，按照节点执行的相反顺序调用补偿组件。

#### Scenario: 多个节点按相反顺序补偿
- **WHEN** 流程执行顺序为 A → B → C → D
- **AND** 节点 D 失败
- **AND** 节点 A、B、C 都需要补偿
- **THEN** 补偿执行顺序为 C → B → A
- **AND** 每个补偿组件都能访问对应原始节点的输出数据

#### Scenario: 部分节点需要补偿
- **WHEN** 流程执行顺序为 A → B → C → D
- **AND** 节点 D 失败
- **AND** 只有 B 和 D 需要补偿（A、C 为只读操作）
- **THEN** 只执行 B 的补偿组件
- **AND** 跳过 A 和 C

### Requirement: 补偿组件数据访问
系统 SHALL 确保补偿组件能够访问原始节点的完整输出数据。

#### Scenario: 补偿组件获取原始输出
- **WHEN** `reserveStock` 节点输出 `{ "sku": "12345", "qty": 10, "reservationId": "RES-001" }`
- **AND** 后续节点失败触发补偿
- **THEN** `releaseStock` 补偿组件从上下文获取完整输出数据
- **AND** 使用 `reservationId` 调用库存服务释放库存

#### Scenario: 补偿组件获取上下文数据
- **WHEN** 原始节点执行时将 `orderId` 存入上下文
- **AND** 补偿组件需要该 ID 执行回滚
- **THEN** 补偿组件通过 `this.getContext().getData("originalNode.output")` 访问
- **AND** 或通过 SagaStateService 查询原始节点的输出数据

### Requirement: 补偿状态追踪
系统 SHALL 记录每个补偿操作的执行状态。

#### Scenario: 补偿成功记录
- **WHEN** 补偿组件 `cancelOrder` 执行成功
- **THEN** 系统更新 `StepExecution` 的 `compensatedAt` 时间戳
- **AND** 补偿状态为 `COMPENSATED`
- **AND** 记录到 `compensationLog`

#### Scenario: 补偿失败处理
- **WHEN** 补偿组件 `releaseStock` 执行失败
- **THEN** 系统记录补偿失败到 `compensationLog`
- **AND** 标记 Saga 状态为 `COMPENSATION_FAILED`
- **AND** 发送告警通知
- **AND** 继续执行后续节点的补偿（不中断补偿流程）

#### Scenario: 补偿状态查询
- **WHEN** 查询 Saga 执行详情
- **THEN** 显示每个节点的补偿状态
- **AND** 原始节点状态为 `COMPLETED`
- **AND** 补偿状态为 `COMPENSATED` 或 `COMPENSATION_FAILED`

### Requirement: 补偿幂等性
系统 SHALL 确保补偿操作的幂等性，避免重复执行造成数据不一致。

#### Scenario: 重复补偿请求
- **WHEN** 补偿组件 `cancelOrder` 已成功执行
- **AND** 系统再次尝试执行该补偿
- **THEN** 补偿组件检查订单状态
- **AND** 如果订单已取消，直接返回成功
- **AND** 不执行重复的取消操作

#### Scenario: 幂等性检查机制
- **WHEN** 补偿组件需要实现幂等性
- **THEN** 补偿组件通过唯一标识（如 `compensationId`）检查是否已执行
- **AND** 或检查业务实体的状态是否已回滚

### Requirement: 补偿失败重试
系统 SHALL 支持补偿操作失败后的自动重试。

#### Scenario: 补偿自动重试
- **WHEN** 补偿组件执行失败
- **AND** 错误类型为网络超时
- **AND** 重试次数 < 3
- **THEN** 系统自动重试补偿操作
- **AND** 每次重试间隔递增（1s, 2s, 4s）

#### Scenario: 补偿重试超限
- **WHEN** 补偿组件重试次数达到上限仍失败
- **THEN** 停止自动重试
- **AND** 标记补偿状态为 `COMPENSATION_FAILED`
- **AND** 记录到补偿日志
- **AND** 发送告警通知运维人员

### Requirement: 手动触发补偿
系统 SHALL 支持通过 API 手动触发补偿流程。

#### Scenario: 管理员手动补偿
- **WHEN** Saga 状态为 `MANUAL_INTERVENTION`
- **AND** 管理员通过 API 调用 `/api/saga/executions/{id}/compensate`
- **THEN** 系统启动补偿流程
- **AND** 按相反顺序执行已执行节点的补偿
- **AND** 记录操作人为手动触发

#### Scenario: 手动补偿部分节点
- **WHEN** 管理员只想补偿特定节点
- **AND** 调用补偿 API 并指定 `stepIds`
- **THEN** 系统只补偿指定的节点
- **AND** 其他节点保持原状态

### Requirement: 补偿日志审计
系统 SHALL 记录完整的补偿操作日志用于审计。

#### Scenario: 补偿操作日志
- **WHEN** 补偿组件执行
- **THEN** 记录到 `saga_compensation_log` 表
- **AND** 包含字段：executionId, stepId, compensateComponent, status, compensatedAt
- **AND** 如果失败，记录 errorMessage 和 stackTrace
- **AND** 如果是手动触发，记录 operator

#### Scenario: 查询补偿历史
- **WHEN** 审计人员查询某个 Saga 的补偿历史
- **THEN** 系统返回该 Saga 的所有补偿操作
- **AND** 按时间正序排列
- **AND** 显示每个补偿的成功/失败状态

### Requirement: 嵌套补偿
系统 SHALL 支持子流程（subChain）的补偿。

#### Scenario: 子流程补偿
- **WHEN** 主流程调用子流程 `subChain(paymentProcess)`
- **AND** 子流程内部节点失败
- **THEN** 先补偿子流程内部的节点
- **AND** 子流程补偿完成后，返回主流程继续补偿

#### Scenario: 子流程整体补偿
- **WHEN** 子流程配置了整体补偿组件
- **AND** 子流程执行失败
- **THEN** 调用子流程的整体补偿组件
- **AND** 而不是逐个补偿子流程内部节点

### Requirement: 补偿回滚策略
系统 SHALL 支持部分补偿失败时的回滚策略。

#### Scenario: 补偿失败时继续补偿
- **WHEN** 补偿 C 节点失败
- **THEN** 系统记录失败
- **AND** 继续补偿 B 节点和 A 节点
- **AND** 最终标记 Saga 为 `PARTIALLY_COMPENSATED`

#### Scenario: 补偿失败时停止补偿
- **WHEN** 补偿 C 节点失败
- **AND** 流程配置 `compensationFailureStrategy=STOP_ON_FAILURE`
- **THEN** 系统停止后续补偿
- **AND** 标记 Saga 为 `COMPENSATION_FAILED`
- **AND** 发送紧急告警
