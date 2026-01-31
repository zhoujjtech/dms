## Context

当前系统是一个基于 Maven 的 Java 项目，需要引入规则引擎和流程编排能力来处理复杂的业务规则和多变的业务流程。现有的业务规则硬编码在代码中，难以维护和扩展，无法快速响应业务变化。

系统采用 Spring Boot 框架，需要在不破坏现有架构的基础上集成 LiteFlow 引擎。需求包括同时支持 XML 文件配置和动态数据库配置两种方式，并提供规则版本管理、测试调试、监控统计和完整的 CRUD 管理接口。

### 约束条件

- 必须使用 Maven 进行依赖管理
- 必须与 Spring Boot 框架无缝集成
- 核心稳定流程使用 XML 配置，可变规则使用动态加载
- 必须提供规则版本管理、测试和监控功能
- 测试环境必须与生产数据隔离

## System Architecture

### 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                              Client Layer                                  │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐             │
│  │   Web UI   │  │  Mobile   │  │  3rd Party│  │   Admin   │             │
│  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘             │
└────────┼───────────────┼───────────────┼───────────────┼───────────────┘
         │               │               │               │
         └───────────────┴───────────────┴───────────────┘
                                 │
         ┌───────────────────────────────┼───────────────────────────────┐
         │                           │                           │
         ▼                           ▼                           ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                      API Gateway / Load Balancer                           │
└────────────────────────────────────────┬────────────────────────────────────────┘
                                     │
         ┌─────────────────────────────┼─────────────────────────────┐
         │                             │                             │
         ▼                             ▼                             ▼
┌─────────────────┐        ┌─────────────────┐        ┌─────────────────┐
│  Application   │        │  Application   │        │  Application   │
│   Server 1    │        │   Server 2    │        │   Server N    │
└───────┬───────┘        └───────┬───────┘        └───────┬───────┘
        │                         │                         │
        │                         │                         │
        ▼                         ▼                         ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                         Spring Boot Application                               │
│  ┌─────────────────────────────────────────────────────────────────────────┐   │
│  │                     REST API Layer                                 │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────────┐  │   │
│  │  │Component │  │  Chain   │  │Version  │  │  Testing    │  │   │
│  │  │Controller│  │Controller│  │Controller│ │ Controller  │  │   │
│  │  └────┬────┘  └────┬────┘  └────┬────┘  └──────┬──────┘  │   │
│  └───────┼───────────────┼───────────────┼────────────┼─────────┘   │
│          │               │               │               │               │   │
│  ┌───────┼───────────────┼───────────────┼────────────┼─────────┐   │
│  │       │               │               │               │         │     │   │
│  │   ┌───▼───┐       ┌───▼───┐       ┌───▼───┐   ┌───▼───┐ │   │
│  │   │Component│       │ Chain  │       │Version │   │Testing │ │   │
│  │   │ Service│       │Service │       │Service │   │Service │ │   │
│  │   └───┬───┘       └───┬───┘       └───┬───┘   └───┬───┘ │   │
│  │       │               │               │               │     │   │   │
│  │   ┌───▼───────────────▼───────────────▼───────────────▼─────┐ │   │
│  │   │              LiteFlow Engine                                 │ │   │
│  │   │  ┌─────────────────────────────────────────────────────┐  │ │   │
│  │   │  │            Flow Executor                              │  │ │   │
│  │   │  │  ┌───────────────────────────────────────────────┐ │ │   │
│  │   │  │  │            Transaction Manager                │ │ │   │
│  │   │  │  └───────────────────────────────────────────────┘ │ │   │
│  │   │  │  ┌───────────────────────────────────────────────┐ │ │   │
│  │   │  │  │            Component Registry               │ │ │   │
│  │   │  │  └───────────────────────────────────────────────┘ │ │   │
│  │   │  │  ┌───────────────────────────────────────────────┐ │ │   │
│  │   │  │  │            Chain Config Loader              │ │ │   │
│  │   │  │  └───────────────────────────────────────────────┘ │ │   │
│  │   │  └─────────────────────────────────────────────────────┘  │ │   │
│  │   └────────────────────┬──────────────────────────────────────┘ │   │
│  │                        │                                    │   │
│  │   ┌────────────────────▼────────────────────────────────────┐ │   │
│  │   │                Rule Components (Business Logic)          │ │   │
│  │   │  ┌─────────┐  ┌─────────┐  ┌─────────┐          │ │   │
│  │   │  │Component │  │Component │  │Component │  ...     │ │   │
│  │   │  │   A     │  │   B     │  │   C     │          │ │   │
│  │   │  └─────────┘  └─────────┘  └─────────┘          │ │   │
│  │   └────────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                      Data Access Layer                          │  │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐              │  │
│  │  │Component │  │  Chain  │  │Version  │              │  │
│  │  │Repository│  │Repository│  │Repository│              │  │
│  │  └────┬────┘  └────┬────┘  └────┬────┘              │  │
│  └───────┼─────────────┼─────────────┼──────────────────────┘  │
│          │             │             │                          │
└──────────┼─────────────┼─────────────┼──────────────────────────┘
           │             │             │
           ▼             ▼             ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                            MySQL Database                               │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐     │
│  │rule_component   │  │  flow_chain    │  │config_version  │     │
│  │                │  │                │  │                │     │
│  │flow_sub_chain  │  │config_test_case│  │execution_      │     │
│  │                │  │                │  │monitoring      │     │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘     │
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────────┐    │
│  │                   Caffeine Cache                             │    │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐               │    │
│  │  │Component │  │  Chain  │  │Version  │               │    │
│  │  │  Cache  │  │  Cache  │  │  Cache  │               │    │
│  │  └─────────┘  └─────────┘  └─────────┘               │    │
│  └─────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
```

### LiteFlow 集成架构

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          Spring Boot Application                          │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────────┐   │
│  │                      Configuration Layer                           │   │
│  │  ┌─────────────────┐  ┌─────────────────┐                   │   │
│  │  │application.yml  │  │   XML Config   │                   │   │
│  │  │(LiteFlow Config)│  │ (flow/*.xml)   │                   │   │
│  │  └────────┬────────┘  └────────┬────────┘                   │   │
│  │           │                      │                             │   │
│  │           └──────────────┬───────┘                             │   │
│  │                          ▼                                     │   │
│  │  ┌───────────────────────────────────────────────────────┐        │   │
│  │  │         LiteFlow Auto Configuration               │        │   │
│  │  │  - Component Scanning                          │        │   │
│  │  │  - Flow Executor Bean                         │        │   │
│  │  │  - Transaction Manager Integration            │        │   │
│  │  └─────────────────────┬─────────────────────────┘        │   │
│  │                        │                                   │   │
│  │                        ▼                                   │   │
│  │  ┌───────────────────────────────────────────────────────┐        │   │
│  │  │            Flow Config Loader                   │        │   │
│  │  │  ┌──────────────────┐  ┌──────────────────┐        │   │
│  │  │  │ XML Loader      │  │ DB Loader       │        │   │
│  │  │  │ (Core Chains)  │  │ (Dynamic Rules) │        │   │
│  │  │  └────────┬────────┘  └────────┬────────┘        │   │
│  │  │           │                     │                  │   │
│  │  │           └──────────┬──────────┘                  │   │
│  │  │                      ▼                             │   │
│  │  │  ┌───────────────────────────────────────┐          │   │
│  │  │  │      Caffeine Local Cache            │          │   │
│  │  │  │  - Chain Configs                 │          │   │
│  │  │  │  - Component Metadata             │          │   │
│  │  │  └───────────────────────────────────────┘          │   │
│  │  └─────────────────────┬─────────────────────────────────┘   │   │
│  │                        │                                   │   │
│  └────────────────────────┼───────────────────────────────────┘   │
│                         │                                       │
│  ┌────────────────────────▼───────────────────────────────────┐   │
│  │                 Flow Executor                             │   │
│  │  ┌───────────────────────────────────────────────────┐  │   │
│  │  │         Component Registry                        │  │   │
│  │  │  ┌─────────┐  ┌─────────┐  ┌─────────┐   │  │   │
│  │  │  │Business │  │Condition│  │  Loop   │   │  │   │
│  │  │  │Components│ │Components│ │Components│   │  │   │
│  │  │  └─────────┘  └─────────┘  └─────────┘   │  │   │
│  │  └───────────────────────────────────────────────────┘  │   │
│  │                                                          │   │
│  │  ┌───────────────────────────────────────────────────┐  │   │
│  │  │         EL Expression Parser                  │  │   │
│  │  │  - THEN, WHEN, IF, FOR, CATCH             │  │   │
│  │  │  - subChain, PRE, FINALLY                │  │   │
│  │  └───────────────────────────────────────────────────┘  │   │
│  │                                                          │   │
│  │  ┌───────────────────────────────────────────────────┐  │   │
│  │  │         Transaction Manager                    │  │   │
│  │  │  - @LiteflowTransaction Support               │  │   │
│  │  │  - Flow-level Transactions                  │  │   │
│  │  │  - Component-level Transactions            │  │   │
│  │  └───────────────────────────────────────────────────┘  │   │
│  └────────────────────────┬──────────────────────────────────┘   │
│                         │                                       │
│  ┌────────────────────────▼───────────────────────────────────┐   │
│  │              Execution Context                          │   │
│  │  - Request Data                                       │   │
│  │  - Context Bean                                       │   │
│  │  - Component Variables                                 │   │
│  └───────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### 组件与流程关系图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        Component Repository                             │
│                                                                          │
│  ┌─────────────┐     ┌─────────────┐     ┌─────────────┐             │
│  │ Component A │     │ Component B │     │ Component C │             │
│  │(Validation) │     │(Check Stock)│     │(Create Order)│             │
│  └──────┬──────┘     └──────┬──────┘     └──────┬──────┘             │
└─────────┼──────────────────┼──────────────────┼──────────────────┘          │
          │                  │                  │                          │
          │                  │                  │                          │
          └──────────────────┼──────────────────┘                          │
                             │                                           │
         ┌─────────────────────▼──────────────────────┐                     │
         │          Flow Chain Repository             │                     │
         │                                         │                     │
         │  ┌─────────────────────────────────────┐  │                     │
         │  │   Order Process Chain            │  │                     │
         │  │   EL: THEN(A, WHEN(B, C), D)    │  │                     │
         │  └───────────┬─────────────────────┘  │                     │
         │              │                           │                     │
         │              ▼                           │                     │
         │  ┌─────────────────────────────────────┐  │                     │
         │  │   VIP Approval Chain             │  │                     │
         │  │   EL: IF(E, THEN(F, G))        │  │                     │
         │  └───────────┬─────────────────────┘  │                     │
         │              │                           │                     │
         │              ▼                           │                     │
         │  ┌─────────────────────────────────────┐  │                     │
         │  │   Sub Chain: Notify Process     │  │                     │
         │  │   EL: THEN(H, I)               │  │                     │
         │  └─────────────────────────────────────┘  │                     │
         └─────────────────────────────────────────────┘                     │
                                                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 数据流向图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          Request Flow                                   │
│                                                                          │
│  Client Request                                                        │
│       │                                                                 │
│       ▼                                                                 │
│  ┌─────────────────┐                                                   │
│  │ REST API Layer │                                                   │
│  │ /api/execute/ │                                                   │
│  │   {chainName}   │                                                   │
│  └────────┬────────┘                                                   │
│           │                                                             │
│           ▼                                                             │
│  ┌─────────────────┐                                                   │
│  │ Execution Service │                                                   │
│  │  - Validate    │                                                   │
│  │  - Prepare     │                                                   │
│  └────────┬────────┘                                                   │
│           │                                                             │
│           ▼                                                             │
│  ┌───────────────────────────────────────────────────────────────────────┐   │
│  │                    LiteFlow Engine                             │   │
│  │                                                               │   │
│  │  1. Load Chain Config                                          │   │
│  │     ┌──────────────┐  ┌──────────────┐                          │   │
│  │     │ XML / Cache  │  │  DB / Cache  │                          │   │
│  │     └──────┬───────┘  └──────┬───────┘                          │   │
│  │            └────────┬─────────┘                                    │   │
│  │                     ▼                                             │   │
│  │            ┌───────────────────┐                                     │   │
│  │            │ EL Expression    │                                     │   │
│  │            │     Parser      │                                     │   │
│  │            └─────────┬───────┘                                     │   │
│  │                      │                                             │   │
│  │                      ▼                                             │   │
│  │  2. Parse EL Expression                                          │   │
│  │     THEN(A, WHEN(B, C), D)                                      │   │
│  │                      │                                             │   │
│  │                      ▼                                             │   │
│  │  3. Execute Components                                           │   │
│  │     ┌───────────────────────────────────────────────┐               │   │
│  │     │ Execution Sequence                     │               │   │
│  │     │                                    │               │   │
│  │     │  ┌──────┐  ┌──────┐  ┌──────┐            │   │
│  │     │  │  A   │  │  B   │  │  C   │            │   │
│  │     │  └───┬──┘  └───┬──┘  └───┬──┘            │   │
│  │     │      │          │          │                  │   │
│  │     │      └────┬─────┘          │                  │   │
│  │     │           │                 │                  │   │
│  │     │           ▼                 ▼                  │   │
│  │     │      ┌──────────┐  ┌──────────┐            │   │
│  │     │      │    D     │  │    E     │            │   │
│  │     │      └────┬─────┘  └────┬─────┘            │   │
│  │     └───────────┼───────────────┼───────────────────┘   │
│  │                 │               │                          │   │
│  │                 ▼               │                          │   │
│  │  4. Handle Transaction                                      │   │
│  │     ┌───────────────────────────────────────┐               │   │
│  │     │ Transaction Manager               │               │   │
│  │     │ - Begin/Commit/Rollback              │               │   │
│  │     └───────────────────────────────────────┘               │   │
│  │                 │                                          │   │
│  │                 ▼                                          │   │
│  │  5. Collect Monitoring Data                                  │   │
│  │     ┌───────────────────────────────────────┐               │   │
│  │     │ Monitoring Collector               │               │   │
│  │     │ - Execution Time                               │               │   │
│  │     │ - Component Status                            │               │   │
│  │     │ - Error Information                          │               │   │
│  │     └───────────────────────────────────────┘               │   │
│  │                 │                                          │   │
│  │                 ▼                                          │   │
│  │  6. Return Result                                            │   │
│  └───────────────────────────────┬──────────────────────────────────┘   │
│                              │                                     │
│                              ▼                                     │
│  ┌─────────────────┐                                                   │
│  │ Response Body  │                                                   │
│  │ {              │                                                   │
│  │   "success": true,                                                 │
│  │   "executionId": "...",                                               │
│  │   "result": {...},                                                 │
│  │   "path": [A, B, C, D]                                             │
│  │ }                                                                  │
│  └──────┬──────────┘                                                   │
│         │                                                             │
│         ▼                                                             │
│   Client Response                                                        │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────────────┘
```

### 监控架构图

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      Monitoring & Tracing Architecture                    │
│                                                                          │
│  ┌───────────────────────────────────────────────────────────────────────┐   │
│  │                   Flow Execution                               │   │
│  │                                                               │   │
│  │  Component A ──► Component B ──► Component C ──► Result   │   │
│  │      │               │               │                           │   │
│  │      ▼               ▼               ▼                           │   │
│  │  ┌─────────┐    ┌─────────┐    ┌─────────┐                │   │
│  │  │ 100ms   │    │  50ms   │    │  200ms  │                │   │
│  │  │ Success │    │ Success │    │ Success │                │   │
│  │  └────┬────┘    └────┬────┘    └────┬────┘                │   │
│  └───────┼───────────────┼───────────────┼──────────────┘          │
│          │               │               │                          │
│          ▼               ▼               ▼                          │
│  ┌───────────────────────────────────────────────────────────────┐        │   │
│  │          Monitoring Collector                         │        │   │
│  │  ┌─────────────────────────────────────────────────────┐  │        │   │
│  │  │         Metrics Collector                     │  │        │   │
│  │  │  - Execution Count                              │  │        │   │
│  │  │  - Success/Failure Rate                        │  │        │   │
│  │  │  - Average Time                               │  │        │   │
│  │  └──────────────────┬──────────────────────────────┘  │        │   │
│  │                   │                                 │        │   │
│  │  ┌────────────────▼──────────────────────────────┐  │        │   │
│  │  │         Trace Collector                     │  │        │   │
│  │  │  - Component Execution Path                  │  │        │   │
│  │  │  - Data Flow                                 │  │        │   │
│  │  │  - Error Location                            │  │        │   │
│  │  └──────────────────┬──────────────────────────────┘  │        │   │
│  │                   │                                 │        │   │
│  │  ┌────────────────▼──────────────────────────────┐  │        │   │
│  │  │         Alert Manager                      │  │        │   │
│  │  │  - Failure Rate Check                        │  │        │   │
│  │  │  - Slow Transaction Alert                   │  │        │   │
│  │  └──────────────────┬──────────────────────────────┘  │        │   │
│  └───────────────────────┼──────────────────────────────────┘          │
│                          │                                             │
│          ┌───────────────┼───────────────┐                          │
│          │               │               │                          │
│          ▼               ▼               ▼                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │   MySQL     │  │ Caffeine    │  │ Alert      │             │
│  │ Monitoring  │  │ Stats Cache │  │ Service    │             │
│  │    Table    │  │             │  │            │             │
│  └─────────────┘  └─────────────┘  └─────┬──────┘             │
│                                        │                         │
│                         ┌──────────────▼──────────────┐             │
│                         │   Email / DingTalk     │             │
│                         │   WeCom Notification   │             │
│                         └─────────────────────────┘             │
└─────────────────────────────────────────────────────────────────────────────┘
```


## Goals / Non-Goals

**Goals:**
- 集成 LiteFlow 引擎，同时支持规则引擎和流程编排能力
- 实现基于 EL 表达式的灵活流程编排，支持串行、并行、条件、循环等编排模式
- 实现规则组件与流程编排的无缝整合，组件可被多个流程复用
- 实现混合配置模式（XML + 动态数据库加载）
- 提供完整的规则和流程版本管理功能
- 实现规则和流程测试调试工具，支持断点和执行路径可视化
- 提供规则和流程执行监控和异常告警能力
- 提供完整的规则和流程管理 REST API

**Non-Goals:**
- 可视化规则编辑器（后期迭代）
- 规则模板化（后期迭代）
- 规则权限细粒度控制（使用 Spring Security 基础权限即可）
- 规则性能分析和优化建议（监控仅提供基础统计）

## Decisions

### 1. LiteFlow 版本选择

**决策**: 使用 LiteFlow 2.12.x 版本

**理由**:
- LiteFlow 2.12.x 是当前稳定的主版本，支持 Spring Boot 3.x
- 提供完善的数据库动态加载支持
- 内置监控和统计功能，减少开发工作量
- 社区活跃，文档完善

**替代方案考虑**:
- LiteFlow 2.11.x：功能相对较旧，不推荐
- Drools：功能更强大但学习曲线陡峭，配置复杂，过度设计

### 2. 混合配置模式架构

**决策**: 核心稳定流程使用 XML 文件，可变规则使用数据库动态加载

**理由**:
- XML 配置适合稳定、版本控制严格的核心流程
- 数据库动态加载适合频繁变更的业务规则
- 混合模式兼顾稳定性和灵活性
- 符合配置管理的最佳实践

**实现方式**:
```yaml
liteflow:
  rule-source:
    configData: classpath:flow/  # XML 文件路径
  script-file-function-data:    # 数据库动态加载
    enabled: true
    sql: "SELECT rule_content FROM rule_config WHERE rule_name = ? AND status = 'ACTIVE'"
```

### 3. 流程编排架构

**决策**: 使用 LiteFlow 的 EL 表达式实现流程编排，支持多种编排模式

**理由**:
- LiteFlow 提供强大的 EL 表达式，支持串行、并行、条件、循环等复杂编排
- 规则组件与流程编排天然融合，组件可被多个流程复用
- EL 表达式简洁易读，非技术人员也能理解流程逻辑
- 支持子流程编排，可实现流程嵌套和复用
- 内置流程执行监控和状态追踪能力

**支持的编排模式**:

1. **串行编排**: `THEN(a, b, c)` - 依次执行 a、b、c 组件
2. **并行编排**: `WHEN(a, b, c)` - 并行执行 a、b、c 组件
3. **条件编排**: `IF(cond, a, b)` - 条件成立执行 a，否则执行 b
4. **循环编排**: `FOR(n).DO(a)` - 循环 n 次执行 a 组件
5. **异常捕获**: `CATCH(a, b)` - a 组件异常时执行 b
6. **子流程编排**: `subChain(subflow)` - 调用子流程
7. **编排模式组合**: `THEN(a, WHEN(b, c), IF(d, e, f))` - 组合多种模式

**流程编排与规则的整合**:

- **规则组件作为最小执行单元**: 每个 `@LiteflowComponent` 注解的类都是可被编排的组件
- **流程链定义业务流程**: 通过 EL 表达式将多个规则组件组合成业务流程
- **规则逻辑内聚在组件中**: 复杂的业务逻辑封装在组件内部，流程编排只关注执行顺序和条件
- **组件复用**: 同一个组件可被多个流程链引用，避免重复开发
- **动态流程编排**: 流程链配置可存储在数据库中，实现动态调整

**示例流程**:
```xml
<!-- 订单审核流程 -->
<chain name="orderApprovalChain">
    THEN(
        validateOrder,           <!-- 验证订单 -->
        WHEN(
            checkStock,          <!-- 库存检查（并行） -->
            calculateAmount      <!-- 金额计算（并行） -->
        ),
        IF(
            isVIPUser,           <!-- 条件判断 -->
            vipApproval,         <!-- VIP 审批 -->
            THEN(
                normalApproval,  <!-- 普通审批 -->
                sendNotify       <!-- 发送通知 -->
            )
        ),
        createOrder             <!-- 创建订单 -->
    )
</chain>
```

**流程配置管理**:
- 核心业务流程使用 XML 文件配置，进行版本控制
- 可变业务流程使用数据库动态加载
- 流程链配置支持参数化，可灵活调整组件执行逻辑
- 子流程支持独立配置和复用

### 4. 数据库表设计

**决策**: 使用 7 张表支持多租户 DDD 架构，每个限界上下文对应相应的数据表

**理由**:
- 每个限界上下文拥有独立的数据表，符合 DDD 边界
- 所有业务表添加 tenant_id 字段，支持多租户完全隔离
- 规则组件表存储可复用的业务组件
- 流程链表存储流程编排配置
- 版本表存储规则和流程的历史版本
- 子流程表支持流程嵌套和复用
- 测试用例表独立存储，与规则/流程版本关联
- 监控数据表使用时序表设计，便于数据聚合
- 租户表支持多租户管理

**表结构与 DDD 上下文映射**:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                    DDD 限界上下文 → 数据库表映射                                 │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Rule Config Bounded Context (规则配置上下文)
   ┌──────────────────────────────────────────────────────────────────────┐
   │ rule_component (规则组件表)                                          │
   │ - 聚合根: RuleComponent                                             │
   │ ├─ id BIGINT PRIMARY KEY AUTO_INCREMENT                             │
   │ ├─ tenant_id BIGINT NOT NULL                # 租户隔离              │
   │ ├─ component_id VARCHAR(50) UNIQUE NOT NULL                         │
   │ ├─ component_name VARCHAR(100) NOT NULL                             │
   │ ├─ description TEXT                                                 │
   │ ├─ component_type VARCHAR(20) NOT NULL   # BUSINESS/CONDITION/LOOP  │
   │ ├─ content TEXT NOT NULL                 # 组件内容（Java代码/脚本）│
   │ ├─ status VARCHAR(20) DEFAULT 'DRAFT'     # DRAFT/PUBLISHED/ARCHIVED│
   │ ├─ created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                   │
   │ ├─ updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP│
   │ └─ INDEX idx_tenant_component(tenant_id, component_id)              │
   └──────────────────────────────────────────────────────────────────────┘

2. Flow Exec Bounded Context (流程执行上下文)
   ┌──────────────────────────────────────────────────────────────────────┐
   │ flow_chain (流程链表)                                                │
   │ - 聚合根: FlowChain                                                 │
   │ ├─ id BIGINT PRIMARY KEY AUTO_INCREMENT                             │
   │ ├─ tenant_id BIGINT NOT NULL                # 租户隔离              │
   │ ├─ chain_name VARCHAR(100) NOT NULL                                 │
   │ ├─ chain_code VARCHAR(50) UNIQUE NOT NULL   # EL表达式              │
   │ ├─ description TEXT                                                 │
   │ ├─ config_type VARCHAR(20) NOT NULL      # XML/DATABASE             │
   │ ├─ status VARCHAR(20) DEFAULT 'DRAFT'     # DRAFT/PUBLISHED/ARCHIVED│
   │ ├─ current_version INT DEFAULT 1                                    │
   │ ├─ transactional TINYINT DEFAULT 0         # 是否启用流程级事务      │
   │ ├─ transaction_timeout INT DEFAULT 30      # 事务超时时间(秒)       │
   │ ├─ transaction_propagation VARCHAR(20) DEFAULT 'REQUIRED'           │
   │ ├─ created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                   │
   │ ├─ updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP│
   │ ├─ deleted_at TIMESTAMP NULL                                          │
   │ └─ UNIQUE INDEX uk_tenant_chain(tenant_id, chain_name)              │
   └──────────────────────────────────────────────────────────────────────┘

   ┌──────────────────────────────────────────────────────────────────────┐
   │ flow_sub_chain (子流程表)                                            │
   │ - 实体: FlowSubChain                                                │
   │ ├─ id BIGINT PRIMARY KEY AUTO_INCREMENT                             │
   │ ├─ tenant_id BIGINT NOT NULL                # 租户隔离              │
   │ ├─ sub_chain_name VARCHAR(100) NOT NULL                             │
   │ ├─ chain_code TEXT NOT NULL               # EL表达式                │
   │ ├─ description TEXT                                                 │
   │ ├─ parent_chain_id BIGINT                                           │
   │ ├─ status VARCHAR(20) DEFAULT 'ACTIVE'    # ACTIVE/INACTIVE         │
   │ ├─ created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                   │
   │ ├─ updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP│
   │ └─ INDEX idx_tenant_subchain(tenant_id, sub_chain_name)              │
   └──────────────────────────────────────────────────────────────────────┘

3. Monitoring Bounded Context (监控上下文)
   ┌──────────────────────────────────────────────────────────────────────┐
   │ execution_monitoring (执行监控表) - 时序表设计                        │
   │ - 聚合根: ExecutionRecord                                           │
   │ ├─ id BIGINT PRIMARY KEY AUTO_INCREMENT                             │
   │ ├─ tenant_id BIGINT NOT NULL                # 租户隔离              │
   │ ├─ chain_id BIGINT NOT NULL                                         │
   │ ├─ component_id VARCHAR(50)                # 组件ID（可为空）       │
   │ ├─ chain_execution_id VARCHAR(50) NOT NULL  # 执行ID                │
   │ ├─ execute_time BIGINT NOT NULL            # 执行耗时(ms)           │
   │ ├─ status VARCHAR(20) NOT NULL             # SUCCESS/FAILURE        │
   │ ├─ error_message TEXT                                                 │
   │ ├─ created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                   │
   │ └─ INDEX idx_tenant_chain(tenant_id, chain_id, created_at)           │
   │ └─ INDEX idx_execution_time(created_at)                              │
   └──────────────────────────────────────────────────────────────────────┘

4. Testing Bounded Context (测试上下文)
   ┌──────────────────────────────────────────────────────────────────────┐
   │ config_test_case (测试用例表)                                        │
   │ - 聚合根: TestCase                                                  │
   │ ├─ id BIGINT PRIMARY KEY AUTO_INCREMENT                             │
   │ ├─ tenant_id BIGINT NOT NULL                # 租户隔离              │
   │ ├─ config_type VARCHAR(20) NOT NULL      # COMPONENT/CHAIN          │
   │ ├─ config_id BIGINT NOT NULL                                        │
   │ ├─ name VARCHAR(100) NOT NULL                                       │
   │ ├─ input_data TEXT NOT NULL               # JSON格式               │
   │ ├─ expected_result TEXT NOT NULL          # JSON格式               │
   │ ├─ created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                   │
   │ └─ INDEX idx_tenant_config(tenant_id, config_type, config_id)        │
   └──────────────────────────────────────────────────────────────────────┘

5. Version Bounded Context (版本管理上下文)
   ┌──────────────────────────────────────────────────────────────────────┐
   │ config_version (配置版本表) - 统一管理规则和流程版本                   │
   │ - 聚合根: ConfigVersion                                             │
   │ ├─ id BIGINT PRIMARY KEY AUTO_INCREMENT                             │
   │ ├─ tenant_id BIGINT NOT NULL                # 租户隔离              │
   │ ├─ config_type VARCHAR(20) NOT NULL      # COMPONENT/CHAIN/SUB_CHAIN│
   │ ├─ config_id BIGINT NOT NULL                                        │
   │ ├─ version INT NOT NULL                                             │
   │ ├─ content TEXT NOT NULL                 # 配置内容                │
   │ ├─ status VARCHAR(20) DEFAULT 'DRAFT'     # DRAFT/PUBLISHED/ARCHIVED│
   │ ├─ created_by VARCHAR(50)                                            │
   │ ├─ created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                   │
   │ └─ UNIQUE INDEX uk_tenant_config_version(tenant_id, config_type, config_id, version)│
   └──────────────────────────────────────────────────────────────────────┘

6. Tenant Bounded Context (租户上下文)
   ┌──────────────────────────────────────────────────────────────────────┐
   │ tenant_info (租户信息表)                                             │
   │ - 聚合根: Tenant                                                    │
   │ ├─ id BIGINT PRIMARY KEY AUTO_INCREMENT                             │
   │ ├─ tenant_code VARCHAR(50) UNIQUE NOT NULL                           │
   │ ├─ tenant_name VARCHAR(100) NOT NULL                                 │
   │ ├─ status VARCHAR(20) DEFAULT 'ACTIVE'    # ACTIVE/SUSPENDED/DELETED│
   │ ├─ max_chains INT DEFAULT 100             # 最大流程链数量          │
   │ ├─ max_components INT DEFAULT 500          # 最大组件数量            │
   │ ├─ executor_cached TINYINT DEFAULT 0      # Executor是否已缓存      │
   │ ├─ created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP                   │
   │ ├─ updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP│
   │ ├─ deleted_at TIMESTAMP NULL                                          │
   │ └─ INDEX idx_tenant_code(tenant_code)                                │
   │ └─ INDEX idx_status(status)                                          │
   └──────────────────────────────────────────────────────────────────────┘
```

**数据库索引策略**:

```sql
-- 租户隔离索引（所有查询必须包含 tenant_id）
CREATE INDEX idx_tenant_component ON rule_component(tenant_id, component_id);
CREATE UNIQUE INDEX uk_tenant_chain ON flow_chain(tenant_id, chain_name);
CREATE INDEX idx_tenant_subchain ON flow_sub_chain(tenant_id, sub_chain_name);
CREATE INDEX idx_tenant_version ON config_version(tenant_id, config_type, config_id);
CREATE INDEX idx_tenant_test ON config_test_case(tenant_id, config_type, config_id);
CREATE INDEX idx_tenant_monitoring ON execution_monitoring(tenant_id, chain_id, created_at);

-- 性能优化索引
CREATE INDEX idx_execution_time ON execution_monitoring(created_at);
CREATE INDEX idx_tenant_code ON tenant_info(tenant_code);
CREATE INDEX idx_status ON tenant_info(status);
```

### 5. 监控数据存储方案

**决策**: 使用 MySQL 存储监控数据，通过定时任务聚合统计

**理由**:
- 项目已使用 MySQL，避免引入新的存储系统
- 监控数据量可控，使用 MySQL 足够
- 通过定时任务实现数据聚合，减少存储开销
- 同时支持规则组件监控和流程链监控

**监控指标**:

1. **流程级监控**:
   - 流程执行次数和成功率
   - 流程平均执行耗时
   - 流程执行路径追踪
   - 流程异常统计

2. **组件级监控**:
   - 组件执行次数和成功率
   - 组件平均执行耗时
   - 组件被引用的流程数量
   - 组件异常统计

3. **链路追踪**:
   - 单次流程执行的完整路径
   - 每个组件的执行时间和状态
   - 组件间的数据传递
   - 异常发生位置和原因

**聚合策略**:
- 原始数据保留 7 天
- 小时级统计数据保留 30 天
- 日级统计数据保留 1 年

### 6. 规则与流程测试隔离方案

**决策**: 使用独立的测试上下文执行器，模拟数据而不修改生产数据

**理由**:
- 不需要搭建独立的测试环境，降低复杂度
- 通过内存 Mock 实现隔离，执行速度快
- 测试结果可重复，便于调试
- 支持规则组件测试和流程链测试两种场景

**实现方式**:
- 创建测试专用的 FlowExecutor
- 使用 Mock 数据源，不连接真实数据库
- 测试执行后清理上下文
- 流程测试提供完整的执行路径追踪

**测试覆盖范围**:
1. **组件级测试**: 测试单个规则组件的输入输出逻辑
2. **流程级测试**: 测试完整流程链的执行路径和结果
3. **子流程测试**: 测试子流程的独立性和复用性
4. **异常场景测试**: 测试异常处理和错误恢复机制

### 7. 版本对比实现方案

**决策**: 使用 Java diff 库进行文本对比

**理由**:
- Java-diff-utils 库成熟稳定
- 支持 HTML 格式化输出，便于展示
- 不依赖外部服务

### 8. 异常告警实现

**决策**: 使用 Spring Boot 自带的 Actuator 健康检查 + 自定义告警服务

**理由**:
- Actuator 提供基础健康检查能力
- 自定义告警服务通过邮件/钉钉/企业微信发送告警
- 避免引入复杂的告警系统

### 9. API 权限控制

**决策**: 使用 Spring Security + 自定义注解实现基础权限控制

**理由**:
- Spring Security 成熟稳定
- 自定义 `@RequireRole` 注解简洁易用
- 满足当前需求，避免过度设计

**实现方式**:
```java
@RequireRole("RULE_ADMIN")
public class RuleController { ... }
```

### 10. 架构设计：DDD + Spring Cloud

**决策**: 采用领域驱动设计（DDD）+ Spring Cloud Alibaba + Maven 多模块单体架构

**理由**:
- **DDD 分层**: 领域层独立，业务逻辑清晰，易于维护和扩展
- **多限界上下文**: 按业务领域划分上下文，降低耦合，支持独立演进
- **Maven 多模块**: 单体应用多模块部署，简化开发和部署，未来可拆分为微服务
- **Spring Cloud Alibaba**: 使用成熟的 Nacos 生态（注册中心、配置中心、Sentinel 熔断）
- **基础分层 DDD**: User Interface → Application → Domain → Infrastructure，简单实用

**整体架构图**:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                      DMS LiteFlow - DDD + Spring Cloud                          │
│                                                                                  │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                         Spring Cloud Layer                               │   │
│  │                                                                          │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐                   │   │
│  │  │   Nacos      │  │   Nacos      │  │   Sentinel   │                   │   │
│  │  │  Discovery   │  │   Config     │  │   Circuit    │                   │   │
│  │  │  (注册中心)   │  │  (配置中心)   │  │   Breaker    │                   │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘                   │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                     │                                            │
│                                     ▼                                            │
│  ┌──────────────────────────────────────────────────────────────────────────┐   │
│  │                        Application Layer (单应用部署)                       │   │
│  │                                                                          │   │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │   │
│  │  │                   API Gateway / Controller Layer                  │  │   │
│  │  │  (用户接口层 - 处理 HTTP 请求，参数验证，路由)                      │  │   │
│  │  └───────────────────────────┬────────────────────────────────────────┘  │   │
│  │                              │                                             │   │
│  │                              ▼                                             │   │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │   │
│  │  │                    Application Layer (应用服务层)                     │  │   │
│  │  │  - 应用服务 (ApplicationService)                                   │  │   │
│  │  │  - 编排领域模型                                                     │  │   │
│  │  │  - 事务管理                                                         │  │   │
│  │  └───────────────────────────┬────────────────────────────────────────┘  │   │
│  │                              │                                             │   │
│  │                              ▼                                             │   │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │   │
│  │  │                      Domain Layer (领域层)                           │  │   │
│  │  │                                                                      │  │   │
│  │  │  ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐        │  │   │
│  │  │  │ Rule Config     │ │  Flow Exec      │ │  Monitoring     │        │  │   │
│  │  │  │ Bounded Context│ │ Bounded Context│ │ Bounded Context│        │  │   │
│  │  │  └────────┬────────┘ └────────┬────────┘ └────────┬────────┘        │  │   │
│  │  │           │                   │                   │                 │  │   │
│  │  │  ┌────────▼────────┐ ┌────────▼────────┐ ┌────────▼────────┐        │  │   │
│  │  │  │ Tenant          │ │ Testing         │ │ Version         │        │  │   │
│  │  │  │ Bounded Context│ │ Bounded Context│ │ Bounded Context│        │  │   │
│  │  │  └─────────────────┘ └─────────────────┘ └─────────────────┘        │  │   │
│  │  └────────────────────────────────────────────────────────────────────┘  │   │
│  │                              │                                             │   │
│  │                              ▼                                             │   │
│  │  ┌────────────────────────────────────────────────────────────────────┐  │   │
│  │  │                   Infrastructure Layer (基础设施层)                  │  │   │
│  │  │  - Repository 实现 (MyBatis)                                        │  │   │
│  │  │  - 外部服务客户端                                                    │  │   │
│  │  │  - 领域事件发布/订阅                                                  │  │   │
│  │  │  - 缓存实现 (Caffeine + Redis)                                       │  │   │
│  │  └────────────────────────────────────────────────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**Maven 多模块结构**:

```xml
dms-liteflow (父工程)
├── dms-liteflow-api              # API 层（REST 控制器）
│   ├── pom.xml
│   └── src/main/java/com/dms/liteflow/api/
│
├── dms-liteflow-application      # 应用层（应用服务、编排）
│   ├── pom.xml
│   └── src/main/java/com/dms/liteflow/application/
│
├── dms-liteflow-domain           # 领域层（核心业务逻辑）
│   ├── pom.xml
│   └── src/main/java/com/dms/liteflow/domain/
│       ├── rule-config/          # 规则配置上下文
│       ├── flow-exec/            # 流程执行上下文
│       ├── monitoring/           # 监控上下文
│       ├── testing/              # 测试上下文
│       ├── version/              # 版本管理上下文
│       ├── tenant/               # 租户上下文
│       └── shared/               # 共享内核
│
├── dms-liteflow-infrastructure   # 基础设施层（技术实现）
│   ├── pom.xml
│   └── src/main/java/com/dms/liteflow/infrastructure/
│       ├── persistence/          # 持久化（MyBatis）
│       ├── cache/                # 缓存
│       ├── messaging/            # 消息（领域事件）
│       └── external/             # 外部服务
│
├── dms-liteflow-start            # 启动模块
│   ├── pom.xml
│   └── src/main/java/com/dms/liteflow/
│       └── DmsLiteflowApplication.java
│
└── pom.xml                        # 父 POM
```

**DDD 限界上下文划分**:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                        DDD 限界上下文                              │
└─────────────────────────────────────────────────────────────────────────────────┘

1. Rule Config Bounded Context (规则配置上下文)
   职责: 规则组件的定义、管理、版本控制

   聚合根: RuleComponent
   领域服务: ComponentValidationService, ComponentVersionService
   领域事件: ComponentCreatedEvent, ComponentUpdatedEvent, ComponentPublishedEvent

2. Flow Exec Bounded Context (流程执行上下文)
   职责: 流程链定义、编排、执行

   聚合根: FlowChain, FlowSubChain
   领域服务: FlowExecutorService, FlowValidationService, TenantFlowExecutorManager
   领域事件: FlowChainCreatedEvent, FlowChainExecutedEvent, FlowChainFailedEvent

3. Monitoring Bounded Context (监控上下文)
   职责: 执行监控、统计、告警

   聚合根: ExecutionRecord
   领域服务: MonitoringCollectorService, MetricsAggregationService, AlertService
   读模型: ChainMetricsView, ComponentMetricsView

4. Testing Bounded Context (测试上下文)
   职责: 规则和流程的测试、调试

   聚合根: TestCase
   领域服务: ComponentTestingService, FlowTestingService, TestExecutionContextService

5. Version Bounded Context (版本管理上下文)
   职责: 规则和流程的版本管理、对比、回滚

   聚合根: ConfigVersion
   领域服务: VersionManagementService, VersionComparisonService, VersionRollbackService

6. Tenant Bounded Context (租户上下文)
   职责: 租户管理、隔离、配额

   聚合根: Tenant
   领域服务: TenantManagementService, TenantIsolationService, TenantQuotaService
   领域事件: TenantCreatedEvent, TenantSuspendedEvent, TenantQuotaExceededEvent

7. Shared Kernel (共享内核)
   跨上下文共享的概念

   值对象: TenantId, ComponentId, ChainId, Version, Status
   领域事件: DomainEvent (基类), DomainEventPublisher
```

**DDD 分层详细包结构**:

以 **Rule Config Bounded Context** 为例：

```
dms-liteflow-domain/rule-config/
├── aggregate/
│   └── RuleComponent.java              # 聚合根
├── entity/
│   └── ComponentContent.java           # 实体
├── valueobject/
│   ├── ComponentId.java                # 值对象：组件ID
│   ├── ComponentType.java              # 值对象：组件类型
│   ├── ComponentStatus.java            # 值对象：组件状态
│   └── TenantId.java                   # 值对象：租户ID
├── service/
│   ├── ComponentValidationService.java # 领域服务
│   └── ComponentVersionService.java    # 领域服务
├── event/
│   ├── ComponentCreatedEvent.java      # 领域事件
│   ├── ComponentUpdatedEvent.java      # 领域事件
│   └── ComponentPublishedEvent.java    # 领域事件
├── repository/
│   └── RuleComponentRepository.java    # 仓储接口（领域层定义）
└── factory/
    └── RuleComponentFactory.java       # 工厂
```

**技术栈选型**:

```xml
<!-- Spring Cloud Alibaba 依赖 -->
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>
</dependency>
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-sentinel</artifactId>
</dependency>

<!-- MyBatis -->
<dependency>
    <groupId>org.mybatis.spring.boot</groupId>
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>

<!-- 其他依赖保持不变 -->
```

### 11. 多租户架构设计

**决策**: 采用多 FlowExecutor + LRU 缓存方案实现租户完全隔离

**理由**:
- 符合"租户完全隔离"的需求，每个租户有独立的规则集和流程集
- 性能优异：规则预加载，执行时无需查询数据库
- 适配中等规模（10-100 个租户），内存占用可控（每个 Executor 约 10-50MB）
- LiteFlow 原生支持多 Executor，实现简单可靠
- 通过 LRU 缓存管理 Executor 实例，自动卸载冷门租户

**租户隔离架构**:

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                          Multi-Tenant Architecture                              │
│                                                                                  │
│   User Request                                                                    │
│       │                                                                           │
│       ▼                                                                           │
│   ┌──────────────┐                                                               │
│   │   Spring     │                                                               │
│   │  Security    │  获取认证用户，提取 tenantId                                   │
│   └──────┬───────┘                                                               │
│          │                                                                       │
│          ▼                                                                       │
│   ┌───────────────────────────────────────────────────────────────┐             │
│   │              Tenant Context Management                        │             │
│   │  ┌────────────────┐         ┌──────────────────────────────┐ │             │
│   │  │  ThreadLocal   │         │   TenantInterceptor          │ │             │
│   │  │  TenantContext │         │   - Auto set tenant_id       │ │             │
│   │  └────────────────┘         │   - Validate tenant access   │ │             │
│   │                             └──────────────────────────────┘ │             │
│   └────────────────────────────┬──────────────────────────────────┘             │
│                                │                                                  │
│                                ▼                                                  │
│   ┌─────────────────────────────────────────────────────────────────────────┐   │
│   │                    Tenant Executor Manager (LRU Cache)                 │   │
│   │                                                                          │   │
│   │   ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │   │
│   │   │  Executor   │  │  Executor   │  │  Executor   │  │  Executor   │  │   │
│   │   │  Tenant A   │  │  Tenant B   │  │  Tenant C   │  │  ...        │  │   │
│   │   │  (Active)   │  │  (Active)   │  │  (Idle)     │  │             │  │   │
│   │   └──────┬──────┘  └──────┬──────┘  └──────┬──────┘  └─────────────┘  │   │
│   │          │                │                │                          │   │
│   │          ▼                ▼                ▼                          │   │
│   │   ┌─────────────────────────────────────────────────────────────┐    │   │
│   │   │         Per-Tenant Configuration Cache                     │    │   │
│   │   │  ┌──────────┐  ┌──────────┐  ┌──────────┐                 │    │   │
│   │   │  │  Chains  │  │Component │  │SubChain  │                 │    │   │
│   │   │  │   A      │  │   Set A  │  │   Set A   │                 │    │   │
│   │   │  └──────────┘  └──────────┘  └──────────┘                 │    │   │
│   │   └─────────────────────────────────────────────────────────────┘    │   │
│   └────────────────────────────┬───────────────────────────────────────┘   │
│                                │                                             │
│                                ▼                                             │
│   ┌─────────────────────────────────────────────────────────────────────┐   │
│   │                     Database (Tenant Isolation)                     │   │
│   │                                                                      │   │
│   │   rule_component          flow_chain          flow_sub_chain        │   │
│   │   ┌──────────────┐       ┌──────────────┐    ┌──────────────┐      │   │
│   │   │ id           │       │ id           │    │ id           │      │   │
│   │   │ tenant_id    │◄─────││ tenant_id    │    │ tenant_id    │      │   │
│   │   │ component_id │       │ chain_name   │    │ sub_chain_.. │      │   │
│   │   └──────────────┘       └──────────────┘    └──────────────┘      │   │
│   │                                                                      │   │
│   │   config_version         config_test_case                           │   │
│   │   ┌──────────────┐       ┌──────────────┐                           │   │
│   │   │ id           │       │ id           │                           │   │
│   │   │ tenant_id    │       │ tenant_id    │                           │   │
│   │   │ version      │       │ test_data    │                           │   │
│   │   └──────────────┘       └──────────────┘                           │   │
│   └─────────────────────────────────────────────────────────────────────┘   │
│                                                                                  │
└─────────────────────────────────────────────────────────────────────────────────┘
```

**数据库表设计变更**:

1. **所有业务表添加 tenant_id 字段**:
   - `rule_component` - 添加 `tenant_id BIGINT` 和索引 `idx_tenant_component`
   - `flow_chain` - 添加 `tenant_id BIGINT` 和唯一索引 `uk_tenant_chain`
   - `flow_sub_chain` - 添加 `tenant_id BIGINT` 和索引 `idx_tenant_subchain`
   - `config_version` - 添加 `tenant_id BIGINT` 和索引 `idx_tenant_version`
   - `config_test_case` - 添加 `tenant_id BIGINT` 和索引 `idx_tenant_test`

2. **新增租户管理表**:
   ```sql
   CREATE TABLE tenant_info (
       id BIGINT PRIMARY KEY AUTO_INCREMENT,
       tenant_code VARCHAR(50) UNIQUE NOT NULL COMMENT '租户编码',
       tenant_name VARCHAR(100) NOT NULL COMMENT '租户名称',
       status VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态: ACTIVE/SUSPENDED/DELETED',
       max_chains INT DEFAULT 100 COMMENT '最大流程链数量',
       max_components INT DEFAULT 500 COMMENT '最大组件数量',
       executor_cached TINYINT DEFAULT 0 COMMENT 'Executor是否已缓存',
       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
       deleted_at TIMESTAMP NULL,
       INDEX idx_tenant_code(tenant_code),
       INDEX idx_status(status)
   ) COMMENT '租户信息表';
   ```

**租户上下文传递**:

1. **TenantContext (ThreadLocal)**:
   ```java
   public class TenantContext {
       private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();

       public static void setTenantId(Long tenantId) {
           TENANT_ID.set(tenantId);
       }

       public static Long getTenantId() {
           return TENANT_ID.get();
       }

       public static void clear() {
           TENANT_ID.remove();
       }
   }
   ```

2. **TenantInterceptor**:
   ```java
   @Component
   public class TenantInterceptor implements HandlerInterceptor {
       @Override
       public boolean preHandle(HttpServletRequest request,
                               HttpServletResponse response,
                               Object handler) {
           // 从认证用户中提取租户ID
           Authentication auth = SecurityContextHolder.getContext().getAuthentication();
           if (auth != null && auth.getPrincipal() instanceof UserDetails) {
               Long tenantId = ((CustomUserDetails) auth.getPrincipal()).getTenantId();
               TenantContext.setTenantId(tenantId);
           }
           return true;
       }

       @Override
       public void afterCompletion(HttpServletRequest request,
                                  HttpServletResponse response,
                                  Object handler,
                                  Exception ex) {
           TenantContext.clear(); // 清理 ThreadLocal
       }
   }
   ```

**多 Executor 管理**:

```java
@Component
public class TenantExecutorManager {
    private final Map<Long, FlowExecutor> executorCache = new ConcurrentHashMap<>();
    private final int MAX_EXECUTORS = 50; // LRU 最大缓存数

    @Autowired
    private FlowConfigLoader flowConfigLoader;

    public FlowExecutor getExecutor(Long tenantId) {
        return executorCache.computeIfAbsent(tenantId, this::createExecutor);
    }

    private FlowExecutor createExecutor(Long tenantId) {
        // 如果超过最大缓存数，移除最久未使用的 Executor
        if (executorCache.size() >= MAX_EXECUTORS) {
            removeOldestExecutor();
        }

        // 创建租户专用的 FlowExecutor
        FlowExecutor executor = FlowExecutor.loadInstance(
            TenantFlowConfig.class,
            new TenantFlowConfig(tenantId, flowConfigLoader)
        );

        // 预加载该租户的所有规则和流程
        executor.reloadRule();

        return executor;
    }

    public void evictExecutor(Long tenantId) {
        FlowExecutor executor = executorCache.remove(tenantId);
        if (executor != null) {
            executor.close(); // 释放资源
        }
    }

    public void evictExecutorByVersion(Long tenantId, String version) {
        // 配置更新时，使该租户的 Executor 失效
        evictExecutor(tenantId);
    }
}
```

**自动租户过滤**:

1. **Repository 层手动添加租户过滤**:
   ```java
   @Repository
   public class FlowChainRepository {
       @Autowired
       private FlowChainMapper chainMapper;

       public List<FlowChain> findByTenantId(Long tenantId) {
           return chainMapper.selectByTenantId(tenantId);
       }

       public FlowChain findById(Long id) {
           Long tenantId = TenantContext.getTenantId();
           return chainMapper.selectByIdAndTenantId(id, tenantId);
       }

       public int update(FlowChain chain) {
           chain.setTenantId(TenantContext.getTenantId());
           return chainMapper.update(chain);
       }

       public int deleteById(Long id) {
           Long tenantId = TenantContext.getTenantId();
           return chainMapper.deleteByIdAndTenantId(id, tenantId);
       }
   }
   ```

2. **Mapper XML 自动添加租户过滤**:
   ```xml
   <!-- FlowChainMapper.xml -->
   <mapper namespace="com.xxx.liteflow.repository.FlowChainMapper">
       <select id="selectByTenantId" resultType="FlowChain">
           SELECT * FROM flow_chain
           WHERE tenant_id = #{tenantId}
           AND deleted_at IS NULL
       </select>

       <select id="selectByIdAndTenantId" resultType="FlowChain">
           SELECT * FROM flow_chain
           WHERE id = #{id}
           AND tenant_id = #{tenantId}
           AND deleted_at IS NULL
       </select>

       <update id="updateByIdAndTenantId">
           UPDATE flow_chain SET
               chain_name = #{chainName},
               chain_code = #{chainCode},
               description = #{description},
               updated_at = NOW()
           WHERE id = #{id}
           AND tenant_id = #{tenantId}
       </update>

       <delete id="deleteByIdAndTenantId">
           UPDATE flow_chain SET deleted_at = NOW()
           WHERE id = #{id}
           AND tenant_id = #{tenantId}
       </delete>
   </mapper>
   ```

3. **MyBatis Interceptor 拦截器（可选，用于统一处理）**:
   ```java
   @Intercepts({
       @Signature(
           type = Executor.class,
           method = "query",
           args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}
       ),
       @Signature(
           type = Executor.class,
           method = "update",
           args = {MappedStatement.class, Object.class}
       )
   })
   @Component
   public class TenantInterceptor implements Interceptor {
       private static final Set<String> IGNORE_TABLES = new HashSet<>(
           Arrays.asList("tenant_info")
       );

       @Override
       public Object intercept(Invocation invocation) throws Throwable {
           Object[] args = invocation.getArgs();
           MappedStatement ms = (MappedStatement) args[0];
           Object parameter = args[1];

           // 获取当前租户ID
           Long tenantId = TenantContext.getTenantId();
           if (tenantId == null) {
               return invocation.proceed();
           }

           // 获取 SQL
           BoundSql boundSql = ms.getBoundSql(parameter);
           String sql = boundSql.getSql();

           // 解析表名
           String tableName = extractTableName(sql);
           if (IGNORE_TABLES.contains(tableName)) {
               return invocation.proceed();
           }

           // 修改 SQL，添加租户过滤条件
           String modifiedSql = addTenantCondition(sql, tenantId);

           // 创建新的 BoundSql
           BoundSql newBoundSql = new BoundSql(ms.getConfiguration(), modifiedSql,
               boundSql.getParameterMappings(), boundSql.getParameterObject());

           // 复制额外参数
           for (ParameterMapping mapping : boundSql.getParameterMappings()) {
               String prop = mapping.getProperty();
               if (boundSql.hasAdditionalParameter(prop)) {
                   newBoundSql.setAdditionalParameter(prop, boundSql.getAdditionalParameter(prop));
               }
           }

           // 创建新的 MappedStatement
           MappedStatement newMs = copyMappedStatement(ms, new BoundSqlSource(newBoundSql));

           // 替换参数
           args[0] = newMs;

           return invocation.proceed();
       }

       private String extractTableName(String sql) {
           // 简单的表名提取逻辑
           String upperSql = sql.toUpperCase().trim();
           if (upperSql.startsWith("SELECT")) {
               Pattern pattern = Pattern.compile("FROM\\s+(\\w+)", Pattern.CASE_INSENSITIVE);
               Matcher matcher = pattern.matcher(sql);
               if (matcher.find()) {
                   return matcher.group(1);
               }
           } else if (upperSql.startsWith("INSERT") || upperSql.startsWith("UPDATE") ||
                      upperSql.startsWith("DELETE")) {
               Pattern pattern = Pattern.compile("(?:INSERT\\s+INTO|UPDATE|DELETE\\s+FROM)\\s+(\\w+)",
                   Pattern.CASE_INSENSITIVE);
               Matcher matcher = pattern.matcher(sql);
               if (matcher.find()) {
                   return matcher.group(1);
               }
           }
           return "";
       }

       private String addTenantCondition(String sql, Long tenantId) {
           // 添加 WHERE 条件
           String upperSql = sql.toUpperCase();

           // 如果已有 WHERE 子句，添加 AND
           if (upperSql.contains(" WHERE ")) {
               return sql.replaceAll("(?i)\\s+WHERE\\s+", " WHERE tenant_id = " + tenantId + " AND ");
           }

           // 否则添加 WHERE
           // 在 FROM/JOIN 之后添加
           Pattern pattern = Pattern.compile("(\\s+FROM\\s+\\w+|\\s+JOIN\\s+\\w+)", Pattern.CASE_INSENSITIVE);
           Matcher matcher = pattern.matcher(sql);
           if (matcher.find()) {
               int lastEnd = 0;
               StringBuilder result = new StringBuilder();
               while (matcher.find()) {
                   result.append(sql.substring(lastEnd, matcher.end()));
                   result.append(" WHERE tenant_id = ").append(tenantId);
                   lastEnd = matcher.end();
               }
               if (lastEnd < sql.length()) {
                   result.append(sql.substring(lastEnd));
               }
               return result.toString();
           }

           return sql;
       }

       private MappedStatement copyMappedStatement(MappedStatement ms, SqlSource newSqlSource) {
           MappedStatement.Builder builder = new MappedStatement.Builder(
               ms.getConfiguration(),
               ms.getId(),
               newSqlSource,
               ms.getSqlCommandType()
           );

           builder.resource(ms.getResource());
           builder.fetchSize(ms.getFetchSize());
           builder.statementType(ms.getStatementType());
           builder.keyGenerator(ms.getKeyGenerator());
           builder.timeout(ms.getTimeout());
           builder.parameterMap(ms.getParameterMap());
           builder.resultMaps(ms.getResultMaps());
           builder.resultSetType(ms.getResultSetType());
           builder.cache(ms.getCache());
           builder.flushCacheRequired(ms.isFlushCacheRequired());
           builder.useCache(ms.isUseCache());

           return builder.build();
       }

       @Override
       public Object plugin(Object target) {
           return Plugin.wrap(target, this);
       }

       @Override
       public void setProperties(Properties properties) {
       }

       static class BoundSqlSource implements SqlSource {
           private final BoundSql boundSql;

           public BoundSqlSource(BoundSql boundSql) {
               this.boundSql = boundSql;
           }

           @Override
           public BoundSql getBoundSql(Object parameterObject) {
               return boundSql;
           }

           @Override
           public Class<?> getParameterType() {
               return boundSql.getParameterObject().getClass();
           }
       }
   }
   ```

**API 层租户隔离**:

所有 API 自动添加租户隔离，无需手动传递 tenantId：

```
GET    /api/chains                    → 查询当前租户的流程链
POST   /api/chains                    → 创建当前租户的流程链
PUT    /api/chains/{id}               → 更新当前租户的流程链（自动验证所有权）
DELETE /api/chains/{id}               → 删除当前租户的流程链（自动验证所有权）

GET    /api/components                → 查询当前租户的组件
POST   /api/components                → 创建当前租户的组件

POST   /api/execute/{chainName}       → 执行当前租户的流程链
GET    /api/monitoring/chains         → 查询当前租户的监控数据
```

**缓存策略**:

```
┌─────────────────────────────────────────────────────────┐
│              三层缓存架构                               │
└─────────────────────────────────────────────────────────┘

   L1: FlowExecutor 实例缓存
   ┌────────────────────────────────────────────────┐
   │ Key: tenant:{tenantId}                         │
   │ Value: FlowExecutor 实例                       │
   │ Policy: LRU, 最大 50 个                        │
   │ Evict: 配置更新时主动失效                      │
   └────────────────────────────────────────────────┘

   L2: 规则配置缓存
   ┌────────────────────────────────────────────────┐
   │ Key: chain:{tenantId}:{chainName}             │
   │ Value: Chain EL Expression                    │
   │ Policy: Caffeine, TTL 60min                   │
   │ Evict: 配置更新时主动失效                      │
   └────────────────────────────────────────────────┘

   L3: 组件元数据缓存
   ┌────────────────────────────────────────────────┐
   │ Key: component:{tenantId}:{componentId}       │
   │ Value: Component Metadata                     │
   │ Policy: Caffeine, TTL 30min                   │
   │ Evict: 组件更新时主动失效                      │
   └────────────────────────────────────────────────┘
```

**租户监控隔离**:

- 执行监控数据按租户自动聚合
- 监控查询 API 自动过滤当前租户数据
- 支持跨租户监控（仅管理员）
- 租户级监控指标：执行次数、成功率、平均耗时

**租户管理功能**:

1. **租户 CRUD**:
   - 创建/编辑/删除租户
   - 租户启用/禁用
   - 租户配额管理（最大流程数、最大组件数）

2. **租户数据管理**:
   - 租户规则导出/导入
   - 租户间规则复制
   - 租户数据清理

3. **租户监控**:
   - 租户活跃度监控
   - 租户资源使用统计
   - 租户异常告警

**租户规模适配**:

| 租户数量 | Executor 策略          | 内存估算     | 适用场景         |
|---------|----------------------|-------------|----------------|
| 1-10    | 全部缓存              | 10-500MB    | 小规模部署       |
| 10-100  | LRU 缓存 50 个         | 50-500MB    | 中等规模（推荐）  |
| 100-1000| LRU 缓存 + 动态加载    | 100-500MB   | 大规模部署       |

### 12. 流程事务管理

**决策**: 使用 Spring TransactionManager 实现流程级事务，通过注解方式配置组件的事务需求

**理由**:
- 业务流程通常涉及多个数据库操作，需要事务保证数据一致性
- 不是所有组件都需要事务（如查询、外部调用等）
- 灵活配置可以满足不同业务场景的事务需求
- LiteFlow 与 Spring 事务机制天然兼容

**事务策略**:

1. **流程级事务**: 整个流程链在一个事务中执行
   - 适用场景：流程中所有组件都是写操作且强一致性要求
   - 优点：简单，全流程原子性
   - 缺点：长事务锁定资源，影响并发

2. **组件级事务**: 每个组件独立管理事务
   - 适用场景：组件之间数据一致性要求不高
   - 优点：组件独立，失败不影响其他组件
   - 缺点：无法保证跨组件一致性

3. **混合事务策略**: 灵活组合流程级和组件级事务
   - 适用场景：复杂业务流程
   - 优点：平衡一致性和性能
   - 缺点：配置复杂度较高

**实现方式**:

1. **组件事务注解**:
```java
@Component("createOrder")
@LiteflowTransaction(TransactionalType.REQUIRED)  // 需要事务
public class CreateOrderComponent extends NodeComponent {
    @Override
    public void process() {
        // 组件逻辑，自动加入事务
    }
}

@Component("queryOrder")
@LiteflowTransaction(TransactionalType.NOT_SUPPORTED)  // 不需要事务
public class QueryOrderComponent extends NodeComponent {
    @Override
    public void process() {
        // 查询操作，不使用事务
    }
}

@Component("sendNotify")
@LiteflowTransaction(TransactionalType.NOT_SUPPORTED)  // 不需要事务
public class SendNotifyComponent extends NodeComponent {
    @Override
    public void process() {
        // 外部调用（如邮件、短信），不使用事务
    }
}
```

2. **流程级事务配置**:
```xml
<!-- XML 配置方式 -->
<chain name="orderProcessChain" transactional="true">
    THEN(
        validateOrder,
        checkStock,
        createOrder,
        sendNotify
    )
</chain>
```

或者通过 EL 表达式包装:
```xml
<chain name="orderProcessChain">
    transaction(
        THEN(
            validateOrder,
            checkStock,
            createOrder,
            sendNotify
        )
    )
</chain>
```

3. **数据库配置**:
```yaml
liteflow:
  transaction:
    enabled: true
    manager: springTransactionManager  # 使用 Spring 事务管理器
    default-propagation: REQUIRED   # 默认事务传播行为
    default-timeout: 30          # 默认事务超时时间（秒）
```

4. **事务传播类型**:
```java
public enum TransactionalType {
    REQUIRED,      // 需要事务（默认）
    REQUIRES_NEW,  // 总是新建事务
    NOT_SUPPORTED,  // 不支持事务
    MANDATORY,     // 必须在已有事务中
    NEVER,         // 从不在事务中
    SUPPORTS       // 如果有事务则加入
}
```

5. **事务配置表扩展**:

在 `flow_chain` 表中添加事务配置字段:
```sql
ALTER TABLE flow_chain ADD COLUMN transactional TINYINT DEFAULT 0 COMMENT '是否启用流程级事务: 0-否 1-是';
ALTER TABLE flow_chain ADD COLUMN transaction_timeout INT DEFAULT 30 COMMENT '事务超时时间(秒)';
ALTER TABLE flow_chain ADD COLUMN transaction_propagation VARCHAR(20) DEFAULT 'REQUIRED' COMMENT '事务传播行为';
```

在 `rule_component` 表中添加组件事务配置字段:
```sql
ALTER TABLE rule_component ADD COLUMN transactional_type VARCHAR(20) DEFAULT 'REQUIRED' COMMENT '事务类型';
```

**组件事务需求判断规则**:

| 组件类型 | 事务需求 | 推荐配置 | 示例 |
|---------|---------|----------|------|
| **数据插入/更新** | 需要 | REQUIRED/REQUIRES_NEW | 创建订单、更新库存 |
| **数据删除** | 需要 | REQUIRED | 删除用户、取消订单 |
| **数据查询** | 不需要 | NOT_SUPPORTED/SUPPORTS | 查询订单、获取配置 |
| **计算处理** | 不需要 | NOT_SUPPORTED | 金额计算、条件判断 |
| **外部调用** | 不需要 | NOT_SUPPORTED | 发送邮件、调用第三方API |
| **日志记录** | 不需要 | NOT_SUPPORTED | 写日志、记录审计 |
| **缓存操作** | 不需要 | NOT_SUPPORTED | 更新缓存、清理缓存 |

**事务回滚策略**:

1. **自动回滚**: 组件抛出 RuntimeException 时自动回滚
```java
@Component("createOrder")
@LiteflowTransaction(rollbackFor = {BusinessException.class})
public class CreateOrderComponent extends NodeComponent {
    @Override
    public void process() throws BusinessException {
        // 业务逻辑，抛出异常时回滚
    }
}
```

2. **流程级回滚**: 流程中任一组件失败，回滚整个流程
```java
@Component("orderProcessChain")
@FlowChainTransactional(rollbackOnFailure = true)
public class OrderProcessChain extends NodeComponent {
    @Override
    public void process() {
        // 流程级事务
    }
}
```

3. **补偿事务（Saga 模式）**: 用于分布式场景
```xml
<chain name="orderChain">
    THEN(
        createOrder,
        WHEN(
            checkStock,
            deductBalance
        ),
        IF(
            paymentSuccess,
            sendNotify,
            compensation(
                createOrder,
                checkStock,
                deductBalance
            )
        )
    )
</chain>
```

**事务监控**:

1. **事务执行记录**: 记录事务开始、提交、回滚时间和状态
2. **事务超时监控**: 监控长事务，超过阈值告警
3. **死锁检测**: 记录死锁信息，便于排查
4. **事务统计**: 统计事务成功率、回滚率

**示例场景**:

```xml
<!-- 订单处理流程 - 混合事务配置 -->
<chain name="orderProcessChain" transactional="true">
    THEN(
        validateOrder,        <!-- REQUIRED: 验证订单（查询，可选事务） -->
        WHEN(
            checkStock,          <!-- REQUIRED: 扣减库存（需要事务） -->
            calculateAmount      <!-- NOT_SUPPORTED: 金额计算（不需要事务） -->
        ),
        IF(
            isVIPUser,
            THEN(
                vipApproval      <!-- REQUIRED: VIP审批（可能需要事务） -->
            ),
            THEN(
                normalApproval,  <!-- REQUIRED: 普通审批 -->
                createOrder      <!-- REQUIRES_NEW: 创建订单（新建事务） -->
            )
        ),
        sendNotify           <!-- NOT_SUPPORTED: 发送通知（外部调用，不需要事务） -->
    )
</chain>
```

```java
@Component("validateOrder")
@LiteflowTransaction(TransactionalType.SUPPORTS)  // 支持事务，但不强制
public class ValidateOrderComponent extends NodeComponent {
    @Override
    public void process() {
        // 查询验证，如果在事务中则加入
    }
}

@Component("checkStock")
@LiteflowTransaction(TransactionalType.REQUIRED)  // 必须有事务
public class CheckStockComponent extends NodeComponent {
    @Override
    public void process() {
        // 扣减库存，必须在事务中
    }
}

@Component("createOrder")
@LiteflowTransaction(TransactionalType.REQUIRES_NEW)  // 独立新事务
public class CreateOrderComponent extends NodeComponent {
    @Override
    public void process() {
        // 创建订单，使用独立事务
    }
}

@Component("sendNotify")
@LiteflowTransaction(TransactionalType.NOT_SUPPORTED)  // 不使用事务
public class SendNotifyComponent extends NodeComponent {
    @Override
    public void process() {
        // 发送通知（邮件/短信），不使用事务
    }
}
```

### 13. 规则与流程组件异常处理

**决策**: 全局异常处理器 + 组件级异常配置

**理由**:
- 全局异常处理器统一处理异常
- 组件级异常配置支持灵活控制（继续执行/中止流程）
- 便于日志记录和监控

**实现方式**:
```java
@Component("exceptionHandler")
public class GlobalExceptionHandler extends NodeComponent {
    @Override
    public void process() {
        // 全局异常处理
    }
}
```

## Risks / Trade-offs

### 风险 1: 数据库动态规则加载性能

**描述**: 频繁从数据库加载规则可能影响性能

**缓解措施**:
- 实现本地缓存（Caffeine），缓存已加载的规则
- 配置缓存刷新策略（时间/事件触发）
- 数据库查询添加索引，优化查询性能

### 风险 2: 监控数据量过大

**描述**: 规则执行频繁时，监控数据可能快速增长

**缓解措施**:
- 实现数据聚合策略（原始数据 → 小时级 → 日级）
- 定时清理过期数据
- 可配置采样率，减少数据量

### 风险 3: 规则版本管理复杂性

**描述**: 版本过多可能导致管理复杂

**缓解措施**:
- 限制版本数量上限（如最多保留 50 个版本）
- 提供版本归档功能
- 定期清理旧版本

### 风险 4: 测试环境隔离不彻底

**描述**: 测试可能意外修改生产数据

**缓解措施**:
- 使用 Mock 数据源，确保测试不连接真实数据库
- 测试执行后强制清理上下文
- 添加明确的测试标识，在日志中记录

### 风险 5: 多租户内存占用

**描述**: 多 FlowExecutor 方案会为每个活跃租户维护独立的 Executor 实例，租户数量增加时内存占用可能较高

**缓解措施**:
- 使用 LRU 缓存策略，最多缓存 50 个活跃租户的 Executor
- 实现 Executor 按需加载和自动卸载机制
- 冷门租户的 Executor 自动释放，需要时重新加载
- 监控 Executor 内存使用，设置内存使用告警阈值
- 对于超大规模租户（100+），考虑降级为单 Executor + 动态过滤方案

### 风险 6: 租户数据隔离泄露

**描述**: 租户隔离机制实现不当可能导致数据泄露，用户访问到其他租户的规则或数据

**缓解措施**:
- 使用 MyBatis Interceptor 自动添加租户过滤条件到 SQL
- 所有 SQL 查询强制包含 tenant_id 过滤，禁止手动绕过
- API 层添加所有权验证，更新/删除操作校验资源所属租户
- 定期进行租户隔离审计测试
- 监控异常的跨租户访问尝试并告警
- 敏感操作（删除、批量导出）添加二次验证

### 风险 7: 租户配置热更新冲突

**描述**: 多租户环境下，不同租户同时更新配置可能导致缓存不一致或 Executor 状态混乱

**缓解措施**:
- 每个租户的 Executor 独立管理，更新互不影响
- 配置更新时使用分布式锁（Redis Lock）确保同一租户的更新串行化
- 更新完成后使该租户的缓存失效，其他租户不受影响
- 实现配置版本号机制，避免并发更新覆盖
- 监控配置更新频率，异常频繁时告警

### 权衡 1: XML vs 数据库配置

**权衡点**: XML 配置简单但不够灵活，数据库配置灵活但复杂

**决策**: 混合模式，核心流程用 XML，可变规则用数据库

### 权衡 2: 监控实时性 vs 存储成本

**权衡点**: 实时监控需要更多存储，减少存储影响分析

**决策**: 原始数据保留 7 天，使用聚合数据支撑长期分析

### 权衡 3: 测试完整性 vs 执行速度

**权衡点**: 完整的隔离测试执行慢，简化测试速度快但不彻底

**决策**: 使用 Mock 数据源实现隔离，平衡速度和完整性

### 权衡 4: 多 Executor 内存占用 vs 执行性能

**权衡点**: 多 FlowExecutor 方案占用更多内存，但执行性能更好；单 Executor 节省内存但每次执行需要查询数据库

**决策**: 采用多 Executor + LRU 缓存方案，平衡内存和性能。对于 10-100 个中等规模租户，每个 Executor 约 10-50MB，总内存占用 100-500MB 可接受。通过 LRU 策略自动卸载冷门租户，控制内存增长。

### 权衡 5: 租户完全隔离 vs 规则复用

**权衡点**: 完全隔离保证安全但无法跨租户复用规则；共享规则可以提高复用率但增加管理复杂度

**决策**: 采用完全隔离方案，每个租户有独立的规则集。虽然无法自动复用，但通过租户规则导出/导入功能支持手动复制规则，平衡了安全性和灵活性。

## Migration Plan

### 阶段 0: DDD 架构搭建（Week 1）

**Maven 多模块项目搭建**:
1. 创建父 POM，定义依赖管理和 Spring Cloud 版本
2. 创建 dms-liteflow-api 模块（API 层）
3. 创建 dms-liteflow-application 模块（应用层）
4. 创建 dms-liteflow-domain 模块（领域层）
5. 创建 dms-liteflow-infrastructure 模块（基础设施层）
6. 创建 dms-liteflow-start 模块（启动模块）
7. 配置 Spring Cloud Alibaba（Nacos、Sentinel）
8. 配置 MyBatis 集成

**DDD 分层结构搭建**:
1. 创建 7 个限界上下文包结构（rule-config、flow-exec、monitoring、testing、version、tenant、shared）
2. 为每个上下文创建基础分层（aggregate、entity、valueobject、service、event、repository、factory）
3. 创建共享内核（TenantId、ComponentId、ChainId 等值对象）
4. 创建领域事件基础设施（DomainEvent、DomainEventPublisher）
5. 创建应用服务基础框架（ApplicationService 基类）
6. 创建 API 控制器基础框架（RestController 基类）
7. 创建基础设施层基础框架（Repository 实现基类）

**数据库初始化**:
1. 创建 7 张数据库表（rule_component、flow_chain、flow_sub_chain、config_version、config_test_case、execution_monitoring、tenant_info）
2. 添加所有字段和索引
3. 创建数据库初始化脚本（schema.sql）
4. 插入默认租户数据
5. 验证 MyBatis 连接和查询

### 阶段 1: 基础集成（Week 2-3）

1. 引入 LiteFlow 依赖和配置
2. 创建基础的组件开发框架（NodeComponent 基类）
3. 创建流程编排框架（EL 表达式解析器）
4. 实现 XML 配置加载（流程链和规则组件）
5. 实现 Rule Config Bounded Context 基础聚合（RuleComponent）
6. 实现 Flow Exec Bounded Context 基础聚合（FlowChain）

### 阶段 2: 动态配置加载（Week 4）

1. 实现数据库动态流程链加载
2. 实现数据库动态规则组件加载
3. 实现配置验证功能（EL 表达式语法检查、组件存在性检查）
4. 实现流程链热更新机制
5. 添加本地缓存（Caffeine）
6. 实现子流程配置和加载
7. 实现 FlowSubChain 聚合和仓储

### 阶段 3: 版本管理（Week 5）

1. 实现流程链版本存储
2. 实现规则组件版本存储
3. 实现子流程版本存储
4. 实现版本对比功能（EL 表达式 diff）
5. 实现版本回滚功能
6. 实现版本状态管理（草稿、已发布、已废弃）

### 阶段 5: 测试调试（Week 6）

1. 实现规则组件测试功能（单元测试）
2. 实现流程链测试功能（集成测试）
3. 实现测试环境隔离
4. 实现执行路径可视化（流程执行图）
5. 实现断点调试功能
6. 实现测试用例管理（组件测试用例、流程测试用例）
7. 实现批量测试和测试报告

### 阶段 6: 监控告警（Week 7）

1. 实现流程链监控数据采集
2. 实现规则组件监控数据采集
3. 实现链路追踪功能
4. 实现监控仪表盘（流程监控、组件监控、链路追踪）
5. 实现数据聚合和清理
6. 实现异常告警（流程失败告警、组件异常告警）

### 阶段 7: 管理 API（Week 8）

1. 实现规则组件 CRUD API
2. 实现流程链 CRUD API
3. 实现子流程管理 API
4. 实现版本管理 API
5. 实现配置验证 API（EL 表达式验证、组件引用检查）
6. 实现测试 API（组件测试、流程测试、批量测试）
7. 实现监控 API（监控数据查询、链路追踪查询）
8. 实现流程执行 API
9. 添加 API 权限控制

### 阶段 8: 多租户支持（Week 9-10）

**数据库层改造**:
1. 创建租户管理表（tenant_info）
2. 为所有业务表添加 tenant_id 字段
3. 添加租户相关索引和唯一约束
4. 数据迁移脚本（为现有数据添加默认租户）
5. 实现 MyBatis Interceptor 租户隔离拦截器（自动添加 tenant_id 条件）

**租户上下文管理**:
1. 实现 TenantContext（ThreadLocal 租户上下文）
2. 实现 TenantInterceptor（从用户认证中提取租户ID）
3. 实现 TenantExecutorManager（管理多 FlowExecutor）
4. 实现租户 Executor 的创建、缓存和卸载机制
5. 实现 LRU 缓存策略（最多缓存 50 个活跃租户）

**租户隔离机制**:
1. 修改所有 Repository 添加租户过滤
2. 修改所有 Service 添加租户隔离逻辑
3. 实现 API 层租户所有权验证
4. 修改 FlowExecutor 支持租户隔离执行
5. 实现租户配置的独立加载和缓存

**租户管理功能**:
1. 实现租户 CRUD API
2. 实现租户启用/禁用功能
3. 实现租户配额管理（最大流程数、最大组件数）
4. 实现租户规则导出/导入功能
5. 实现租户间规则复制功能
6. 实现租户数据清理功能

**租户监控**:
1. 监控数据按租户自动聚合
2. 实现租户活跃度监控
3. 实现租户资源使用统计
4. 实现租户异常告警
5. 支持跨租户监控（仅管理员）

**测试和验证**:
1. 租户隔离功能测试（单元测试、集成测试）
2. 租户数据隔离泄露测试
3. 租户并发更新测试
4. 租户 Executor 内存占用测试
5. 租户性能测试（验证多 Executor 方案性能）
6. 租户配置热更新测试

**文档和培训**:
1. 编写多租户架构设计文档
2. 编写租户管理操作手册
3. 编写租户隔离开发指南
4. 编写租户迁移指南

### 回滚策略

- 每个阶段完成后创建 Git 标签
- 保留数据库备份，支持快速回滚
- 使用功能开关，可随时禁用新功能

## Open Questions

1. **配置中心选择**: 是否需要集成配置中心（如 Nacos、Apollo）用于动态规则和流程分发？当前设计使用数据库，如需配置中心需要额外集成工作。

2. **规则执行审计**: 是否需要记录规则和流程执行的详细日志（包括输入输出、执行时间等）？当前监控仅记录统计信息，如需详细审计需要额外设计。

3. **规则模板化**: 是否需要支持规则和流程模板，允许快速创建相似规则和流程？当前设计不支持，如需要需要设计模板引擎。

4. **多租户支持**: ✅ **已决策** - 采用多 FlowExecutor + LRU 缓存方案，支持 10-100 个租户的完全隔离。每个租户有独立的规则集和流程集，通过用户关联自动获取租户标识，实现透明的租户隔离。详见 Decision 11。

5. **规则权限细粒度控制**: 是否需要对规则和流程进行更细粒度的权限控制（如只能查看/修改特定规则/流程的权限）？当前设计使用角色级别的简单权限。

6. **流程可视化编辑器**: 是否需要提供可视化的流程编排编辑器（拖拽式编排）？当前设计使用文本编辑器编写 EL 表达式，可视化编辑器可以降低使用门槛。

7. **流程并行度控制**: 是否需要对流程的并行执行度进行精细化控制（如限制并发数、超时时间等）？当前设计使用 LiteFlow 默认配置。

8. **流程编排模式扩展**: 是否需要支持更复杂的编排模式（如 WHILE 循环、SWITCH 分支、异步编排等）？当前设计支持基础模式。

9. **流程间依赖管理**: 是否需要支持流程间的依赖关系（如一个流程完成后触发另一个流程）？当前设计不支持流程间触发。
