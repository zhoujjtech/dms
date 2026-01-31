# DMS LiteFlow

基于 DDD + Spring Cloud Alibaba 的 LiteFlow 规则引擎管理系统

## 项目简介

DMS LiteFlow 是一个企业级的规则引擎管理系统，采用领域驱动设计（DDD）架构，集成 LiteFlow 2.12.4 规则引擎，支持多租户、动态配置、版本管理和监控告警等功能。

## 核心特性

### ✅ 已实现功能

- **DDD 四层架构**：API、Application、Domain、Infrastructure 完全分离
- **多租户支持**：租户完全隔离，支持独立的配置和资源
- **动态配置加载**：从数据库动态加载流程链和组件配置
- **配置热更新**：支持配置发布后自动刷新，无需重启
- **配置验证**：EL 表达式语法验证、组件存在性检查
- **版本管理**：配置版本控制、版本回滚、版本对比
- **测试调试**：组件测试、流程链测试、执行路径追踪
- **监控告警**：执行监控、性能统计、失败率告警
- **缓存机制**：基于 Caffeine 的本地缓存，5分钟自动刷新
- **异常处理**：全局异常处理器，统一错误响应格式

## 技术栈

### 核心框架
- **Spring Boot 3.2.0**：应用框架
- **Spring Cloud 2023.0.0**：微服务框架
- **Spring Cloud Alibaba 2022.0.0.0**：阿里云微服务组件
- **LiteFlow 2.12.4**：规则引擎

### 持久化
- **MyBatis 3.0.3**：ORM 框架
- **MySQL**：数据库
- **Caffeine**：本地缓存

### 服务治理
- **Nacos**：服务注册与配置中心
- **Sentinel**：限流熔断（待集成）

## 模块结构

```
dms-liteflow/
├── dms-liteflow-domain/          # 领域层
│   ├── ruleconfig/               # 规则配置上下文
│   ├── flowexec/                 # 流程执行上下文
│   ├── monitoring/               # 监控上下文
│   ├── testing/                  # 测试上下文
│   ├── version/                  # 版本上下文
│   ├── tenant/                   # 租户上下文
│   └── shared/                   # 共享内核
│       ├── kernel/
│       │   ├── valueobject/      # 值对象
│       │   └── validation/       # 验证器
│       └── event/                # 领域事件
├── dms-liteflow-application/     # 应用层
│   ├── component/                # 组件应用服务
│   ├── flowchain/                # 流程链应用服务
│   ├── subchain/                 # 子流程应用服务
│   ├── tenant/                   # 租户应用服务
│   ├── version/                  # 版本应用服务
│   ├── testing/                  # 测试应用服务
│   ├── monitoring/               # 监控应用服务
│   ├── alerting/                 # 告警应用服务
│   └── scheduled/                # 定时任务
├── dms-liteflow-infrastructure/  # 基础设施层
│   ├── persistence/              # 持久化
│   │   ├── entity/               # 实体
│   │   ├── mapper/               # MyBatis Mapper
│   │   └── repository/           # 仓储实现
│   ├── liteflow/                 # LiteFlow 组件
│   ├── scheduled/                # 调度任务
│   ├── interceptor/              # 拦截器
│   └── config/                   # 配置类
├── dms-liteflow-api/             # API 层
│   ├── component/                # 组件控制器
│   ├── flowchain/                # 流程链控制器
│   ├── subchain/                 # 子流程控制器
│   ├── tenant/                   # 租户控制器
│   ├── version/                  # 版本控制器
│   ├── testing/                  # 测试控制器
│   ├── monitoring/               # 监控控制器
│   ├── validation/               # 验证控制器
│   └── exception/                # 异常处理
└── dms-liteflow-start/           # 启动模块
```

## 快速开始

### 环境要求

- JDK 17+
- Maven 3.6+
- MySQL 8.0+
- Nacos 2.2+

### 数据库初始化

```sql
-- 创建数据库
CREATE DATABASE dms_liteflow DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 执行 schema.sql 初始化表结构
USE dms_liteflow;
SOURCE schema.sql;
```

### 配置文件

编辑 `dms-liteflow-start/src/main/resources/application.yml`：

```yaml
spring:
  application:
    name: dms-liteflow
  cloud:
    nacos:
      discovery:
        server-addr: localhost:8848
      config:
        server-addr: localhost:8848
  datasource:
    url: jdbc:mysql://localhost:3306/dms_liteflow
    username: root
    password: your_password

liteflow:
  rule-source: configData
  configData: classpath:flow/
```

### 启动应用

```bash
# 编译项目
mvn clean package -DskipTests

# 启动应用
cd dms-liteflow-start
java -jar target/dms-liteflow-start-1.0.0-SNAPSHOT.jar
```

## API 文档

### 组件管理

- `POST /api/components` - 创建组件
- `GET /api/components` - 查询组件列表
- `POST /api/components/{componentId}/publish` - 发布组件
- `DELETE /api/components/{componentId}` - 删除组件

### 流程链管理

- `POST /api/chains` - 创建流程链
- `GET /api/chains` - 查询流程链列表
- `PUT /api/chains/{chainId}` - 更新流程链
- `POST /api/chains/{chainId}/publish` - 发布流程链
- `DELETE /api/chains/{chainId}` - 删除流程链

### 子流程管理

- `POST /api/subchains` - 创建子流程
- `GET /api/subchains` - 查询子流程列表
- `PUT /api/subchains/{id}` - 更新子流程
- `POST /api/subchains/{id}/publish` - 发布子流程

### 租户管理

- `POST /api/tenants` - 创建租户
- `GET /api/tenants` - 查询租户列表
- `PUT /api/tenants/{id}` - 更新租户
- `POST /api/tenants/{id}/activate` - 激活租户
- `POST /api/tenants/{id}/deactivate` - 停用租户

### 版本管理

- `GET /api/versions` - 查询版本列表
- `POST /api/versions/rollback` - 回滚版本
- `GET /api/versions/compare` - 对比版本

### 测试调试

- `POST /api/testing/components/{componentId}` - 测试组件
- `POST /api/testing/chains/{chainName}` - 测试流程链
- `POST /api/testing/chains/{chainName}/with-path` - 测试并返回执行路径

### 监控查询

- `GET /api/monitoring/executions/{executionId}` - 查询执行记录
- `GET /api/monitoring/chains/{chainId}/stats` - 获取执行统计
- `GET /api/monitoring/chains/{chainId}/success-rate` - 获取成功率

### 配置验证

- `POST /api/validation/chains/{chainId}` - 验证流程链
- `POST /api/validation/components/{componentId}` - 验证组件
- `POST /api/validation/tenant` - 验证租户配置

## 开发指南

### DDD 分层规范

1. **Domain 层**：纯粹的业务逻辑，不依赖任何框架
2. **Application 层**：业务编排，协调领域对象
3. **Infrastructure 层**：技术实现，持久化、缓存等
4. **API 层**：接口定义，处理 HTTP 请求

### 多租户隔离

所有数据表都包含 `tenant_id` 字段，通过 `TenantContext` 自动设置：

```java
// 在拦截器中自动设置
TenantContext.setTenantId(TenantId.of(tenantId));

// 在服务中使用
TenantId currentTenantId = TenantContext.getTenantId();
```

### 配置热更新流程

1. 发布配置：调用 `publishComponent()` 或 `publishChain()`
2. 清除缓存：触发 `@CacheEvict` 清除对应缓存
3. 下次请求：从数据库重新加载最新配置

## 部署说明

### Docker 部署

```bash
# 构建镜像
docker build -t dms-liteflow:latest .

# 运行容器
docker run -d \
  -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:mysql://mysql:3306/dms_liteflow \
  -e SPRING_DATASOURCE_USERNAME=root \
  -e SPRING_DATASOURCE_PASSWORD=root \
  dms-liteflow:latest
```

### Kubernetes 部署

```bash
# 部署到 Kubernetes
kubectl apply -f k8s/

# 查看状态
kubectl get pods -n dms-liteflow
```

## 监控告警

### 数据清理策略

- 原始执行记录：保留 7 天
- 小时级统计数据：保留 30 天
- 日级统计数据：保留 1 年

### 告警规则

- 失败率超过阈值（默认 50%）
- 5 分钟内最多发送一次告警
- 支持邮件告警

## 贡献指南

欢迎提交 Issue 和 Pull Request！

## 许可证

Apache License 2.0
