# Saga 分布式事务支持 - 技术设计

## Context

### 当前状态

DMS LiteFlow 是一个基于 LiteFlow 引擎的流程编排系统，当前具备：
- LiteFlow 流程编排引擎集成（通过 EL 表达式定义流程）
- XXL-JOB 分布式调度支持
- Redis 分布式缓存（已集成）
- 多租户隔离（基于 TenantContext 拦截器）
- 流程执行监控（ExecutionRecord）
- 组件和链路的动态配置管理

**当前限制**：
- 流程执行失败时无法自动回滚已执行的操作
- 跨服务调用（通过 Feign/RestTemplate）失败后数据不一致
- 缺乏执行状态的可视化追踪
- 无法人工介入处理异常流程

### 业务背景

在典型的订单处理场景中：
```
validateOrder → checkStock → createOrder → reserveStock → payment → confirmStock
```

如果 `payment` 节点失败，前面的 `createOrder` 和 `reserveStock` 已经执行，需要回滚但当前系统无法自动处理。

### 技术约束

- **架构模式**：同进程 Saga（单应用 + 外部服务调用）
- **通信方式**：Feign/RestTemplate 同步调用外部服务
- **数据存储**：Redis（缓存） + MySQL（持久化）
- **流程引擎**：LiteFlow 2.11.x
- **调度框架**：XXL-JOB
- **多租户**：已实现租户上下文隔离

### 利益相关者

- **开发团队**：需要清晰的扩展点来集成 Saga
- **运维团队**：需要可视化的监控和管理界面
- **业务团队**：需要保证数据一致性，支持人工介入

## Goals / Non-Goals

### Goals

1. **自动补偿机制**
   - 流程失败时按相反顺序自动执行补偿操作
   - 支持多种失败策略（自动补偿、重试、人工介入）
   - 补偿组件可访问原始节点的输出数据

2. **状态管理和追踪**
   - 实时记录每个节点的执行状态
   - 持久化执行历史用于审计
   - 支持执行状态的查询和可视化

3. **人工干预能力**
   - 提供管理界面支持手动补偿、重试、跳过
   - 需要人工决策的场景支持暂停和审核
   - 完整的操作审计日志

4. **向后兼容**
   - 现有流程配置无需修改即可继续使用
   - Saga 模式为可选功能（通过参数或配置启用）

### Non-Goals

- **跨服务事务协调器**
  - 不实现分布式事务协调器（如 Seata）
  - 不引入额外的中间件依赖

- **两阶段提交（2PC）**
  - 不实现强一致性的 2PC 协议
  - 采用最终一致性的 Saga 模式

- **补偿自动生成**
  - 不自动生成补偿逻辑（需要开发人员编写）
  - 不提供 AI 辅助的补偿代码生成

- **完整的 BPMN 支持**
  - 不实现完整的 BPMN 2.0 规范
  - 保持 LiteFlow 的简洁性

## Decisions

### Decision 1: Saga 编排方式 - 事件驱动 vs 显式调用

**选择：基于 LiteFlow 事件的自动编排**

**理由**：
- ✅ 紧密集成 LiteFlow，无需修改现有组件代码
- ✅ 利用 LiteFlow 的事件机制（BEFORE_NODE, AFTER_SUCCESS, AFTER_FAILURE）
- ✅ 对业务组件无侵入性，通过注解或元数据声明补偿关系
- ✅ 补偿也是节点，可以使用 LiteFlow 的并行、条件等编排能力

**替代方案**：
- **显式调用**：在每个组件中显式调用 SagaService
  - ❌ 业务代码侵入性强
  - ❌ 容易遗漏补偿逻辑
  - ❌ 难以统一管理

**实现**：
```java
@Component
public class SagaEventListener {

    @EventListener
    public void onBeforeNode(BeforeNodeEvent event) {
        // 记录节点开始
        sagaStateService.recordStepStart(executionId, event.getNodeId());
    }

    @EventListener
    public void onAfterSuccess(AfterNodeSuccessEvent event) {
        // 记录节点成功，保存输出数据
        sagaStateService.recordStepSuccess(executionId, event.getNodeId(), event.getOutput());
    }

    @EventListener
    public void onAfterFailure(AfterNodeFailureEvent event) {
        // 记录失败，触发补偿
        sagaStateService.recordStepFailure(executionId, event.getNodeId(), event.getError());
        compensationOrchestrator.compensate(executionId);
    }
}
```

---

### Decision 2: 补偿组件定义方式 - 组件成对 vs 内置方法

**选择：组件成对创建（Component Pair）**

**理由**：
- ✅ 职责分离清晰，业务组件和补偿组件独立
- ✅ 补偿组件可以单独测试和复用
- ✅ 符合单一职责原则
- ✅ 易于理解和管理

**替代方案**：
- **内置补偿方法**：在业务组件中定义 `compensate()` 方法
  - ❌ 组件职责增加
  - ❌ 违反单一职责原则
  - ✅ 但补偿逻辑可访问正向逻辑的内部状态（这一条优势在组件成对方式中通过上下文传递解决）

**实现**：
```java
// 业务组件
@LiteflowComponent("createOrder")
@SagaMetadata(
    compensateComponent = "cancelOrder",
    needsCompensation = true,
    failureStrategy = FailureStrategy.AUTO_COMPENSATE
)
public class CreateOrderComponent extends NodeComponent {
    @Override
    public void process() {
        String orderId = orderService.createOrder(...);
        this.getContext().setData("orderId", orderId);
    }
}

// 补偿组件
@LiteflowComponent("cancelOrder")
@CompensationFor("createOrder")  // 标注为补偿组件
public class CancelOrderComponent extends NodeComponent {
    @Override
    public void process() {
        // 从上下文获取原始节点的输出数据
        String orderId = this.getContext().getData("createOrder.orderId");
        orderService.cancelOrder(orderId);
    }
}
```

**元数据配置方式**（优先级：代码注解 > 数据库配置）：
- **推荐**：代码注解 - 类型安全，IDE 支持
- **备选**：数据库配置 - 动态调整，无需重新部署

---

### Decision 3: 状态存储策略 - Redis vs MySQL

**选择：混合存储（Redis 热数据 + MySQL 持久化）**

**理由**：
- ✅ Redis 提供毫秒级读取性能，适合运行时频繁访问
- ✅ MySQL 提供持久化存储，支持历史查询和审计
- ✅ 利用现有基础设施，无需新增依赖
- ✅ Redis 故障时可降级到纯 MySQL 存储

**存储策略**：

| 数据类型 | Redis | MySQL | 说明 |
|---------|-------|-------|------|
| 执行中的 Saga | ✅ 主存储 | ✅ 异步同步 | 运行时高频读写 |
| 已完成的 Saga | ✅ 24小时 | ✅ 持久化 | Redis 快速查询，MySQL 历史归档 |
| 执行栈（executionStack） | ✅ 完整 | ✅ JSON 字段 | 补偿时必须数据 |
| 步骤执行详情 | ✅ 摘要 | ✅ 完整 | Redis 存储关键数据 |
| 补偿日志 | ❌ | ✅ | 审计需求，低频访问 |

**Redis Key 设计**：
```
saga:execution:{tenantId}:{executionId}           # 完整执行状态
saga:execution:{tenantId}:{executionId}:stack     # 执行栈
saga:execution:{tenantId}:{executionId}:lock      # 分布式锁
saga:execution:query:{tenantId}:{queryHash}       # 查询结果缓存
```

**写入策略**：
- 节点成功：异步写 MySQL（先写 Redis）
- 节点失败：同步写 MySQL（确保不丢失）
- 补偿操作：同步写 MySQL

---

### Decision 4: Saga 服务分层 - 扩展现有 vs 独立服务

**选择：独立服务层（SagaExecutionService）**

**理由**：
- ✅ 职责分离，`ExecutionService` 保持简洁
- ✅ Saga 逻辑独立演进，不影响现有流程
- ✅ 易于测试和维护
- ✅ 支持未来扩展（如跨服务 Saga）

**替代方案**：
- **扩展现有 ExecutionService**：在现有服务中添加 Saga 逻辑
  - ❌ `ExecutionService` 职责过重
  - ❌ Saga 逻辑与普通执行逻辑耦合
  - ✅ 但减少了服务数量

**服务架构**：
```
┌─────────────────────────────────────────────────────┐
│                   API Layer                        │
│  ┌──────────────┐   ┌──────────────┐              │
│  │ Execution    │   │ Saga         │              │
│  │ Controller   │   │ Controller   │              │
│  └──────────────┘   └──────────────┘              │
└─────────────────────────────────────────────────────┘
         │                    │
         ▼                    ▼
┌─────────────────────────────────────────────────────┐
│              Application Layer                      │
│  ┌──────────────────┐   ┌─────────────────────┐   │
│  │ExecutionService  │   │SagaExecutionService │   │
│  │  - executeSync   │   │  - executeSaga      │   │
│  │  - executeAsync  │   │  - compensate       │   │
│  │                  │   │  - retry            │   │
│  └──────────────────┘   │  - manualDecision   │   │
│                         └─────────────────────┘   │
│  ┌─────────────────────────────────────────────┐  │
│  │         SagaStateService                    │  │
│  │  - saveStepState                            │  │
│  │  - getExecutionStack                        │  │
│  │  - updateStatus                             │  │
│  └─────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
         │                    │
         ▼                    ▼
┌─────────────────────────────────────────────────────┐
│            Infrastructure Layer                     │
│  ┌─────────────────────────────────────────────┐  │
│  │         SagaEventListener                    │  │
│  │  (监听 LiteFlow 事件)                        │  │
│  └─────────────────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────┐  │
│  │      CompensationOrchestrator                │  │
│  │  (编排补偿流程)                               │  │
│  └─────────────────────────────────────────────┘  │
│  ┌──────────────────┐   ┌──────────────────┐     │
│  │ Redis Cache      │   │ Saga Repository  │     │
│  └──────────────────┘   └──────────────────┘     │
└─────────────────────────────────────────────────────┘
```

**API 路由策略**：
- `/api/execute/*` - 现有端点，保持不变
- `/api/saga/*` - 新增 Saga 专用端点
- 自动判断：通过 `FlowChain.transactional` 字段或请求参数判断

---

### Decision 5: 失败处理策略 - 单一策略 vs 条件式策略

**选择：条件式失败策略（Conditional Failure Strategy）**

**理由**：
- ✅ 支持不同错误类型的差异化处理
- ✅ 灵活性高，可配置复杂业务规则
- ✅ 减少不必要的补偿（如临时性错误重试即可）

**策略配置**：
```java
@SagaMetadata(
    failureStrategy = {
        @FailureRule(
            condition = "PAYMENT_TIMEOUT",
            action = ActionType.RETRY,
            retryCount = 3
        ),
        @FailureRule(
            condition = "INSUFFICIENT_FUNDS",
            action = ActionType.AUTO_COMPENSATE
        ),
        @FailureRule(
            condition = "RISK_CHECK_FAILED",
            action = ActionType.MANUAL,
            reason = "需要人工审核"
        )
    }
)
```

**默认策略**（未配置时）：
- 网络超时、连接失败 → RETRY（最多 3 次）
- 业务逻辑失败（如余额不足） → AUTO_COMPENSATE
- 系统异常（如数据库不可用） → AUTO_COMPENSATE + 告警
- 未知错误 → AUTO_COMPENSATE

---

### Decision 6: 数据模型设计 - 关系型 vs 文档型

**选择：关系型模型（MySQL 表）+ JSON 字段**

**理由**：
- ✅ 利用现有 MySQL 基础设施
- ✅ 支持复杂查询和关联
- ✅ JSON 字段存储非结构化数据（如输出数据）
- ✅ 事务一致性保证

**数据表设计**：

#### 1. saga_execution（执行实例表）
```sql
CREATE TABLE saga_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(64) NOT NULL UNIQUE,
    tenant_id BIGINT NOT NULL,
    chain_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,  -- PENDING/RUNNING/COMPLETED/FAILED/COMPENSATING/COMPENSATED/MANUAL_INTERVENTION
    current_step_index INT DEFAULT 0,
    failure_reason TEXT,
    input_data JSON,
    output_data JSON,
    execution_stack JSON,  -- 执行栈（用于补偿）
    started_at DATETIME NOT NULL,
    completed_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    version INT DEFAULT 1,  -- 乐观锁

    INDEX idx_tenant_chain (tenant_id, chain_name),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 2. saga_step_execution（节点执行状态表）
```sql
CREATE TABLE saga_step_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    component_name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,  -- RUNNING/COMPLETED/FAILED/SKIPPED
    input_data JSON,
    output_data JSON,
    compensate_component VARCHAR(128),
    needs_compensation BOOLEAN DEFAULT FALSE,
    error_code VARCHAR(64),
    error_message TEXT,
    stack_trace TEXT,
    executed_at DATETIME,
    compensated_at DATETIME,

    INDEX idx_execution (execution_id),
    INDEX idx_step_id (step_id),
    FOREIGN KEY (execution_id) REFERENCES saga_execution(execution_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 3. saga_compensation_log（补偿日志表）
```sql
CREATE TABLE saga_compensation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(64) NOT NULL,
    step_id VARCHAR(64) NOT NULL,
    compensate_component VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,  -- SUCCESS/FAILED/SKIPPED
    error_message TEXT,
    compensated_at DATETIME NOT NULL,
    operator VARCHAR(128),  -- 如果是手动触发
    operation_type VARCHAR(32) DEFAULT 'AUTO',  -- AUTO/MANUAL

    INDEX idx_execution (execution_id),
    INDEX idx_compensated_at (compensated_at),
    FOREIGN KEY (execution_id) REFERENCES saga_execution(execution_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 4. saga_component_metadata（组件元数据表）
```sql
CREATE TABLE saga_component_metadata (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    component_name VARCHAR(128) NOT NULL,
    compensate_component VARCHAR(128),
    needs_compensation BOOLEAN DEFAULT FALSE,
    default_failure_strategy VARCHAR(32) DEFAULT 'AUTO_COMPENSATE',
    timeout_ms INT DEFAULT 30000,
    metadata JSON,  -- 扩展配置
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    UNIQUE KEY uk_tenant_component (tenant_id, component_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

#### 5. saga_manual_intervention（人工介入记录表）
```sql
CREATE TABLE saga_manual_intervention (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    execution_id VARCHAR(64) NOT NULL,
    intervention_type VARCHAR(32) NOT NULL,  -- COMPENSATE/RETRY/SKIP/CONTINUE
    decision VARCHAR(32) NOT NULL,
    reason TEXT,
    operator VARCHAR(128) NOT NULL,
    operated_at DATETIME NOT NULL,
    input_data JSON,  -- 修改后的输入数据（可选）

    INDEX idx_execution (execution_id),
    INDEX idx_operated_at (operated_at),
    FOREIGN KEY (execution_id) REFERENCES saga_execution(execution_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

---

### Decision 7: 并发控制 - 乐观锁 vs 分布式锁

**选择：混合策略（乐观锁 + 分布式锁）**

**乐观锁**（用于数据更新）：
```sql
UPDATE saga_execution
SET status = 'COMPENSATING', version = version + 1
WHERE execution_id = ? AND version = ?
```

- ✅ 性能高，无锁竞争
- ✅ 自动检测并发冲突
- ❌ 冲突时需要重试

**分布式锁**（用于关键操作）：
```
Redis Key: saga:lock:{executionId}
TTL: 30 秒
```

用于场景：
- 触发补偿流程
- 手动操作（补偿、重试、决策）
- 批量操作

**实现**（基于 Redisson）：
```java
public void compensate(String executionId) {
    RLock lock = redissonClient.getLock("saga:lock:" + executionId);
    try {
        if (lock.tryLock(10, 30, TimeUnit.SECONDS)) {
            // 执行补偿逻辑
            doCompensate(executionId);
        } else {
            throw new SagaException("获取锁失败，操作正在进行中");
        }
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

---

### Decision 8: 幂等性保证机制

**选择：基于唯一标识 + 业务状态检查**

**实现层次**：

1. **框架层**：记录补偿执行状态
   - `saga_compensation_log` 记录每次补偿操作
   - 补偿前检查是否已补偿

2. **业务层**：补偿组件实现幂等
   ```java
   @LiteflowComponent("cancelOrder")
   public class CancelOrderComponent extends NodeComponent {
       @Override
       public void process() {
           String orderId = this.getContext().getData("createOrder.orderId");
           Order order = orderService.getById(orderId);

           // 幂等性检查：如果订单已取消，直接返回
           if (order.getStatus() == OrderStatus.CANCELLED) {
               log.info("Order already cancelled: {}", orderId);
               return;
           }

           // 执行取消操作
           orderService.cancel(orderId);
       }
   }
   ```

3. **外部服务层**：使用幂等键
   - Feign 调用携带 `idempotentKey`
   - 外部服务根据键去重

---

### Decision 9: 监控和告警集成

**集成点**：

1. **现有监控**：
   - `ExecutionRecord` 扩展：添加 Saga 相关字段
   - `MonitoringCollectorService`：收集 Saga 指标
   - `AlertService`：配置 Saga 告警规则

2. **新增指标**：
   - Prometheus metrics（可选）
   - 自定义指标：`saga_execution_total`, `saga_compensation_total`, `saga_manual_intervention_total`

3. **告警规则**：
   ```yaml
   alert:
     rules:
       - name: saga-compensation-failed
         condition: compensation_failure_rate > 0.05
         duration: 5m
         action: email, slack

       - name: saga-manual-intervention
         condition: manual_intervention_count > 10
         duration: 1h
         action: email, dingtalk
   ```

---

### Decision 10: XXL-JOB 集成

**定时任务设计**：

| 任务ID | Cron | 功能 |
|--------|------|------|
| saga-cleanup-redis | `0 0 2 * * ?` | 清理 Redis 中超过 24 小时的已完成记录 |
| saga-cleanup-mysql | `0 0 3 * * 1` | 归档 MySQL 中 90 天前的数据 |
| saga-compensation-retry | `0 */10 * * * ?` | 重试失败的补偿操作（配置化） |
| saga-timeout-check | `0 */5 * * * ?` | 检查并标记超时的 Saga |

**任务注册**：
```java
@Component
public class SagaCleanupHandler {

    @XxlJob("sagaCleanupRedisJob")
    public void cleanupRedis() {
        // 清理过期数据
    }

    @XxlJob("sagaCleanupMysqlJob")
    public void cleanupMysql() {
        // 归档历史数据
    }
}
```

---

## Risks / Trade-offs

### Risk 1: 补偿失败导致数据不一致

**风险描述**：补偿操作本身可能失败（如外部服务不可用），导致部分数据已回滚，部分未回滚。

**缓解措施**：
- ✅ 补偿失败时继续执行后续节点的补偿（不中断）
- ✅ 记录详细的补偿日志，支持人工介入
- ✅ 提供"重试补偿"和"手动补偿"功能
- ✅ 告警通知运维人员及时处理
- ✅ 补偿组件实现幂等性，支持无限重试

**监控指标**：
- 补偿成功率（目标 > 99%）
- 补偿失败告警

---

### Risk 2: Redis 故障导致执行状态丢失

**风险描述**：Redis 不可用时，正在执行的 Saga 状态丢失，无法正常补偿。

**缓解措施**：
- ✅ MySQL 同步写入失败数据（关键数据不丢失）
- ✅ Redis 故障时降级为纯 MySQL 存储
- ✅ 应用启动时从 MySQL 恢复未完成的 Saga
- ✅ Redis 主从复制 + 哨兵模式（高可用）

**降级逻辑**：
```java
public void saveStepState(StepExecution step) {
    try {
        // 先写 Redis
        redisTemplate.opsForValue().set("saga:step:" + step.getStepId(), step);
    } catch (RedisException e) {
        log.warn("Redis unavailable, fallback to MySQL only", e);
        metricsService.increment("saga.redis.fallback");
    }

    // 同步/异步写 MySQL
    sagaStepRepository.save(step);
}
```

---

### Risk 3: 性能开销

**风险描述**：每个节点执行时都需要记录状态，可能增加 10-50ms 延迟。

**影响评估**：
- 节点执行时间：通常 100-1000ms
- 状态记录开销：约 5-20ms（Redis 写入）
- 性能影响：约 2-10%

**缓解措施**：
- ✅ Redis 使用 Pipeline 批量写入
- ✅ 异步写入 MySQL（成功场景）
- ✅ 只在 Saga 模式下记录状态（可选功能）
- ✅ Redis 使用本地缓存（本地 + 远程二级缓存）

**性能测试基准**：
- 单流程 10 个节点：总延迟增加 < 200ms
- 并发 100 TPS：CPU 增加 < 5%

---

### Risk 4: 补偿逻辑错误

**风险描述**：开发人员编写的补偿组件可能存在逻辑错误，导致"越补越乱"。

**缓解措施**：
- ✅ 补偿组件强制实现幂等性
- ✅ 单元测试覆盖补偿逻辑
- ✅ 提供补偿组件模板和最佳实践文档
- ✅ 测试环境验证后才能上线
- ✅ 提供"干运行"模式（Dry Run），只记录不执行

**补偿组件 Checklist**：
- [ ] 检查业务实体状态，避免重复补偿
- [ ] 使用唯一标识（如 orderId）定位原始数据
- [ ] 处理补偿失败场景（记录日志，不抛异常）
- [ ] 更新业务实体状态为"已取消/已回滚"

---

### Risk 5: 长事务导致资源占用

**风险描述**：复杂的 Saga 可能包含多个步骤，执行时间较长（如 1-5 分钟），占用数据库连接和内存。

**缓解措施**：
- ✅ 设置流程级超时（默认 5 分钟）
- ✅ 异步执行模式，不阻塞请求线程
- ✅ 执行栈只存储关键数据（不是完整上下文）
- ✅ 定期清理已完成的 Saga

**超时配置**：
```yaml
saga:
  timeout:
    default: 300000  # 5 分钟
    max: 600000      # 10 分钟（硬限制）
```

---

### Trade-off 1: 一致性 vs 可用性

**选择**：最终一致性（AP），而非强一致性（CP）

**理由**：
- Saga 模式本质是最终一致性
- 允许短暂的数据不一致窗口（通常 < 1 秒）
- 优先保证系统可用性（补偿失败不阻塞主流程）

**一致性保证**：
- 正常流程：所有节点成功，数据一致
- 失败场景：补偿完成后达到最终一致
- 异常场景：告警 + 人工介入

---

### Trade-off 2: 灵活性 vs 复杂性

**选择**：支持高灵活性（条件式失败策略、人工介入）

**代价**：
- 配置复杂度增加
- 需要管理界面支持
- 开发人员学习成本

**平衡措施**：
- 提供合理的默认策略
- 提供配置模板
- 文档和培训支持

---

## Migration Plan

### 阶段 1：基础设施准备（Week 1-2）

**任务**：
1. 创建数据库表（Flyway migration）
2. 添加 Maven 依赖（如 Redisson）
3. 设置 Redis key 规范和 TTL 配置
4. 创建基础包结构

**验证**：
- 单元测试：Repository 层数据访问
- 集成测试：Redis 读写性能

---

### 阶段 2：核心功能开发（Week 3-5）

**任务**：
1. 实现 `SagaEventListener`（LiteFlow 事件监听）
2. 实现 `SagaStateService`（状态管理）
3. 实现 `CompensationOrchestrator`（补偿编排）
4. 实现 `SagaExecutionService`（执行服务）
5. 创建领域模型和 Repository

**验证**：
- 单元测试：各个 Service 的核心逻辑
- 集成测试：完整的 Saga 执行和补偿流程
- 性能测试：状态记录的性能开销

---

### 阶段 3：管理界面开发（Week 6-7）

**任务**：
1. 实现 Saga 管理 API（查询、补偿、重试等）
2. 实现统计指标 API
3. 前端页面开发（如使用 Vue/React）
4. 集成现有监控系统

**验证**：
- E2E 测试：完整的用户操作流程
- UI/UX 测试：界面易用性

---

### 阶段 4：示例和文档（Week 8）

**任务**：
1. 编写示例补偿组件
2. 编写最佳实践文档
3. 编写 API 文档
4. 培训和分享

---

### 阶段 5：灰度发布（Week 9-10）

**策略**：
1. **特性开关**：通过配置控制 Saga 功能启用
2. **租户级别灰度**：先对测试租户启用
3. **流程级别灰度**：先对非关键流程启用
4. **监控观察**：观察成功率、性能指标
5. **全量发布**：确认稳定后全量启用

**回滚策略**：
- 关闭特性开关，Saga 功能降级
- 已执行的 Saga 不受影响（状态已持久化）
- 新执行流程走原有逻辑（不启用 Saga）

---

### 阶段 6：清理和优化（Week 11-12）

**任务**：
1. 清理测试数据
2. 性能优化（如 Redis Pipeline）
3. 补偿失败案例分析
4. 文档更新

---

## Open Questions

### Q1: 补偿组件是否支持异步执行？

**当前设计**：补偿组件同步执行（按顺序）

**考虑**：
- 异步补偿可以提高性能（并行补偿无依赖的节点）
- 但增加了复杂度（需要等待所有补偿完成）
- 且补偿操作通常需要保证顺序

**决策**：**V1.0 同步执行**，后续版本根据实际需求考虑异步补偿

---

### Q2: 是否支持 Saga 嵌套（子流程的独立补偿）？

**场景**：主流程调用子流程 `subChain(paymentProcess)`，子流程失败后如何补偿？

**选项**：
- **A**：子流程整体补偿（一个补偿组件）
- **B**：子流程内部节点逐个补偿
- **C**：支持配置，由用户选择

**当前倾向**：**选项 C（可配置）**
- 默认：子流程整体补偿
- 可配置：子流程内部展开补偿

**需要确认**：子流程元数据如何定义？

---

### Q3: 执行栈数据大小限制

**问题**：如果流程包含很多节点，且每个节点的输出数据很大，执行栈可能占用大量内存。

**当前设计**：
- 将完整 executionStack 存储在 Redis（单 key）
- JSON 格式，无大小限制

**风险**：
- Redis 单个 key 最大 512MB
- 但过大的数据会影响性能

**可能的优化**：
- 只存储关键数据（如 ID）
- 完整数据存储在 MySQL，Redis 只存引用
- 分页加载（不一次性加载所有数据）

**需要确认**：输出数据的典型大小？是否需要限制？

---

### Q4: 补偿超时如何处理？

**场景**：补偿组件执行时间过长或卡死

**选项**：
- **A**：设置补偿超时，超时后强制标记为失败
- **B**：不设置超时，等待补偿完成
- **C**：超时后人工介入

**当前倾向**：**选项 A（设置超时）**
- 补偿组件也支持 `timeoutMs` 配置
- 超时后记录失败，继续后续补偿
- 发送告警通知

**需要确认**：超时时间如何配置？（全局默认 vs 组件级别）

---

### Q5: 是否需要 Saga 版本管理？

**场景**：流程变更后，旧的补偿逻辑可能不适用

**选项**：
- **A**：记录流程版本，补偿时使用对应版本的组件
- **B**：始终使用最新版本的补偿组件
- **C**：不支持版本管理，通过变更流程避免

**当前倾向**：**选项 B（使用最新版本）**
- 简化实现
- 通过灰度发布降低风险
- 补偿组件需要保证向后兼容

**需要确认**：是否需要版本管理？

---

## Implementation Checklist

### Phase 1: 基础设施
- [ ] 创建数据库 migration（Flyway）
- [ ] 添加 Redisson 依赖
- [ ] 配置 Redis key 命名规范
- [ ] 创建包结构（domain, application, infrastructure, api）

### Phase 2: 领域层
- [ ] SagaExecution 聚合根
- [ ] StepExecution 实体
- [ ] SagaComponentMetadata 值对象
- [ ] FailureStrategy 值对象
- [ ] Repository 接口定义

### Phase 3: 基础设施层
- [ ] SagaExecutionRepositoryImpl
- [ ] SagaStateService（Redis + MySQL）
- [ ] SagaEventListener
- [ ] CompensationOrchestrator

### Phase 4: 应用层
- [ ] SagaExecutionService
- [ ] SagaManagementService
- [ ] DTO 和 VO 定义

### Phase 5: API 层
- [ ] SagaController
- [ ] 请求响应模型
- [ ] 权限控制

### Phase 6: XXL-JOB 集成
- [ ] 清理任务 Handler
- [ ] 任务注册和配置

### Phase 7: 测试
- [ ] 单元测试（Service 层）
- [ ] 集成测试（完整流程）
- [ ] 性能测试
- [ ] 补偿组件示例

### Phase 8: 文档和培训
- [ ] API 文档
- [ ] 开发指南
- [ ] 最佳实践
- [ ] 团队培训

---

## Appendix

### A. 示例：订单处理 Saga

**流程定义**：
```xml
<chain name="orderProcess">
    THEN(
        validateOrder,
        checkStock,
        createOrder,
        reserveStock,
        payment,
        confirmStock,
        sendNotification
    )
</chain>
```

**组件和补偿关系**：
| 组件 | 补偿组件 | 需要补偿 |
|------|---------|---------|
| validateOrder | - | ❌ |
| checkStock | - | ❌ |
| createOrder | cancelOrder | ✅ |
| reserveStock | releaseStock | ✅ |
| payment | refundPayment | ✅ |
| confirmStock | - | ❌ |
| sendNotification | - | ❌ |

**执行时间线**：
```
10:00:00  validateOrder ✓
10:00:01  checkStock ✓
10:00:02  createOrder ✓ (orderId: ORD-123)
10:00:03  reserveStock ✓ (reservationId: RES-456)
10:00:05  payment ✗ (INSUFFICIENT_FUNDS)
          ━━━━━━━━━━━━━━━━━━━━━━━━━━
          开始补偿（按相反顺序）
          ━━━━━━━━━━━━━━━━━━━━━━━━━━
10:00:06  releaseStock ✓ (释放 RES-456)
10:00:07  cancelOrder ✓ (取消 ORD-123)
10:00:08  Saga 状态: COMPENSATED
```

### B. Redis 数据结构示例

**saga:execution:1:exec-123**:
```json
{
  "executionId": "exec-123",
  "tenantId": 1,
  "chainName": "orderProcess",
  "status": "COMPENSATED",
  "currentStepIndex": 4,
  "startedAt": "2026-02-03T10:00:00",
  "completedAt": "2026-02-03T10:00:08",
  "executionStack": [
    {
      "stepId": "step-3",
      "component": "createOrder",
      "output": { "orderId": "ORD-123" },
      "compensatedAt": "2026-02-03T10:00:07"
    },
    {
      "stepId": "step-4",
      "component": "reserveStock",
      "output": { "reservationId": "RES-456" },
      "compensatedAt": "2026-02-03T10:00:06"
    }
  ]
}
```

### C. API 示例

**执行 Saga**：
```bash
curl -X POST "http://localhost:8080/api/saga/execute" \
  -H "Content-Type: application/json" \
  -d '{
    "tenantId": 1,
    "chainName": "orderProcess",
    "sagaMode": true,
    "async": true,
    "inputData": {
      "userId": 1001,
      "productId": 2001,
      "quantity": 2
    }
  }'

# Response
{
  "executionId": "exec-123",
  "status": "PENDING",
  "message": "Saga execution started"
}
```

**查询执行状态**：
```bash
curl -X GET "http://localhost:8080/api/saga/executions/exec-123"

# Response
{
  "executionId": "exec-123",
  "chainName": "orderProcess",
  "status": "COMPENSATED",
  "startedAt": "2026-02-03T10:00:00",
  "completedAt": "2026-02-03T10:00:08",
  "steps": [
    {
      "stepId": "step-1",
      "component": "validateOrder",
      "status": "COMPLETED"
    },
    ...
  ],
  "compensationLog": [
    {
      "stepId": "step-4",
      "component": "releaseStock",
      "status": "SUCCESS",
      "compensatedAt": "2026-02-03T10:00:06"
    },
    ...
  ]
}
```
