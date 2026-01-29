## ADDED Requirements

### Requirement: 规则版本存储
系统 MUST 为每个规则配置保存历史版本记录。

#### Scenario: 保存新版本
- **WHEN** 创建或更新规则配置时
- **THEN** 系统 MUST 自动生成新的版本号并保存完整配置

### Requirement: 版本号生成规则
系统 MUST 使用语义化版本号规则（如 v1.0.0, v1.0.1）标识规则版本。

#### Scenario: 生成新版本号
- **WHEN** 保存规则的新版本时
- **THEN** 版本号 MUST 按照语义化规则递增

### Requirement: 版本历史查询
系统 MUST 支持查询指定规则的所有历史版本。

#### Scenario: 查询版本列表
- **WHEN** 请求查询某规则的所有版本时
- **THEN** 系统 MUST 返回该规则的所有历史版本列表

### Requirement: 版本内容对比
系统 MUST 支持对比两个不同版本之间的差异。

#### Scenario: 对比版本差异
- **WHEN** 请求对比两个版本时
- **THEN** 系统 MUST 返回详细的内容差异对比结果

### Requirement: 版本回滚
系统 MUST 支持将规则回滚到指定的历史版本。

#### Scenario: 回滚到指定版本
- **WHEN** 请求回滚到某历史版本时
- **THEN** 系统 MUST 将当前规则恢复为该版本的内容

### Requirement: 版本状态管理
系统 MUST 标记每个版本的状态（草稿、已发布、已废弃）。

#### Scenario: 标记版本状态
- **WHEN** 规则版本被创建或更新时
- **THEN** 系统 MUST 自动设置版本状态为草稿

#### Scenario: 发布版本
- **WHEN** 发布某个版本时
- **THEN** 该版本状态 MUST 更新为已发布

### Requirement: 当前版本标识
系统 MUST 明确标识每个规则的当前生效版本。

#### Scenario: 获取当前版本
- **WHEN** 查询规则详情时
- **THEN** 系统 MUST 标识出当前正在使用的版本
