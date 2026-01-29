## ADDED Requirements

### Requirement: 规则创建 API
系统 MUST 提供 API 用于创建新的规则配置。

#### Scenario: 创建规则
- **WHEN** 发送 POST 请求到 /api/rules 且请求体包含规则配置时
- **THEN** 系统 MUST 创建规则并返回新创建的规则详情

### Requirement: 规则查询 API
系统 MUST 提供 API 用于查询规则列表和详情。

#### Scenario: 查询规则列表
- **WHEN** 发送 GET 请求到 /api/rules 时
- **THEN** 系统 MUST 返回所有规则的分页列表

#### Scenario: 查询规则详情
- **WHEN** 发送 GET 请求到 /api/rules/{ruleId} 时
- **THEN** 系统 MUST 返回指定规则的详细信息

### Requirement: 规则更新 API
系统 MUST 提供 API 用于更新现有规则配置。

#### Scenario: 更新规则
- **WHEN** 发送 PUT 请求到 /api/rules/{ruleId} 且请求体包含更新内容时
- **THEN** 系统 MUST 更新规则配置并创建新版本

### Requirement: 规则删除 API
系统 MUST 提供 API 用于删除规则配置。

#### Scenario: 删除规则
- **WHEN** 发送 DELETE 请求到 /api/rules/{ruleId} 时
- **THEN** 系统 MUST 将规则标记为已删除（软删除）

### Requirement: 规则启用/禁用 API
系统 MUST 提供 API 用于启用或禁用规则。

#### Scenario: 启用规则
- **WHEN** 发送 POST 请求到 /api/rules/{ruleId}/enable 时
- **THEN** 规则状态 MUST 更新为启用且可用于执行

#### Scenario: 禁用规则
- **WHEN** 发送 POST 请求到 /api/rules/{ruleId}/disable 时
- **THEN** 规则状态 MUST 更新为禁用且不可用于执行

### Requirement: 规则版本管理 API
系统 MUST 提供管理规则版本的 API。

#### Scenario: 查询规则版本列表
- **WHEN** 发送 GET 请求到 /api/rules/{ruleId}/versions 时
- **THEN** 系统 MUST 返回该规则的所有历史版本

#### Scenario: 回滚到指定版本
- **WHEN** 发送 POST 请求到 /api/rules/{ruleId}/versions/{versionId}/rollback 时
- **THEN** 系统 MUST 将规则回滚到指定版本

### Requirement: 规则验证 API
系统 MUST 提供 API 用于验证规则配置的格式和语法。

#### Scenario: 验证规则配置
- **WHEN** 发送 POST 请求到 /api/rules/validate 且请求体包含规则配置时
- **THEN** 系统 MUST 返回验证结果，如果格式错误则返回详细错误信息

### Requirement: API 权限控制
系统 MUST 对规则管理 API 进行权限控制。

#### Scenario: 无权限访问被拒绝
- **WHEN** 没有规则管理权限的用户访问规则管理 API 时
- **THEN** 系统 MUST 返回 403 Forbidden 错误

### Requirement: API 请求参数验证
系统 MUST 对规则管理 API 的请求参数进行验证。

#### Scenario: 参数验证失败
- **WHEN** 请求参数不符合要求时
- **THEN** 系统 MUST 返回 400 Bad Request 错误及详细的错误信息
