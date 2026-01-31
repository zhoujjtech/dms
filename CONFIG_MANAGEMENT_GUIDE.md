# DMS LiteFlow 配置管理指南

## 1. 配置概述

DMS LiteFlow 支持多种配置来源和热更新机制，实现动态配置管理。

## 2. 配置来源

### 2.1 XML 配置 (本地)
配置文件路径：`resources/flow/el-rule.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<flow>
    <chain name="chain1">
        THEN(a, b, c)
    </chain>
</flow>
```

### 2.2 数据库配置 (动态)
配置存储在数据库表 `flow_chain` 中，支持动态加载和热更新。

**优势**：
- 动态修改无需重启
- 版本管理
- 多租户隔离

## 3. 配置加载

### 3.1 配置加载流程

```
应用启动 → FlowConfigLoader → 数据库查询 → FlowConfigService → Caffeine缓存 → LiteFlow引擎
```

### 3.2 配置刷新策略

#### 定时刷新
每 5 分钟自动刷新配置：

```java
@Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
public void refreshAllConfigs() {
    flowConfigService.clearAllCache();
}
```

#### 事件触发
配置发布时立即刷新：

```java
@PostMapping("/{chainId}/publish")
public ResponseEntity<Void> publishChain(@PathVariable Long chainId) {
    flowChainApplicationService.publishChain(tenantId, chainId);
    flowConfigService.evictCache(tenantId, chainName); // 立即刷新
    return ResponseEntity.ok().build();
}
```

## 4. 配置管理 API

### 4.1 创建配置

#### 创建组件
```bash
curl -X POST "http://localhost:8080/api/components" \
  -d "tenantId=1&componentId=validateOrder&componentName=订单校验&componentType=COMMON&content=public class ValidateOrder extends NodeComponent { public void process() { } }"
```

#### 创建流程链
```bash
curl -X POST "http://localhost:8080/api/chains" \
  -d "tenantId=1&chainName=orderProcess&chainCode=THEN(validateOrder,checkStock,createOrder)&description=订单处理流程"
```

#### 创建子流程
```bash
curl -X POST "http://localhost:8080/api/subchains" \
  -d "tenantId=1&subChainName=notifyProcess&chainCode=THEN(sendEmail,sendSMS)&description=通知流程"
```

### 4.2 更新配置

#### 更新组件
```bash
curl -X PUT "http://localhost:8080/api/components/validateOrder" \
  -d "componentName=订单校验&content=public class ValidateOrder extends NodeComponent { public void process() { /* 更新后的逻辑 */ } }"
```

#### 更新流程链
```bash
curl -X PUT "http://localhost:8080/api/chains/1" \
  -d "tenantId=1&chainCode=THEN(validateOrder,checkStock,createOrder,sendEmail)&description=更新后的流程"
```

### 4.3 发布配置

发布后配置生效（已发布配置不能直接修改，需要创建新版本）：

```bash
# 发布组件
curl -X POST "http://localhost:8080/api/components/validateOrder/publish?tenantId=1"

# 发布流程链
curl -X POST "http://localhost:8080/api/chains/1/publish?tenantId=1"

# 发布子流程
curl -X POST "http://localhost:8080/api/subchains/1/publish"
```

### 4.4 启用/禁用配置

```bash
# 启用组件
curl -X POST "http://localhost:8080/api/components/validateOrder/enable?tenantId=1"

# 禁用组件
curl -X POST "http://localhost:8080/api/components/validateOrder/disable?tenantId=1"

# 启用流程链
curl -X POST "http://localhost:8080/api/chains/1/enable?tenantId=1"

# 禁用流程链
curl -X POST "http://localhost:8080/api/chains/1/disable?tenantId=1"
```

## 5. 版本管理

### 5.1 查询版本
```bash
curl -X GET "http://localhost:8080/api/versions?tenantId=1&configType=CHAIN&configId=1"
```

### 5.2 版本对比
```bash
curl -X GET "http://localhost:8080/api/versions/compare?versionId1=1&versionId2=2"
```

### 5.3 版本回滚
```bash
curl -X POST "http://localhost:8080/api/versions/rollback" \
  -d "tenantId=1&configType=CHAIN&configId=1&version=2"
```

## 6. 配置验证

### 6.1 验证组件
```bash
curl -X POST "http://localhost:8080/api/validation/components/validateOrder" \
  -d "tenantId=1"
```

**返回**：
```json
{
  "valid": true,
  "errors": [],
  "warnings": []
}
```

### 6.2 验证流程链
```bash
curl -X POST "http://localhost:8080/api/validation/chains/1" \
  -d "tenantId=1"
```

**验证内容**：
- EL 表达式语法
- 组件存在性
- 循环依赖（待实现）

### 6.3 验证租户所有配置
```bash
curl -X POST "http://localhost:8080/api/validation/tenant" \
  -d "tenantId=1"
```

## 7. 缓存管理

### 7.1 缓存配置

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      specs:
        flowConfigCache:
          expireAfterWrite: 5m  # 5分钟过期
          maximumSize: 1000     # 最多缓存1000个配置
```

### 7.2 缓存操作

#### 清除单个配置缓存
```java
flowConfigService.evictCache(tenantId, chainName);
```

#### 清除所有缓存
```java
flowConfigService.clearAllCache();
```

#### 预加载配置
```java
flowConfigService.preloadConfigs();
```

## 8. 配置状态管理

### 8.1 状态类型

| 状态 | 说明 | 可操作性 |
|------|------|----------|
| DRAFT | 草稿，可修改 | 可更新、可发布、可删除 |
| PUBLISHED | 已发布，生效中 | 不可修改，可创建新版本 |
| ARCHIVED | 已归档，不可用 | 不可修改 |
| ENABLED | 已启用 | 可执行 |
| DISABLED | 已禁用 | 不可执行 |

### 8.2 状态转换

```
DRAFT → PUBLISHED → ENABLED
  ↓         ↓
ARCHIVED  DISABLED
```

## 9. 配置导入导出

### 9.1 导出配置
```sql
SELECT * FROM flow_chain WHERE tenant_id = 1;
SELECT * FROM rule_component WHERE tenant_id = 1;
```

### 9.2 导入配置
```sql
INSERT INTO flow_chain (...) VALUES (...);
INSERT INTO rule_component (...) VALUES (...);
```

## 10. 配置备份

### 10.1 数据库备份
```bash
mysqldump -u root -p dms_liteflow > backup_$(date +%Y%m%d).sql
```

### 10.2 配置版本备份
所有配置修改都会自动创建版本，最多保留 50 个版本。

## 11. 多租户配置隔离

每个租户的配置完全隔离：
- 组件通过 `tenant_id` 隔离
- 流程链通过 `tenant_id` 隔离
- 查询时必须提供 `tenantId` 参数

## 12. 配置最佳实践

### 12.1 命名规范
- 组件 ID: 小驼峰，如 `validateOrder`
- 流程链名称: 小驼峰，如 `orderProcess`
- 子流程名称: 小驼峰，如 `notifyProcess`

### 12.2 版本管理
- 重要修改前先备份
- 使用语义化版本号
- 定期归档旧版本

### 12.3 安全考虑
- 验证用户输入
- 限制配置数量
- 定期审查权限

## 13. 故障排查

### 13.1 配置未生效
1. 检查配置状态是否为 PUBLISHED/ENABLED
2. 清除缓存：`flowConfigService.clearAllCache()`
3. 查看日志确认配置加载成功

### 13.2 组件找不到
1. 验证组件 ID 是否正确
2. 检查组件是否已发布
3. 确认租户 ID 是否匹配

### 13.3 EL 表达式错误
1. 使用验证 API 检查语法
2. 查看 LiteFlow 日志
3. 使用测试 API 验证流程

## 14. 配置监控

### 14.1 查询配置加载状态
```bash
curl -X GET "http://localhost:8080/actuator/health/config"
```

### 14.2 查询组件注册状态
```bash
curl -X GET "http://localhost:8080/actuator/health/components"
```

### 14.3 查询引擎健康状态
```bash
curl -X GET "http://localhost:8080/actuator/health/liteflow"
```
