# DMS LiteFlow 流程编排指南

## 1. 流程编排概述

LiteFlow 提供了强大的流程编排能力，通过 EL 表达式定义组件之间的执行关系。

## 2. EL 表达式语法

### 2.1 基本编排

#### THEN (串行)
按顺序依次执行组件。

```xml
<chain name="orderProcess">
    THEN(validateOrder, checkStock, createOrder, sendNotification)
</chain>
```

#### WHEN (并行)
同时执行多个组件。

```xml
<chain name="parallelProcess">
    WHEN(sendEmail, sendSMS, sendPush)
</chain>
```

#### SWICTH (分支)
根据选择组件的结果选择执行路径。

```xml
<chain name="paymentProcess">
    SWITCH(selectPaymentMethod).to(
        alipay,
        wechatPay,
        creditCard
    )
</chain>
```

#### IF (条件)
根据条件组件的结果决定是否执行。

```xml
<chain name="vipProcess">
    IF(checkVip).THEN(vipDiscount).ELSE(normalDiscount)
</chain>
```

#### WHILE (循环)
循环执行直到条件不满足。

```xml
<chain name="batchProcess">
    WHILE(hasMoreItems).DO(
        processItem,
        updateProgress
    )
</chain>
```

#### FOR (次数循环)
固定次数循环。

```xml
<chain name="retryProcess">
    FOR(3).THEN(retryOperation)
</chain>
```

### 2.2 复杂编排

#### 嵌套编排
```xml
<chain name="complexProcess">
    THEN(
        validateInput,
        WHEN(
            checkA,
            checkB,
            checkC
        ),
        IF(isValid).THEN(
            processMain
        ).ELSE(
            handleError
        )
    )
</chain>
```

#### 子流程引用
```xml
<chain name="mainProcess">
    THEN(
        initProcess,
        subChain(subProcess),
        finalizeProcess
    )
</chain>
```

#### 条件分支
```xml
<chain name="orderProcess">
    SWITCH(orderType).to(
        THEN(retailOrderProcess).tag("retail"),
        THEN(wholesaleOrderProcess).tag("wholesale"),
        THEN(specialOrderProcess).tag("special")
    )
</chain>
```

## 3. 流程链创建

### 3.1 通过 API 创建

```bash
curl -X POST "http://localhost:8080/api/chains" \
  -d "tenantId=1&chainName=orderProcess&chainCode=THEN(a,b,c)&description=订单处理流程"
```

### 3.2 通过数据库插入
```sql
INSERT INTO flow_chain (
    tenant_id,
    chain_name,
    chain_code,
    description,
    config_type,
    status,
    current_version
) VALUES (
    1,
    'orderProcess',
    'THEN(validateOrder, checkStock, createOrder)',
    '订单处理流程',
    'DATABASE',
    'DRAFT',
    1
);
```

## 4. 流程链管理

### 4.1 查询流程链
```bash
curl -X GET "http://localhost:8080/api/chains?tenantId=1"
```

### 4.2 更新流程链
```bash
curl -X PUT "http://localhost:8080/api/chains/1" \
  -d "chainCode=THEN(a,b,c,d)&description=更新后的流程"
```

### 4.3 发布流程链
```bash
curl -X POST "http://localhost:8080/api/chains/1/publish?tenantId=1"
```

### 4.4 启用/禁用流程链
```bash
# 启用
curl -X POST "http://localhost:8080/api/chains/1/enable?tenantId=1"

# 禁用
curl -X POST "http://localhost:8080/api/chains/1/disable?tenantId=1"
```

## 5. 子流程管理

### 5.1 创建子流程
```bash
curl -X POST "http://localhost:8080/api/subchains" \
  -d "tenantId=1&subChainName=notifyProcess&chainCode=THEN(sendEmail,sendSMS)&description=通知流程"
```

### 5.2 引用子流程
在主流程中使用 `subChain()` 引用：

```xml
<chain name="mainProcess">
    THEN(init, subChain(notifyProcess), finalize)
</chain>
```

或使用 EL 表达式：
```
THEN(init, subChain(notifyProcess), finalize)
```

## 6. 版本管理

### 6.1 创建新版本
每次修改流程链时会自动创建新版本。

### 6.2 版本回滚
```bash
curl -X POST "http://localhost:8080/api/versions/rollback" \
  -d "tenantId=1&configType=CHAIN&configId=1&version=2"
```

### 6.3 版本对比
```bash
curl -X GET "http://localhost:8080/api/versions/compare?versionId1=1&versionId2=2"
```

## 7. 流程测试

### 7.1 测试流程链
```bash
curl -X POST "http://localhost:8080/api/testing/chains/orderProcess" \
  -H "Content-Type: application/json" \
  -d '{"tenantId": 1, "inputData": {"orderId": "12345"}}'
```

### 7.2 测试并获取执行路径
```bash
curl -X POST "http://localhost:8080/api/testing/chains/orderProcess/with-path" \
  -H "Content-Type: application/json" \
  -d '{"tenantId": 1, "inputData": {"orderId": "12345"}}'
```

## 8. 配置验证

### 8.1 验证流程链
```bash
curl -X POST "http://localhost:8080/api/validation/chains/1" \
  -d "tenantId=1"
```

### 8.2 验证租户所有配置
```bash
curl -X POST "http://localhost:8080/api/validation/tenant" \
  -d "tenantId=1"
```

## 9. 流程执行

### 9.1 同步执行
```bash
curl -X POST "http://localhost:8080/api/execute/sync" \
  -H "Content-Type: application/json" \
  -d '{"tenantId": 1, "chainName": "orderProcess", "inputData": "{\\"orderId\\": \\"12345\\"}"}'
```

### 9.2 异步执行
```bash
curl -X POST "http://localhost:8080/api/execute/async" \
  -H "Content-Type: application/json" \
  -d '{"tenantId": 1, "chainName": "orderProcess", "inputData": "{\\"orderId\\": \\"12345\\"}"}'
```

### 9.3 查询执行状态
```bash
curl -X GET "http://localhost:8080/api/execute/status/exec-1234567890"
```

## 10. 最佳实践

### 10.1 组件粒度
- 每个组件只做一件事
- 组件名称要清晰表达功能
- 避免组件过于复杂

### 10.2 流程设计
- 优先使用串行(THEN)保证顺序
- 并行(WHEN)用于独立操作
- 使用子流程复用逻辑
- 合理使用条件分支(SWITCH/IF)

### 10.3 错误处理
- 在组件中捕获并处理业务异常
- 使用 IF 组件处理错误分支
- 配置全局异常处理器

### 10.4 性能优化
- 并行执行独立操作
- 使用缓存减少重复计算
- 控制组件执行时间

## 11. 常见场景示例

### 11.1 订单处理
```xml
<chain name="orderProcess">
    THEN(
        validateOrder,
        IF(checkStock).THEN(
            createOrder,
            WHEN(sendEmail, sendSMS)
        ).ELSE(
            outOfStockNotice
        )
    )
</chain>
```

### 11.2 审批流程
```xml
<chain name="approvalProcess">
    THEN(
        submitRequest,
        SWITCH(approvalLevel).to(
            THEN(managerApprove).tag("manager"),
            THEN(directorApprove).tag("director"),
            THEN(ceoApprove).tag("ceo")
        ),
        IF(isApproved).THEN(notifyApplicant).ELSE(notifyRejection)
    )
</chain>
```

### 11.3 数据同步
```xml
<chain name="dataSync">
    THEN(
        validateData,
        WHEN(
            syncToDB,
            syncToCache,
            syncToES
        ),
        updateSyncStatus
    )
</chain>
```

## 12. 监控和告警

### 12.1 查询执行统计
```bash
curl -X GET "http://localhost:8080/api/monitoring/chains/1/stats?tenantId=1"
```

### 12.2 查询成功率
```bash
curl -X GET "http://localhost:8080/api/monitoring/chains/1/success-rate?tenantId=1"
```

### 12.3 配置告警
在 application.yml 中配置：
```yaml
alert:
  failure-rate-threshold: 0.1  # 10% 失败率触发告警
  email:
    enabled: true
    to: admin@example.com
```
