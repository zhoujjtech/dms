# Redis 分布式缓存部署指南

## 概述

项目已从本地缓存（Caffeine）迁移到分布式缓存（Redis），以支持多实例部署和缓存共享。

## 前置条件

### 1. 安装 Redis 服务

**Windows (开发环境)**:
```powershell
# 使用 Chocolatey 安装
choco install redis-64

# 或手动下载安装
# https://github.com/microsoftarchive/redis/releases
```

**Linux (生产环境)**:
```bash
# Ubuntu/Debian
sudo apt-get install redis-server

# CentOS/RHEL
sudo yum install redis

# 源码编译
# wget http://download.redis.io/releases/redis-7.2.0.tar.gz
```

**Docker**:
```bash
docker run -d -p 6379:6379 --name redis redis:7-alpine
```

### 2. 验证 Redis 服务

```bash
# 启动 Redis 服务
redis-server

# 测试连接
redis-cli ping
# 应返回 PONG

# 查看运行状态
redis-cli info
```

## 配置说明

### application.yml 配置

```yaml
spring:
  data:
    redis:
      host: localhost          # Redis 服务器地址
      port: 6379              # Redis 端口
      password:               # 密码（可选）
      database: 0               # 数据库索引
      timeout: 5000ms          # 连接超时
      lettuce:
        pool:
          max-active: 8        # 最大连接数
          max-idle: 8          # 最大空闲连接数
          min-idle: 0          # 最小空闲连接数
          max-wait: -1ms        # 最大等待时间
      cache:
        type: redis
        redis:
          time-to-live: 300000       # 缓存过期时间（5分钟）
          cache-null-values: false  # 不缓存 null 值
          key-prefix: "dms:cache:"    # 缓存 key 前缀
          use-key-prefix: true
```

### 缓存配置类

```java
@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // 使用 Redis 分布式缓存
        // JSON 序列化支持复杂对象
        // 5分钟 TTL
        // 支持事务
    }
}
```

## 缓存 Key 格式

Redis 中的 key 格式：

```
dms:cache:flowConfig::1
dms:cache:componentConfig::1
dms_cache:chainConfig::2
```

**格式说明**:
- `dms:cache:` - 统一前缀
- `flowConfig` - 缓存名称
- `::1` - 分隔符 + 缓存 key 参数

## 缓存注解使用

### Service 层缓存

```java
@Service
public class FlowConfigService {

    @Cacheable(value = "flowConfig", key = "#tenantId")
    public String getFlowChainConfig(Long tenantId) {
        // 从数据库加载
        return flowConfigLoader.loadFlowChainConfig(tenantId);
    }

    @CacheEvict(value = "flowConfig", key = "#tenantId")
    public void refreshConfig(Long tenantId) {
        // 缓存清除，下次查询重新加载
    }

    @CacheEvict(value = "flowConfig", allEntries = true)
    public void clearAllCache() {
        // 清除所有缓存
    }
}
```

### 手动清除缓存

```bash
# 通过 API 接口
curl -X POST http://localhost:8080/api/admin/config/refresh

# 通过 redis-cli
redis-cli
keys "dms:cache:*"
del "dms:cache:flowConfig::1"

# 清除所有缓存
redis-cli FLUSHDB
```

## 生产环境建议

### 1. Redis 高可用配置

#### 主从复制
```bash
# 主服务器（写入）
redis-server --port 6379

# 从服务器（读取）
redis-server --port 6380 --slaveof localhost 6379
```

#### Redis Sentinel（高可用）
```bash
# Sentinel 1
redis-sentinel monitor mymaster localhost 6379 2

# Sentinel 2
redis-sentinel monitor mymaster localhost 6379 2

# Sentinel 3
redis-sentinel monitor mymaster localhost 6379 2
```

#### Redis Cluster（集群）
```bash
# 集群配置
redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 ...
```

### 2. 性能优化

```yaml
spring:
  data:
    redis:
      lettuce:
        pool:
          max-active: 16    # 根据并发量调整
          max-idle: 8
          min-idle: 2
```

### 3. 监控指标

```bash
# 查看 Redis 状态
redis-cli info

# 查看内存使用
redis-cli info memory

# 查看连接数
redis-cli info clients

# 查看缓存命中情况
redis-cli info stats
```

## 故障排查

### 问题1: 无法连接 Redis

**错误信息**:
```
io.lettuce.core.RedisConnectionException: Unable to connect
```

**解决方案**:
1. 检查 Redis 服务是否启动：`redis-cli ping`
2. 检查防火墙是否开放 6379 端口
3. 检查配置文件中的 host 和 port 是否正确

### 问题2: 序列化错误

**错误信息**:
```
org.springframework.data.redis.serializer.SerializationException
```

**解决方案**:
1. 确保缓存的对象实现了 `Serializable`
2. 使用 `@JsonIgnore` 忽略不需要序列化的字段
3. 对于复杂对象，考虑使用缓存抽象（DTO）

### 问题3: 缓存击穿

**解决方案**:
```java
@Cacheable(value = "flowConfig", key = "#tenantId", unless = "#result == null")
public String getFlowChainConfig(Long tenantId) {
    // ...
}
```

或使用 `@Cacheable` + `@CachePut` 组合。

### 问题4: 内存占用过高

**解决方案**:
```yaml
spring:
  data:
    redis:
      cache:
        redis:
          time-to-live: 180000  # 缩短 TTL 到 3 分钟
```

## 迁移步骤

### 从 Caffeine 迁移到 Redis

1. **安装 Redis 服务**（见前置条件）

2. **更新配置文件**（已完成）

3. **启动应用**

4. **验证缓存工作**
   ```bash
   # 调用 API 查询数据
   curl http://localhost:8080/api/flow-chains/1

   # 查看 Redis 中的缓存
   redis-cli keys "dms:cache:*"
   redis-cli get "dms:cache:flowConfig::1"
   ```

5. **清理旧的 Caffeine 配置**（自动完成）

## 回滚方案

如果 Redis 不可用，可以快速回滚到 Caffeine：

### 1. 回滚 pom.xml

```xml
<!-- 移除 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- 恢复 -->
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
</dependency>
```

### 2. 恢复 CacheConfig.java

使用 Caffeine 配置替换当前的 Redis 配置。

### 3. 重启应用

## 相关文档

- [Redis 官方文档](https://redis.io/docs)
- [Spring Data Redis 文档](https://docs.spring.io/spring-data/data-redis/docs/current/reference/html/)
- [Spring Boot Redis 集成指南](https://spring.io/guides/gs/spring-data-redis)
