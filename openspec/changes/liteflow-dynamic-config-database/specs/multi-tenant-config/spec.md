## ADDED Requirements

### Requirement: 支持租户级别的配置隔离
系统 SHALL 支持多租户场景下的配置隔离，不同租户使用独立的流程链和组件配置。

#### Scenario: 不同租户使用独立的流程链配置
- **WHEN** 租户 A 和租户 B 创建相同名称的流程链
- **THEN** 系统通过 `tenant_id` 字段区分配置
- **AND** 租户 A 的执行请求仅使用租户 A 的流程链配置
- **AND** 租户 B 的执行请求仅使用租户 B 的流程链配置
- **AND** 两个租户的配置互不干扰

#### Scenario: 不同租户使用独立的组件配置
- **WHEN** 租户 A 和租户 B 创建相同名称的组件
- **THEN** 系统通过 `tenant_id` 字段区分配置
- **AND** 租户 A 的流程链使用租户 A 的组件
- **AND** 租户 B 的流程链使用租户 B 的组件
- **AND** 两个租户的组件互不干扰

### Requirement: 从租户上下文获取租户ID
系统 SHALL 从请求上下文中获取租户ID，并使用该租户ID加载对应的配置。

#### Scenario: 从请求头获取租户ID
- **WHEN** 客户端发送流程执行请求并携带 `X-Tenant-Id` 请求头
- **THEN** 系统从请求头中提取租户ID
- **AND** 系统加载该租户的流程链和组件配置
- **AND** 系统使用租户专属配置执行流程

#### Scenario: 请求中缺少租户ID时使用默认租户
- **WHEN** 客户端发送流程执行请求但未携带租户ID
- **THEN** 系统使用默认租户ID（如 1 或 `default`）
- **AND** 系统记录 WARN 级别日志
- **AND** 系统使用默认租户的配置执行流程

### Requirement: 租户配置的查询过滤
系统 SHALL 在查询配置时按租户ID进行过滤。

#### Scenario: 查询流程链时按租户过滤
- **WHEN** LiteFlow SQL 插件查询流程链配置
- **THEN** 系统添加 `tenant_id = ?` 查询条件
- **AND** 系统使用当前请求上下文中的租户ID
- **AND** 系统仅返回该租户的流程链配置

#### Scenario: 查询组件时按租户过滤
- **WHEN** LiteFlow SQL 插件查询组件配置
- **THEN** 系统添加 `tenant_id = ?` 查询条件
- **AND** 系统使用当前请求上下文中的租户ID
- **AND** 系统仅返回该租户的组件配置

### Requirement: 租户配置缓存隔离
系统 SHALL 确保不同租户的配置缓存相互隔离。

#### Scenario: 不同租户的配置缓存独立
- **WHEN** 系统缓存租户 A 和租户 B 的配置
- **THEN** 租户 A 的配置缓存使用 `flowConfig::tenantA` 作为缓存键
- **AND** 租户 B 的配置缓存使用 `flowConfig::tenantB` 作为缓存键
- **AND** 刷新租户 A 的配置不影响租户 B 的缓存

#### Scenario: 清除指定租户的配置缓存
- **WHEN** 管理员调用清除缓存 API 并指定租户ID
- **THEN** 系统仅清除指定租户的配置缓存
- **AND** 其他租户的配置缓存保持不变

### Requirement: 租户不存在时的处理
系统 SHALL 在租户不存在或租户没有配置时提供合理的处理方式。

#### Scenario: 租户不存在时返回友好错误
- **WHEN** 客户端使用不存在的租户ID执行流程
- **THEN** 系统返回 `404 Not Found` 错误
- **AND** 错误信息包含 "Tenant not found" 或 "No configuration found for tenant"
- **AND** 系统记录 WARN 级别日志

#### Scenario: 租户没有已发布配置时返回错误
- **WHEN** 客户端使用存在租户ID执行流程，但该租户没有已发布的配置
- **THEN** 系统返回 `400 Bad Request` 错误
- **AND** 错误信息包含 "No published configuration found for tenant"
- **AND** 系统记录 INFO 级别日志
