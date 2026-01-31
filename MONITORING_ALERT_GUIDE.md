# DMS LiteFlow 监控告警指南

## 1. 监控概述

DMS LiteFlow 提供了完整的监控和告警体系，实时采集流程执行数据并支持异常告警。

## 2. 监控数据采集

### 2.1 采集内容

| 数据类型 | 说明 | 存储位置 |
|---------|------|----------|
| 流程执行记录 | 每次流程链的执行信息 | `execution_monitoring` 表 |
| 组件执行记录 | 每个组件的执行信息 | `execution_monitoring` 表 |
| 执行耗时 | 组件/流程的执行时间 | `execution_monitoring.execute_time` |
| 执行状态 | SUCCESS/FAILURE | `execution_monitoring.status` |
| 错误信息 | 失败时的异常堆栈 | `execution_monitoring.error_message` |
| 链路追踪 | 执行ID追踪完整调用链 | `execution_monitoring.chain_execution_id` |

### 2.2 自动采集

流程执行时自动采集监控数据：

```java
// 流程开始时
MonitoringCollectorService.recordChainExecutionStart(
    tenantId, chainId, executionId
);

// 组件执行时
MonitoringCollectorService.recordComponentExecution(
    tenantId, chainId, componentId, executionId,
    executeTime, success, errorMessage
);

// 流程结束时
MonitoringCollectorService.recordChainExecutionEnd(
    tenantId, chainId, executionId,
    executeTime, success, errorMessage
);
```

## 3. 监控数据查询

### 3.1 查询执行记录

#### 按执行ID查询
```bash
curl -X GET "http://localhost:8080/api/monitoring/executions/exec-1234567890"
```

#### 按流程链查询
```bash
curl -X GET "http://localhost:8080/api/monitoring/chains/1/executions?tenantId=1&startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59"
```

#### 按租户查询
```bash
curl -X GET "http://localhost:8080/api/monitoring/tenant/executions?tenantId=1&startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59"
```

### 3.2 查询统计信息

#### 执行统计
```bash
curl -X GET "http://localhost:8080/api/monitoring/chains/1/stats?tenantId=1&startTime=2026-01-01T00:00:00&endTime=2026-01-31T23:59:59"
```

**响应**：
```json
{
  "totalExecutions": 1000,
  "successExecutions": 950,
  "failureExecutions": 50,
  "successRate": 95.0,
  "averageExecuteTime": 200.5
}
```

#### 成功率
```bash
curl -X GET "http://localhost:8080/api/monitoring/chains/1/success-rate?tenantId=1"
```

#### 平均执行时间
```bash
curl -X GET "http://localhost:8080/api/monitoring/chains/1/avg-time?tenantId=1"
```

## 4. 告警配置

### 4.1 告警类型

#### 失败率告警
当流程失败率超过阈值时触发告警。

#### 超时告警
当执行时间超过阈值时记录（待实现）。

#### 异常告警
当流程抛出异常时自动记录到监控。

### 4.2 配置告警

在 `application.yml` 中配置：

```yaml
dms:
  liteflow:
    alert:
      enabled: true
      failure-rate-threshold: 0.1  # 10% 失败率触发告警
      check-interval: 5m            # 每5分钟检查一次
      cooldown: 30m                 # 告警冷却时间30分钟
      email:
        enabled: true
        to: admin@example.com
        subject: "[DMS LiteFlow] 告警通知"
```

### 4.3 告警频率控制

为避免告警轰炸，实施以下策略：

1. **冷却期**: 同一告警在30分钟内只发送一次
2. **聚合告警**: 将多个告警合并发送
3. **优先级**: 根据严重程度分优先级

## 5. 告警发送

### 5.1 邮件告警

#### 配置邮件服务
```yaml
spring:
  mail:
    host: smtp.example.com
    port: 587
    username: your-email@example.com
    password: your-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

#### 告警邮件内容
```java
@Async
public void sendFailureRateAlert(Long tenantId, Long chainId, double failureRate) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(alertConfig.getTo());
    message.setSubject("[DMS LiteFlow] 流程失败率告警");
    message.setText(String.format(
        "租户 %d 的流程 %d 失败率已达到: %.2f%%\n" +
        "告警阈值: %.2f%%\n" +
        "时间: %s",
        tenantId, chainId, failureRate * 100,
        alertConfig.getFailureRateThreshold() * 100,
        LocalDateTime.now()
    ));
    mailSender.send(message);
}
```

### 5.2 其他告警方式（可选）

#### 钉钉告警
```java
public void sendDingDingAlert(String webhook, String content) {
    // POST 请求到钉钉 Webhook
    // 待实现
}
```

#### 企业微信告警
```java
public void sendWeChatWorkAlert(String webhook, String content) {
    // POST 请求到企业微信 Webhook
    // 待实现
}
```

## 6. 数据清理

### 6.1 清理策略

| 数据类型 | 保留时间 | 清理时间 |
|---------|---------|---------|
| 原始执行记录 | 7天 | 每天凌晨2点 |
| 小时级统计 | 30天 | 每天凌晨2点 |
| 日级统计 | 1年 | 每天凌晨2点 |

### 6.2 自动清理任务

```java
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点
public void cleanupOldExecutionRecords() {
    LocalDateTime cutoffTime = LocalDateTime.now().minusDays(7);
    int deletedCount = monitoringQueryService.deleteExpiredRecords(cutoffTime);
    log.info("Deleted {} expired execution records", deletedCount);
}
```

### 6.3 手动清理

```bash
# 删除指定时间之前的记录
curl -X DELETE "http://localhost:8080/api/monitoring/expired?beforeTime=2026-01-01T00:00:00"
```

## 7. 监控数据聚合

### 7.1 聚合类型

#### 小时级统计
按小时聚合执行数据。

#### 日级统计
按天聚合执行数据。

### 7.2 聚合服务（待完善）

```java
@Service
public class MonitoringAggregator {
    public void aggregateHourlyStats(LocalDateTime hour) {
        // 聚合一小时内的数据
    }

    public void aggregateDailyStats(LocalDate date) {
        // 聚合一整天内的数据
    }
}
```

## 8. 链路追踪

### 8.1 执行ID生成

每次执行生成唯一的 `chainExecutionId`：

```java
public String generateExecutionId() {
    return "exec-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
}
```

### 8.2 链路追踪查询

```bash
# 查询完整执行链路
curl -X GET "http://localhost:8080/api/monitoring/executions/exec-1234567890"
```

**响应**：
```json
[
  {
    "componentId": "validateOrder",
    "executeTime": 50,
    "status": "SUCCESS",
    "chainExecutionId": "exec-1234567890"
  },
  {
    "componentId": "checkStock",
    "executeTime": 100,
    "status": "SUCCESS",
    "chainExecutionId": "exec-1234567890"
  }
]
```

## 9. 性能监控

### 9.1 慢查询识别

查询执行时间超过阈值的记录：

```sql
SELECT * FROM execution_monitoring
WHERE execute_time > 1000  -- 超过1秒
ORDER BY execute_time DESC
LIMIT 100;
```

### 9.2 性能趋势分析

```bash
# 查询最近7天的性能趋势
curl -X GET "http://localhost:8080/api/monitoring/trend?chainId=1&days=7"
```

## 10. 异常监控

### 10.1 全局异常上报

所有异常自动上报到监控系统：

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final MonitoringCollectorService monitoringCollectorService;

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // 上报异常到监控
        reportExceptionToMonitoring(ex, "GENERIC_EXCEPTION");
        // ...
    }
}
```

### 10.2 异常查询

```sql
SELECT * FROM execution_monitoring
WHERE status = 'FAILURE'
ORDER BY created_at DESC
LIMIT 100;
```

## 11. 监控仪表盘

### 11.1 健康检查端点

```bash
# 主健康检查
curl -X GET "http://localhost:8080/actuator/health"

# LiteFlow 引擎状态
curl -X GET "http://localhost:8080/actuator/health/liteflow"

# 配置加载状态
curl -X GET "http://localhost:8080/actuator/health/config"

# 组件注册状态
curl -X GET "http://localhost:8080/actuator/health/components"
```

### 11.2 自定义仪表盘（可选）

可以使用 Grafana 等工具创建可视化仪表盘：

1. 配置 Prometheus 数据源
2. 查询监控数据
3. 创建可视化图表

## 12. 告警最佳实践

### 12.1 告警级别

| 级别 | 条件 | 通知方式 |
|------|------|----------|
| INFO | 正常信息 | 日志 |
| WARN | 警告（失败率 < 5%） | 邮件 |
| ERROR | 错误（失败率 >= 5%） | 邮件+短信 |
| CRITICAL | 严重（失败率 >= 10%） | 邮件+短信+电话 |

### 12.2 告警降噪

1. **重复抑制**: 相同告告在冷却期内只发送一次
2. **时间窗口**: 在时间窗口内聚合告警
3. **依赖关系**: 避免级联告警

### 12.3 告警处理流程

1. 接收告警
2. 确认告警级别
3. 查看监控详情
4. 分析根因
5. 处理问题
6. 验证恢复
7. 关闭告警

## 13. 故障排查

### 13.1 查看监控数据
```bash
# 查看失败的执行记录
curl -X GET "http://localhost:8080/api/monitoring/chains/1/executions?tenantId=1&status=FAILURE"
```

### 13.2 分析执行日志
```bash
# 查看执行时间
curl -X GET "http://localhost:8080/api/monitoring/chains/1/avg-time?tenantId=1"
```

### 13.3 检查组件状态
```bash
# 查看组件执行情况
curl -X GET "http://localhost:8080/api/monitoring/components/validateOrder/stats?tenantId=1"
```

## 14. 性能优化建议

### 14.1 监控采样
```yaml
dms:
  liteflow:
    monitoring:
      sampling-rate: 0.1  # 10% 采样率，减少存储压力
```

### 14.2 异步采集
```java
@Async
public void recordComponentExecution(...) {
    // 异步记录，不阻塞主流程
}
```

### 14.3 批量插入
```java
public void batchInsert(List<ExecutionRecord> records) {
    // 批量插入，提高性能
}
```
