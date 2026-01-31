# DMS LiteFlow 组件开发指南

## 1. 组件概述

LiteFlow 组件是流程编排的基本执行单元，每个组件负责一个具体的业务逻辑。

## 2. 组件类型

### 2.1 普通组件 (COMMON)
最常用的组件类型，执行具体业务逻辑。

```java
@LiteflowComponent("validateOrder")
public class ValidateOrder extends NodeComponent {
    @Override
    public void process() {
        // 获取上下文数据
        OrderContext context = this.getContextBean(OrderContext.class);

        // 业务逻辑处理
        boolean isValid = validate(context.getOrder());

        // 设置结果到上下文
        context.setValid(isValid);
    }
}
```

### 2.2 选择组件 (SELECT)
基于条件选择执行路径。

```java
@LiteflowComponent("selectPaymentMethod")
public class SelectPaymentMethod extends NodeSwitchComponent {
    @Override
    public String processSwitch() throws Exception {
        OrderContext context = this.getContextBean(OrderContext.class);

        if (context.getOrder().getAmount() > 1000) {
            return "creditCard"; // 跳转到 creditCard 节点
        } else {
            return "cash"; // 跳转到 cash 节点
        }
    }
}
```

### 2.3 条件组件 (IF)
根据条件判断是否执行。

```java
@LiteflowComponent("checkStock")
public class CheckStock extends NodeIfComponent {
    @Override
    public boolean processIf() throws Exception {
        OrderContext context = this.getContextBean(OrderContext.class);
        return context.getOrder().getQuantity() <= getStock();
    }
}
```

### 2.4 循环组件 (WHILE)
基于条件循环执行。

```java
@LiteflowComponent("processBatch")
public class ProcessBatch extends NodeWhileComponent {
    @Override
    public boolean processWhile() throws Exception {
        ProcessContext context = this.getContextBean(ProcessContext.class);
        return context.hasMoreItems();
    }
}
```

### 2.5 退出组件 (BREAK)
退出循环。

```java
@LiteflowComponent("checkExit")
public class CheckExit extends NodeBreakComponent {
    @Override
    public boolean processBreak() throws Exception {
        ProcessContext context = this.getContextBean(ProcessContext.class);
        return context.shouldExit();
    }
}
```

## 3. 上下文使用

### 3.1 获取上下文数据

```java
// 获取 DefaultContext
DefaultContext context = this.getFirstContextBean();

// 获取自定义上下文对象
OrderContext orderContext = this.getContextBean(OrderContext.class);

// 获取单个数据
String orderId = this.getContextBean("orderId", String.class);
```

### 3.2 设置上下文数据

```java
// 设置单个数据
this.getContextBean(OrderContext.class).setResult(result);

// 或使用 DefaultContext
DefaultContext context = this.getFirstContextBean();
context.setData("key", "value");
```

## 4. 组件开发最佳实践

### 4.1 单一职责
每个组件只负责一个明确的业务功能。

### 4.2 幂等性
组件应该是幂等的，多次执行结果一致。

### 4.3 异常处理
```java
@Override
public void process() {
    try {
        // 业务逻辑
    } catch (BusinessException e) {
        // 业务异常，设置错误信息
        this.getContextBean(OrderContext.class).setErrorMessage(e.getMessage());
        throw e;
    }
}
```

### 4.4 日志记录
```java
@Slf4j
@LiteflowComponent("myComponent")
public class MyComponent extends NodeComponent {
    @Override
    public void process() {
        log.info("Executing component with context: {}", this.getContextBean(Context.class));
        // ...
    }
}
```

## 5. 组件测试

### 5.1 单元测试
```java
@SpringBootTest
public class MyComponentTest {
    @Autowired
    private ComponentTestService componentTestService;

    @Test
    public void testComponent() {
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("orderId", "12345");

        ComponentTestService.TestResult result =
            componentTestService.testComponent(1L, "myComponent", inputData);

        assertTrue(result.isSuccess());
    }
}
```

### 5.2 通过 API 测试
```bash
curl -X POST "http://localhost:8080/api/testing/components/validateOrder" \
  -H "Content-Type: application/json" \
  -d '{"tenantId": 1, "inputData": {"orderId": "12345"}}'
```

## 6. 组件注册

### 6.1 自动注册
使用 `@LiteflowComponent` 注解的组件会自动注册到 LiteFlow 引擎。

```java
@LiteflowComponent("componentId")
public class MyComponent extends NodeComponent {
    // ...
}
```

### 6.2 动态注册
通过 API 动态创建和注册组件：

```bash
curl -X POST "http://localhost:8080/api/components" \
  -d "tenantId=1&componentId=dynamicComponent&componentName=动态组件&componentType=COMMON&content=..."
```

## 7. 组件间通信

### 7.1 通过上下文传递数据
```java
// 组件A设置数据
@LiteflowComponent("componentA")
public class ComponentA extends NodeComponent {
    @Override
    public void process() {
        DefaultContext context = this.getFirstContextBean();
        context.setData("resultFromA", "data");
    }
}

// 组件B获取数据
@LiteflowComponent("componentB")
public class ComponentB extends NodeComponent {
    @Override
    public void process() {
        DefaultContext context = this.getFirstContextBean();
        String data = context.getData("resultFromA", String.class);
    }
}
```

### 7.2 使用自定义对象
```java
// 定义上下文对象
@Data
public class OrderContext {
    private Order order;
    private boolean valid;
    private String errorMessage;
}

// 在组件中使用
@LiteflowComponent("validateOrder")
public class ValidateOrder extends NodeComponent {
    @Override
    public void process() {
        OrderContext context = this.getContextBean(OrderContext.class);
        context.setValid(true);
    }
}
```

## 8. 组件生命周期

1. **加载**: 组件类被扫描并注册到 LiteFlow 引擎
2. **初始化**: Spring 容器创建组件实例
3. **执行**: 流程链执行时调用组件的 process() 方法
4. **销毁**: Spring 容器关闭时销毁组件实例

## 9. 常见问题

### Q: 如何在组件中获取租户信息？
A: 使用 TenantContext:
```java
TenantId tenantId = TenantContext.getTenantId();
```

### Q: 组件执行失败如何处理？
A: 抛出异常会中断流程链执行，可在全局异常处理器中捕获。

### Q: 如何实现组件的异步执行？
A: 使用 `@Async` 注解或 CompletableFuture。

## 10. 示例代码

完整示例参考: `dms-liteflow-infrastructure/src/main/java/com/dms/liteflow/infrastructure/liteflow/component/`
