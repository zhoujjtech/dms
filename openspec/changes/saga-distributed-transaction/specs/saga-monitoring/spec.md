# 监控管理规范

## ADDED Requirements

### Requirement: 执行列表查询
系统 SHALL 提供执行列表查询 API，支持多维度筛选和分页。

#### Scenario: 查询所有执行记录
- **WHEN** 调用 `GET /api/saga/executions`
- **THEN** 返回分页的执行记录列表
- **AND** 每条记录包含：executionId, chainName, status, startedAt
- **AND** 默认按开始时间倒序排列
- **AND** 默认每页 20 条

#### Scenario: 按租户筛选
- **WHEN** 调用 `GET /api/saga/executions?tenantId=1`
- **THEN** 只返回租户 1 的执行记录
- **AND** 自动应用租户隔离

#### Scenario: 按状态筛选
- **WHEN** 调用 `GET /api/saga/executions?status=COMPENSATING`
- **THEN** 只返回补偿中的执行记录
- **AND** 支持多状态：`?status=COMPENSATING,MANUAL_INTERVENTION`

#### Scenario: 按时间范围筛选
- **WHEN** 调用 `GET /api/saga/executions?startTime=2026-02-01&endTime=2026-02-03`
- **THEN** 返回指定时间范围内的执行记录
- **AND** 时间格式为 ISO 8601

#### Scenario: 按流程名称筛选
- **WHEN** 调用 `GET /api/saga/executions?chainName=orderProcess`
- **THEN** 只返回该流程的执行记录

#### Scenario: 分页查询
- **WHEN** 调用 `GET /api/saga/executions?page=1&size=50`
- **THEN** 返回第 1 页，每页 50 条记录
- **AND** 响应包含总数、总页数、当前页信息

### Requirement: 执行详情查询
系统 SHALL 提供单个执行实例的完整详情。

#### Scenario: 查询执行详情
- **WHEN** 调用 `GET /api/saga/executions/{executionId}`
- **THEN** 返回完整的 `SagaExecution` 数据
- **AND** 包含：基本信息、执行栈、补偿日志、状态转换历史
- **AND** 包含所有步骤的详细信息和输出数据

#### Scenario: 查询不存在的执行
- **WHEN** 调用 `GET /api/saga/executions/non-existent-id`
- **THEN** 返回 HTTP 404
- **AND** 错误消息：`Execution not found`

### Requirement: 执行流程可视化
系统 SHALL 提供执行流程的时间线可视化数据。

#### Scenario: 获取执行时间线数据
- **WHEN** 调用 `GET /api/saga/executions/{executionId}/timeline`
- **THEN** 返回时间线数据结构
- **AND** 包含每个节点的：
  - 节点名称
  - 执行时间范围（startedAt ~ completedAt）
  - 状态（成功/失败/补偿中）
  - 持续时间
  - 错误信息（如果失败）

#### Scenario: 时间线数据支持前端渲染
- **WHEN** 前端收到时间线数据
- **THEN** 数据格式易于渲染为甘特图或流程图
- **AND** 节点按执行顺序排列
- **AND** 补偿节点以不同颜色或样式标识

#### Scenario: 可视化显示补偿流程
- **WHEN** 执行包含补偿操作
- **THEN** 时间线显示原始节点和补偿节点的对应关系
- **AND** 使用虚线或箭头连接原始节点和补偿节点

### Requirement: 人工操作入口
系统 SHALL 提供 API 供运维人员进行人工干预。

#### Scenario: 手动触发补偿
- **WHEN** 调用 `POST /api/saga/executions/{executionId}/compensate`
- **AND** 请求体包含操作人和原因
- **THEN** 系统启动补偿流程
- **AND** 记录操作类型为 `MANUAL_COMPENSATION`
- **AND** 记录操作人和操作时间

#### Scenario: 重试失败节点
- **WHEN** 调用 `POST /api/saga/executions/{executionId}/retry`
- **AND** 请求体包含 `stepId`
- **THEN** 系统重新执行该节点
- **AND** 可以修改输入数据（可选）
- **AND** 如果成功，继续执行后续节点

#### Scenario: 跳过失败节点继续执行
- **WHEN** 调用 `POST /api/saga/executions/{executionId}/skip`
- **AND** 请求体包含 `stepId` 和跳过原因
- **THEN** 标记该节点为 `SKIPPED`
- **AND** 继续执行后续节点
- **AND** 记录跳过原因和操作人

#### Scenario: 人工决策处理
- **WHEN** Saga 状态为 `MANUAL_INTERVENTION`
- **AND** 调用 `POST /api/saga/executions/{executionId}/manual-decision`
- **AND** 请求体包含 `decision`（CONTINUE/COMPENSATE/RETRY）
- **THEN** 系统根据决策执行相应操作
- **AND** 如果是 CONTINUE，从当前节点继续执行
- **AND** 如果是 COMPENSATE，执行补偿流程
- **AND** 如果是 RETRY，重新执行失败节点

### Requirement: 操作权限控制
系统 SHALL 确保只有授权人员才能执行人工操作。

#### Scenario: 验证操作权限
- **WHEN** 用户尝试手动触发补偿
- **THEN** 系统检查用户是否具有 `SAGA_MANAGE` 权限
- **AND** 如果无权限，返回 HTTP 403
- **AND** 记录未授权访问尝试到审计日志

#### Scenario: 操作审计日志
- **WHEN** 用户执行任何人工操作（补偿、重试、跳过、决策）
- **THEN** 记录到操作审计日志
- **AND** 包含：操作人、操作类型、操作时间、IP 地址、操作结果
- **AND** 存储到 MySQL 审计表

### Requirement: 执行日志查询
系统 SHALL 提供执行日志的查询接口。

#### Scenario: 查询执行日志
- **WHEN** 调用 `GET /api/saga/executions/{executionId}/logs`
- **THEN** 返回该执行的所有日志
- **AND** 按时间正序排列
- **AND** 包含：timestamp, level, logger, message
- **AND** 支持按日志级别筛选（INFO/WARN/ERROR）

#### Scenario: 按时间范围筛选日志
- **WHEN** 调用 `GET /api/saga/executions/{executionId}/logs?startTime=...&endTime=...`
- **THEN** 返回指定时间范围内的日志
- **AND** 支持时间范围查询

#### Scenario: 日志导出
- **WHEN** 调用 `GET /api/saga/executions/{executionId}/logs/export`
- **AND** 指定格式为 `txt` 或 `json`
- **THEN** 返回可下载的日志文件
- **AND** Content-Type 根据格式设置

### Requirement: 告警和通知
系统 SHALL 在特定场景下发送告警通知。

#### Scenario: 补偿失败告警
- **WHEN** 补偿操作执行失败
- **AND** 重试次数达到上限
- **THEN** 发送告警通知
- **AND** 告警包含：executionId, 失败节点, 错误原因
- **AND** 通知方式：邮件、企业微信、钉钉（根据配置）

#### Scenario: 人工介入提醒
- **WHEN** Saga 状态变更为 `MANUAL_INTERVENTION`
- **THEN** 发送提醒通知给管理员
- **AND** 通知包含：executionId, 流程名称, 等待原因, 处理链接

#### Scenario: 执行超时告警
- **WHEN** Saga 执行时间超过配置的阈值
- **THEN** 发送超时告警
- **AND** 包含：executionId, 执行时长, 超时阈值

#### Scenario: 告警规则配置
- **WHEN** 租户需要自定义告警规则
- **THEN** 支持配置告警条件：
  - 失败率超过阈值
  - 补偿率超过阈值
  - 人工介入率超过阈值
  - 执行时间超过阈值

### Requirement: 统计指标展示
系统 SHALL 提供统计指标的查询和展示。

#### Scenario: 查询整体统计
- **WHEN** 调用 `GET /api/saga/statistics`
- **THEN** 返回以下指标：
  - 总执行次数
  - 成功率
  - 失败率
  - 补偿率
  - 人工介入率
  - 平均执行时间

#### Scenario: 按流程聚合统计
- **WHEN** 调用 `GET /api/saga/statistics?groupBy=chain`
- **THEN** 返回每个流程的统计指标
- **AND** 按流程名称分组聚合

#### Scenario: 按时间范围统计
- **WHEN** 调用 `GET /api/saga/statistics?startTime=...&endTime=...`
- **THEN** 返回指定时间范围内的统计数据
- **AND** 支持按天、小时聚合

#### Scenario: 趋势数据查询
- **WHEN** 调用 `GET /api/saga/statistics/trend`
- **AND** 指定时间范围和粒度（day/hour）
- **THEN** 返回时间序列趋势数据
- **AND** 用于绘制趋势图表

### Requirement: 管理界面集成
系统 SHALL 提供管理界面的数据接口支持前端展示。

#### Scenario: 首页概览数据
- **WHEN** 管理员访问 Saga 管理首页
- **THEN** 系统提供 `GET /api/saga/dashboard` 接口
- **AND** 返回：
  - 今日执行次数、成功率
  - 当前运行中的数量
  - 待人工处理数量
  - 最近 7 天趋势数据

#### Scenario: 执行详情页面数据
- **WHEN** 管理员访问执行详情页面
- **THEN** 系统提供单个接口返回所有必要数据
- **AND** 包含：执行信息、步骤列表、时间线、日志、操作历史
- **AND** 减少前端请求次数

#### Scenario: 批量操作支持
- **WHEN** 管理员选择多个执行记录进行批量操作
- **THEN** 提供 `POST /api/saga/executions/batch-compensate` 接口
- **AND** 支持批量补偿、批量重试等操作
- **AND** 返回批量操作的结果统计

### Requirement: 实时状态推送
系统 SHALL 支持实时推送执行状态变化。

#### Scenario: WebSocket 实时推送
- **WHEN** 管理员打开执行详情页面
- **AND** 系统支持 WebSocket 连接
- **THEN** 建立 WebSocket 连接：`ws://api/saga/executions/{executionId}/stream`
- **AND** 实时推送状态变化、节点执行进度
- **AND** 连接断开时自动重连

#### Scenario: 服务端事件发送
- **WHEN** Saga 状态发生变化
- **THEN** 通过 WebSocket 推送事件到所有订阅的客户端
- **AND** 事件包含：eventType, executionId, newData, timestamp

#### Scenario: 客户端订阅特定事件
- **WHEN** 客户端只关心特定类型的事件
- **THEN** 支持订阅过滤
- **AND** 如只订阅状态变化或节点完成事件
