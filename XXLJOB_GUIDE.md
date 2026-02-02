# XXL-JOB 分布式调度部署指南

## 概述

项目已从本地调度（Spring @Scheduled）迁移到分布式调度（XXL-JOB），支持多实例部署和任务集中管理。

## 前置条件

### 1. 部署 XXL-JOB 调度中心

XXL-JOB 需要独立部署调度中心（Admin）。

#### 方式一：Docker 部署（推荐）

```bash
# 1. 初始化数据库
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

# 2. 导入初始化SQL
# 下载 XXL-JOB 源码：https://github.com/xuxueli/xxl-job
# 执行 doc/db/tables_xxl_job.sql

# 3. 启动 XXL-JOB Admin
docker run -d \
  -p 8080:8080 \
  -e PARAMS="--spring.datasource.url=jdbc:mysql://host.docker.internal:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai \
  --spring.datasource.username=root \
  --spring.datasource.password=root" \
  --name xxl-job-admin \
  xuxueli/xxl-job-admin:2.4.0

# 访问：http://localhost:8080/xxl-job-admin
# 默认账号：admin / 123456
```

#### 方式二：源码部署

```bash
# 1. 下载源码
git clone https://github.com/xuxueli/xxl-job.git
cd xxl-job

# 2. 修改数据库配置（xxl-job-admin/src/main/resources/application.properties）

# 3. 编译打包
cd xxl-job-admin
mvn clean package

# 4. 运行
java -jar target/xxl-job-admin-2.4.0.jar
```

### 2. 初始化数据库

执行 XXL-JOB 初始化 SQL：
- 下载源码中的 `doc/db/tables_xxl_job.sql`
- 在 MySQL 中执行

## 配置说明

### application.yml 配置

```yaml
# XXL-JOB 分布式调度配置
xxl:
  job:
    admin:
      addresses: http://localhost:8080/xxl-job-admin  # 调度中心地址
    executor:
      appname: dms-liteflow  # 执行器应用名称
      address:  # 执行器地址（自动注册，留空即可）
      ip:  # 执行器IP（自动获取，留空即可）
      port: 9999  # 执行器端口
      logpath: ./xxl-job/logs  # 日志路径
      logretentiondays: 30  # 日志保留天数
    accessToken:  # 访问令牌（如果调度中心配置了）
```

### XXL-JOB 配置类

```java
@Configuration
public class XxlJobConfig {
    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {
        XxlJobSpringExecutor xxlJobSpringExecutor = new XxlJobSpringExecutor();
        xxlJobSpringExecutor.setAdminAddresses(adminAddresses);
        xxlJobSpringExecutor.setAppname(appname);
        xxlJobSpringExecutor.setPort(port);
        // ... 其他配置
        return xxlJobSpringExecutor;
    }
}
```

## 任务管理

### 1. 登录 XXL-JOB Admin

访问：`http://localhost:8080/xxl-job-admin`
- 默认账号：`admin`
- 默认密码：`123456`

### 2. 创建执行器

1. 进入 "执行器管理" -> "新增执行器"
2. 填写信息：
   - **AppName**: `dms-liteflow`（必须与 application.yml 中的 `appname` 一致）
   - **名称**: DMS LiteFlow 执行器
   - **注册方式**: 自动注册
   - **机器地址**: 留空（自动注册）

### 3. 配置任务

进入 "任务管理" -> "新增任务"，配置以下任务：

#### 任务1：配置刷新

| 配置项 | 值 |
|--------|-----|
| 任务描述 | 配置刷新任务 |
| 调度类型 | CRON |
| Cron | `0 */5 * * * ?`（每5分钟） |
| 运行模式 | BEAN |
| JobHandler | `configRefreshJob` |
| 任务阻塞策略 | 单机串行 |
| 失败重试次数 | 3 |

#### 任务2：规则刷新

| 配置项 | 值 |
|--------|-----|
| 任务描述 | 规则刷新任务 |
| 调度类型 | CRON |
| Cron | `*/60 * * * * ?`（每60秒） |
| 运行模式 | BEAN |
| JobHandler | `ruleRefreshJob` |
| 任务阻塞策略 | 单机串行 |
| 失败重试次数 | 3 |

#### 任务3：执行记录清理

| 配置项 | 值 |
|--------|-----|
| 任务描述 | 执行记录清理 |
| 调度类型 | CRON |
| Cron | `0 0 2 * * ?`（每天凌晨2点） |
| 运行模式 | BEAN |
| JobHandler | `cleanupExecutionJob` |
| 任务阻塞策略 | 单机串行 |
| 失败重试次数 | 3 |

#### 任务4：小时统计清理

| 配置项 | 值 |
|--------|-----|
| 任务描述 | 小时统计清理 |
| 调度类型 | CRON |
| Cron | `0 0 3 ? * SUN`（每周日凌晨3点） |
| 运行模式 | BEAN |
| JobHandler | `cleanupHourlyStatsJob` |
| 任务阻塞策略 | 单机串行 |
| 失败重试次数 | 3 |

#### 任务5：日统计清理

| 配置项 | 值 |
|--------|-----|
| 任务描述 | 日统计清理 |
| 调度类型 | CRON |
| Cron | `0 0 4 1 * ?`（每月1号凌晨4点） |
| 运行模式 | BEAN |
| JobHandler | `cleanupDailyStatsJob` |
| 任务阻塞策略 | 单机串行 |
| 失败重试次数 | 3 |

## 分布式特性

### 1. 集群部署

XXL-JOB 支持多实例部署，自动进行任务路由：

```
┌─────────────┐     ┌──────────────┐
│  Instance 1 │────▶│ XXL-JOB Admin│
└─────────────┘     └──────────────┘
                          ▲
┌─────────────┐            │
│  Instance 2 │────────────┘
└─────────────┘

┌─────────────┐
│  Instance 3 │────────────┘
└─────────────┘
```

### 2. 路由策略

| 策略 | 说明 |
|------|------|
| 第一个 | 固定使用第一个实例 |
| 最后一个 | 固定使用最后一个实例 |
| 轮询 | 轮流分配到各实例 |
| 随机 | 随机选择实例 |
| 一致性哈希 | 根据参数哈希选择 |
| 最不经常使用 | 选择使用次数最少的实例 |
| 故障转移 | 失败时自动切换到其他实例 |

### 3. 失败处理

- **失败重试**：可配置重试次数
- **失败告警**：支持邮件、钉钉、企业微信告警
- **故障转移**：实例宕机时自动切换

## 开发指南

### 添加新的 XXL-JOB 任务

1. **创建任务处理器**

```java
@Slf4j
@Component
@RequiredArgsConstructor
public class MyJobHandlers {

    @XxlJob("myNewJob")
    public void myNewJob() {
        log.info("XXL-JOB: My new job started");
        try {
            // 任务逻辑
            doSomething();
            log.info("XXL-JOB: My new job completed");
        } catch (Exception e) {
            log.error("XXL-JOB: My new job failed", e);
            throw e;  // 抛出异常触发失败告警
        }
    }
}
```

2. **在 XXL-JOB Admin 配置任务**

- JobHandler: `myNewJob`
- Cron: 根据需求配置
- 其他配置按需设置

3. **带参数的任务**

```java
@XxlJob("paramJob")
public void paramJob() {
    // 获取任务参数
    String param = XxlJobHelper.getJobParam();
    log.info("Job param: {}", param);

    // 获取任务ID
    int jobId = XxlJobHelper.getJobId();

    // 手动记录日志到 XXL-JOB
    XxlJobHelper.log("Custom log message");

    // 业务逻辑...
}
```

## 监控和管理

### 1. 执行日志

在 XXL-JOB Admin 中：
- 进入 "调度日志"
- 查看任务执行记录
- 查看执行日志（支持远程日志查看）

### 2. 执行器监控

- 进入 "执行器管理"
- 查看在线实例
- 查看实例负载

### 3. 任务管理

- 启动/停止任务
- 手动触发执行（支持传参）
- 查看执行历史

## Cron 表达式说明

### 格式

```
秒 分 时 日 月 周
```

### 示例

| 表达式 | 说明 |
|--------|------|
| `0 */5 * * * ?` | 每5分钟执行一次 |
| `*/60 * * * * ?` | 每60秒执行一次 |
| `0 0 2 * * ?` | 每天凌晨2点执行 |
| `0 0 3 ? * SUN` | 每周日凌晨3点执行 |
| `0 0 4 1 * ?` | 每月1号凌晨4点执行 |

### 在线生成器

- https://cron.qqe2.com/
- https://crontab.guru/

## 故障排查

### 问题1: 执行器未注册

**现象**：执行器管理中看不到实例

**解决方案**:
1. 检查应用是否启动
2. 检查 `xxl.job.admin.addresses` 配置是否正确
3. 检查网络连通性
4. 查看应用日志中的注册信息

### 问题2: 任务执行失败

**排查步骤**:
1. 查看 "调度日志" 中的失败原因
2. 查看 "执行日志" 获取详细错误信息
3. 检查应用日志
4. 手动触发任务测试

### 问题3: 端口冲突

**错误信息**:
```
Port 9999 is already in use
```

**解决方案**:
```yaml
xxl:
  job:
    executor:
      port: 9998  # 修改为其他端口
```

### 问题4: 数据库连接失败

**解决方案**:
1. 检查 XXL-JOB Admin 的数据库配置
2. 确保 MySQL 服务正常运行
3. 验证数据库连接信息

## 性能优化

### 1. 调整执行线程池

```yaml
xxl:
  job:
    executor:
      executorService:
        corePoolSize: 10  # 核心线程数
        maxPoolSize: 20   # 最大线程数
```

### 2. 任务分片

对于大数据量处理，使用任务分片：

```java
@XxlJob("shardingJob")
public void shardingJob() {
    // 获取分片参数
    int shardIndex = XxlJobHelper.getShardIndex();  // 分片索引
    int shardTotal = XxlJobHelper.getShardTotal();  // 分片总数

    // 只处理属于当前分片的数据
    processDataBySharding(shardIndex, shardTotal);
}
```

在 Admin 配置时：
- 路由策略：分片广播
- 每个实例会收到不同的分片参数

### 3. 任务超时控制

在任务配置中设置超时时间，超时后自动中断。

## 迁移步骤

### 从 Spring @Scheduled 迁移到 XXL-JOB

1. ✅ **部署 XXL-JOB Admin**（见前置条件）

2. ✅ **添加 XXL-JOB 依赖**（已完成）

3. ✅ **创建 XXL-JOB 配置类**（已完成）

4. ✅ **实现 JobHandler**（已完成）

5. ✅ **配置执行器和任务**（在 Admin 中配置）

6. ✅ **移除 @Scheduled 注解**（已完成）

7. ✅ **移除 @EnableScheduling**（已完成）

8. **验证任务执行**

```bash
# 启动应用
java -jar dms-liteflow-start.jar

# 在 Admin 中查看执行器是否注册成功

# 手动触发任务测试
```

## 回滚方案

如需回滚到 Spring @Scheduled：

### 1. 回滚 pom.xml

```xml
<!-- 移除 -->
<dependency>
    <groupId>com.xuxueli</groupId>
    <artifactId>xxl-job-core</artifactId>
</dependency>
```

### 2. 恢复 @Scheduled 注解

在原有的 Scheduler 类中恢复 `@Scheduled` 注解。

### 3. 恢复 @EnableScheduling

```java
@EnableScheduling
public class DmsLiteFlowApplication {
    // ...
}
```

### 4. 重启应用

```bash
mvn clean package
java -jar dms-liteflow-start.jar
```

## 相关文档

- [XXL-JOB 官方文档](https://www.xuxueli.com/xxl-job/)
- [XXL-JOB GitHub](https://github.com/xuxueli/xxl-job)
- [Spring Boot Scheduler 文档](https://docs.spring.io/spring-framework/reference/integration/scheduling.html)
