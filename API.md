# DMS LiteFlow API 文档

## 基础信息

**Base URL**: `http://localhost:8080`

**通用响应格式**:
```json
{
  "timestamp": "2026-01-31T22:00:00",
  "status": 200,
  "error": null,
  "message": "Success",
  "data": { ... }
}
```

**错误响应格式**:
```json
{
  "timestamp": "2026-01-31T22:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "details": { ... }
}
```

---

## 1. 规则组件管理 API

### 1.1 创建组件

**请求**:
```
POST /api/components
Content-Type: application/x-www-form-urlencoded

tenantId=1&componentId=validateOrder&componentName=订单校验&componentType=COMMON&content=public class ValidateOrder extends NodeComponent { public void process() { ... } }
```

**响应**:
```json
{
  "id": 1,
  "tenantId": 1,
  "componentId": "validateOrder",
  "componentName": "订单校验",
  "componentType": "COMMON",
  "content": "...",
  "status": "DRAFT",
  "createdAt": "2026-01-31T22:00:00"
}
```

### 1.2 查询组件列表

**请求**:
```
GET /api/components?tenantId=1&status=PUBLISHED
```

**响应**:
```json
[
  {
    "id": 1,
    "componentId": "validateOrder",
    "componentName": "订单校验",
    "status": "PUBLISHED"
  }
]
```

### 1.3 发布组件

**请求**:
```
POST /api/components/{componentId}/publish?tenantId=1
```

**响应**: `204 No Content`

### 1.4 删除组件

**请求**:
```
DELETE /api/components/{componentId}
```

**响应**: `204 No Content`

---

## 2. 流程链管理 API

### 2.1 创建流程链

**请求**:
```
POST /api/chains
Content-Type: application/x-www-form-urlencoded

tenantId=1&chainName=orderProcess&chainCode=THEN(validateOrder,checkStock,createOrder)&description=订单处理流程&configType=DATABASE
```

**响应**:
```json
{
  "id": 1,
  "tenantId": 1,
  "chainName": "orderProcess",
  "chainCode": "THEN(validateOrder,checkStock,createOrder)",
  "status": "DRAFT",
  "currentVersion": 1,
  "createdAt": "2026-01-31T22:00:00"
}
```

### 2.2 查询流程链列表

**请求**:
```
GET /api/chains?tenantId=1&status=PUBLISHED
```

**响应**:
```json
[
  {
    "id": 1,
    "chainName": "orderProcess",
    "chainCode": "THEN(validateOrder,checkStock,createOrder)",
    "status": "PUBLISHED"
  }
]
```

### 2.3 更新流程链

**请求**:
```
PUT /api/chains/{chainId}
Content-Type: application/x-www-form-urlencoded

tenantId=1&chainCode=THEN(validateOrder,checkStock,createOrder,sendEmail)&description=更新后的流程
```

### 2.4 发布流程链

**请求**:
```
POST /api/chains/{chainId}/publish?tenantId=1
```

### 2.5 删除流程链

**请求**:
```
DELETE /api/chains/{chainId}?tenantId=1
```

---

## 3. 子流程管理 API

### 3.1 创建子流程

**请求**:
```
POST /api/subchains
Content-Type: application/x-www-form-urlencoded

tenantId=1&subChainName=notifyProcess&chainCode=THEN(sendEmail,sendSMS)&description=通知流程&parentChainId=1
```

### 3.2 查询子流程列表

**请求**:
```
GET /api/subchains?tenantId=1
```

### 3.3 更新子流程

**请求**:
```
PUT /api/subchains/{id}
Content-Type: application/x-www-form-urlencoded

subChainName=notifyProcess&chainCode=THEN(sendEmail,sendSMS,sendWechat)
```

### 3.4 发布子流程

**请求**:
```
POST /api/subchains/{id}/publish
```

### 3.5 删除子流程

**请求**:
```
DELETE /api/subchains/{id}
```

---

## 4. 版本管理 API

### 4.1 查询版本列表

**请求**:
```
GET /api/versions?tenantId=1&configType=COMPONENT&configId=1
```

**响应**:
```json
[
  {
    "id": 1,
    "configType": "COMPONENT",
    "configId": 1,
    "version": 1,
    "content": "...",
    "status": "PUBLISHED",
    "createdAt": "2026-01-31T22:00:00"
  }
]
```

### 4.2 查询版本详情

**请求**:
```
GET /api/versions/{configType}/{configId}/versions/{version}?tenantId=1
```

### 4.3 发布版本

**请求**:
```
POST /api/versions/{configType}/{configId}/versions/{version}/publish?tenantId=1
```

### 4.4 归档版本

**请求**:
```
POST /api/versions/{configType}/{configId}/versions/{version}/archive?tenantId=1
```

### 4.5 删除版本

**请求**:
```
DELETE /api/versions/{versionId}
```

### 4.6 版本对比

**请求**:
```
GET /api/versions/compare?versionId1=1&versionId2=2
```

### 4.7 回滚版本

**请求**:
```
POST /api/versions/rollback
Content-Type: application/x-www-form-urlencoded

tenantId=1&configType=COMPONENT&configId=1&version=1
```

---

## 5. 配置验证 API

### 5.1 验证流程链

**请求**:
```
POST /api/validation/chains/{chainId}
Content-Type: application/x-www-form-urlencoded

tenantId=1
```

**响应**:
```json
{
  "valid": true,
  "errors": [],
  "warnings": []
}
```

### 5.2 验证组件

**请求**:
```
POST /api/validation/components/{componentId}
Content-Type: application/x-www-form-urlencoded

tenantId=1
```

### 5.3 验证租户所有配置

**请求**:
```
POST /api/validation/tenant
Content-Type: application/x-www-form-urlencoded

tenantId=1
```

### 5.4 验证所有流程链

**请求**:
```
POST /api/validation/chains
Content-Type: application/x-www-form-urlencoded

tenantId=1
```

---

## 6. 测试调试 API

### 6.1 测试组件

**请求**:
```
POST /api/testing/components/{componentId}
Content-Type: application/json

{
  "tenantId": 1,
  "inputData": {
    "orderId": "12345"
  }
}
```

**响应**:
```json
{
  "success": true,
  "outputData": { ... },
  "executeTime": 150,
  "executionSteps": [ ... ]
}
```

### 6.2 测试流程链

**请求**:
```
POST /api/testing/chains/{chainName}
Content-Type: application/json

{
  "tenantId": 1,
  "inputData": {
    "orderId": "12345"
  }
}
```

### 6.3 测试流程链并返回执行路径

**请求**:
```
POST /api/testing/chains/{chainName}/with-path
Content-Type: application/json

{
  "tenantId": 1,
  "inputData": { ... }
}
```

### 6.4 批量测试组件

**请求**:
```
POST /api/testing/components/{componentId}/batch?tenantId=1
```

### 6.5 保存测试用例

**请求**:
```
POST /api/testing/test-cases
Content-Type: application/x-www-form-urlencoded

tenantId=1&componentId=validateOrder&name=正常订单测试&inputData={"orderId":"12345"}&expectedResult={"success":true}
```

### 6.6 执行测试用例

**请求**:
```
POST /api/testing/test-cases/{testCaseId}/execute
```

---

## 7. 监控查询 API

### 7.1 查询执行记录

**请求**:
```
GET /api/monitoring/executions/{executionId}
```

**响应**:
```json
[
  {
    "id": 1,
    "tenantId": 1,
    "chainId": 1,
    "componentId": "validateOrder",
    "chainExecutionId": "exec-123",
    "executeTime": 150,
    "status": "SUCCESS",
    "createdAt": "2026-01-31T22:00:00"
  }
]
```

### 7.2 查询流程链执行记录

**请求**:
```
GET /api/monitoring/chains/{chainId}/executions
?tenantId=1
&startTime=2026-01-01T00:00:00
&endTime=2026-01-31T23:59:59
```

### 7.3 查询租户执行记录

**请求**:
```
GET /api/monitoring/tenant/executions
?tenantId=1
&startTime=2026-01-01T00:00:00
&endTime=2026-01-31T23:59:59
```

### 7.4 获取执行统计

**请求**:
```
GET /api/monitoring/chains/{chainId}/stats
?tenantId=1
&startTime=2026-01-01T00:00:00
&endTime=2026-01-31T23:59:59
```

**响应**:
```json
{
  "totalExecutions": 1000,
  "successExecutions": 950,
  "failureExecutions": 50,
  "successRate": 95.0,
  "averageExecuteTime": 200.5
}
```

### 7.5 获取成功率

**请求**:
```
GET /api/monitoring/chains/{chainId}/success-rate
?tenantId=1
&startTime=2026-01-01T00:00:00
&endTime=2026-01-31T23:59:59
```

**响应**:
```json
95.0
```

### 7.6 获取平均执行时间

**请求**:
```
GET /api/monitoring/chains/{chainId}/avg-time
?tenantId=1
&startTime=2026-01-01T00:00:00
&endTime=2026-01-31T23:59:59
```

### 7.7 删除过期记录

**请求**:
```
DELETE /api/monitoring/expired?beforeTime=2026-01-01T00:00:00
```

---

## 8. 租户管理 API

### 8.1 创建租户

**请求**:
```
POST /api/tenants
Content-Type: application/x-www-form-urlencoded

tenantCode=TENANT001&tenantName=租户1&maxChains=100&maxComponents=1000
```

### 8.2 查询所有租户

**请求**:
```
GET /api/tenants
```

### 8.3 更新租户

**请求**:
```
PUT /api/tenants/{id}
Content-Type: application/x-www-form-urlencoded

tenantName=新名称&maxChains=200&maxComponents=2000
```

### 8.4 激活租户

**请求**:
```
POST /api/tenants/{id}/activate
```

### 8.5 停用租户

**请求**:
```
POST /api/tenants/{id}/deactivate
```

### 8.6 检查配额

**请求**:
```
GET /api/tenants/{id}/quota
```

**响应**:
```json
true  // 或 false
```

---

## 9. 错误码说明

| HTTP 状态码 | 错误类型 | 说明 |
|------------|---------|------|
| 200 | OK | 请求成功 |
| 204 | No Content | 删除/更新成功，无返回内容 |
| 400 | Bad Request | 请求参数错误 |
| 403 | Forbidden | 无权限访问 |
| 404 | Not Found | 资源不存在 |
| 409 | Conflict | 资源冲突（如状态不允许操作） |
| 500 | Internal Server Error | 服务器内部错误 |

## 10. 请求示例

### cURL 示例

```bash
# 创建组件
curl -X POST "http://localhost:8080/api/components" \
  -d "tenantId=1&componentId=testComponent&componentName=测试组件&componentType=COMMON&content=public class TestComponent extends NodeComponent { public void process() { } }"

# 查询组件列表
curl -X GET "http://localhost:8080/api/components?tenantId=1&status=PUBLISHED"

# 发布组件
curl -X POST "http://localhost:8080/api/components/testComponent/publish?tenantId=1"

# 创建流程链
curl -X POST "http://localhost:8080/api/chains" \
  -d "tenantId=1&chainName=testChain&chainCode=THEN(a,b,c)&description=测试流程"

# 测试组件
curl -X POST "http://localhost:8080/api/testing/components/testComponent" \
  -H "Content-Type: application/json" \
  -d '{"tenantId": 1, "inputData": {"test": "data"}}'

# 查询执行统计
curl -X GET "http://localhost:8080/api/monitoring/chains/1/stats?tenantId=1"
```

---

## 11. 通用参数说明

### tenantId
**类型**: Long
**必填**: 是（大部分接口）
**说明**: 租户ID，用于多租户隔离

### status
**类型**: String
**可选**: 是
**可选值**:
- `DRAFT` - 草稿
- `PUBLISHED` - 已发布
- `ARCHIVED` - 已归档

### configType
**类型**: String
**可选**: 是
**可选值**:
- `COMPONENT` - 组件
- `CHAIN` - 流程链
- `SUB_CHAIN` - 子流程

---

## 12. 注意事项

1. **租户隔离**: 所有请求必须提供 tenantId 参数（通过 header 或 parameter）
2. **状态检查**: 已发布的配置不能直接更新，需要创建新版本
3. **删除限制**: 已发布的配置不能删除，需要先归档
4. **配额管理**: 创建配置前建议检查租户配额
5. **错误处理**: 所有错误都返回统一的错误格式

---

**文档版本**: v1.0
**最后更新**: 2026-01-31
**维护者**: DMS Team
