## ADDED Requirements

### Requirement: XML 文件配置支持
系统 MUST 支持通过 XML 文件配置稳定的业务流程规则。

#### Scenario: 从 XML 文件加载规则
- **WHEN** LiteFlow 引擎从指定 XML 路径加载规则时
- **THEN** 规则链 MUST 正确解析并可用于执行

### Requirement: 动态配置加载
系统 MUST 支持从数据库或配置中心动态加载可变的业务规则。

#### Scenario: 从数据库加载动态规则
- **WHEN** 请求从数据库加载规则时
- **THEN** 系统 MUST 查询数据库并返回最新版本的规则

#### Scenario: 从配置中心加载动态规则
- **WHEN** 请求从配置中心加载规则时
- **THEN** 系统 MUST 从配置中心获取最新规则配置

### Requirement: 规则配置验证
系统 MUST 在加载规则配置时进行格式和语法验证。

#### Scenario: 规则格式验证通过
- **WHEN** 加载格式正确的规则配置时
- **THEN** 验证 MUST 通过且规则成功加载

#### Scenario: 规则格式验证失败
- **WHEN** 加载格式错误的规则配置时
- **THEN** 系统 MUST 返回详细的错误信息

### Requirement: 配置热更新
系统 MUST 支持在不重启服务的情况下更新动态规则配置。

#### Scenario: 动态规则热更新
- **WHEN** 规则配置在数据库中更新时
- **THEN** 系统 MUST 自动重新加载最新规则配置

### Requirement: 配置文件路径配置
系统 MUST 允许通过 application.yml 配置规则 XML 文件的加载路径。

#### Scenario: 配置规则文件路径
- **WHEN** 在 application.yml 中配置 liteflow.rule-source 配置项时
- **THEN** 系统 MUST 从指定路径加载规则文件
