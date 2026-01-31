# DMS LiteFlow 部署文档

## 部署架构

```
┌─────────────────────────────────────────────────────────────┐
│                       负载均衡器                              │
└────────────────────┬────────────────────────────────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼──────┐ ┌──▼────────┐ ┌─▼──────────┐
│   Instance 1  │ │Instance 2  │ │ Instance 3 │
│  dms-liteflow │ │dms-liteflow│ │dms-liteflow│
└───────┬──────┘ └──┬────────┘ └─┬──────────┘
        │            │            │
        └────────────┼────────────┘
                     │
        ┌────────────┼────────────┐
        │            │            │
┌───────▼──────┐ ┌──▼────────┐ ┌─▼──────────┐
│    Nacos     │ │   MySQL   │ │   Redis    │
│  (注册中心)  │ │ (主数据库) │ │  (缓存)    │
└──────────────┘ └───────────┘ └────────────┘
```

## 环境准备

### 1. 基础设施

#### MySQL 8.0

```bash
# 安装 MySQL
docker run -d \
  --name mysql \
  -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=root \
  -e MYSQL_DATABASE=dms_liteflow \
  mysql:8.0

# 导入数据库
docker cp schema.sql mysql:/tmp/
docker exec mysql mysql -uroot -proot dms_liteflow < /tmp/schema.sql
```

#### Nacos 2.2

```bash
# 下载 Nacos
wget https://github.com/alibaba/nacos/releases/download/2.2.0/nacos-server-2.2.0.zip

# 启动 Nacos
unzip nacos-server-2.2.0.zip
cd nacos/bin
./startup.sh -m standalone

# 访问控制台
open http://localhost:8848/nacos
# 默认账号密码: nacos/nacos
```

### 2. 配置中心配置

在 Nacos 中创建配置：

**Data ID**: `dms-liteflow.yml`
**Group**: `DEFAULT_GROUP`

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/dms_liteflow?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver

liteflow:
  rule-source: configData
  configData: classpath:flow/
  print-banner: false

logging:
  level:
    com.dms.liteflow: INFO
    org.springframework: WARN
```

## 应用部署

### 方式一：直接运行

```bash
# 编译打包
mvn clean package -DskipTests

# 运行应用
cd dms-liteflow-start
java -Xms512m -Xmx1024m \
  -Dspring.profiles.active=prod \
  -jar target/dms-liteflow-start-1.0.0-SNAPSHOT.jar
```

### 方式二：Docker 容器

```dockerfile
FROM openjdk:17-jdk-slim

LABEL maintainer="dms-liteflow"

WORKDIR /app

COPY target/dms-liteflow-start-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
# 构建镜像
docker build -t dms-liteflow:1.0.0 .

# 运行容器
docker run -d \
  --name dms-liteflow \
  -p 8080:8080 \
  -e SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR=nacos:8848 \
  -e SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR=nacos:8848 \
  dms-liteflow:1.0.0
```

### 方式三：Kubernetes

**deployment.yaml**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: dms-liteflow
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: dms-liteflow
  template:
    metadata:
      labels:
        app: dms-liteflow
    spec:
      containers:
      - name: dms-liteflow
        image: dms-liteflow:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: "prod"
        - name: SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR
          value: "nacos:8848"
        - name: SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR
          value: "nacos:8848"
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"
          limits:
            memory: "1024Mi"
            cpu: "1000m"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
---
apiVersion: v1
kind: Service
metadata:
  name: dms-liteflow
  namespace: default
spec:
  selector:
    app: dms-liteflow
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: LoadBalancer
```

```bash
# 部署到 Kubernetes
kubectl apply -f k8s/deployment.yaml

# 查看部署状态
kubectl get pods -l app=dms-liteflow

# 查看日志
kubectl logs -f deployment/dms-liteflow
```

## 监控配置

### Actuator 端点

应用启动后，可通过以下端点进行监控：

- `/actuator/health` - 健康检查
- `/actuator/info` - 应用信息
- `/actuator/metrics` - 监控指标

### Prometheus 监控

在 `pom.xml` 中已添加 Prometheus 依赖：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

在 `application.yml` 中配置：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true
```

### 日志配置

在 `logback-spring.xml` 中配置日志输出：

```xml
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/dms-liteflow.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/dms-liteflow.%d{yyyy-MM-dd}.log</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
        <appender-ref ref="FILE" />
    </root>
</configuration>
```

## 性能优化

### JVM 参数优化

```bash
java -server \
  -Xms1024m -Xmx1024m \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/logs/heapdump.hprof \
  -Dspring.profiles.active=prod \
  -jar dms-liteflow-start.jar
```

### 数据库连接池配置

```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 5
      maximum-pool-size: 20
      idle-timeout: 30000
      max-lifetime: 1800000
      connection-timeout: 30000
      connection-test-query: SELECT 1
```

## 故障排查

### 常见问题

1. **应用启动失败**
   - 检查 Nacos 连接配置
   - 检查数据库连接配置
   - 查看日志文件

2. **配置更新不生效**
   - 检查缓存是否清除
   - 查看定时任务是否执行
   - 手动刷新配置

3. **性能问题**
   - 检查数据库连接池配置
   - 查看 JVM 内存使用
   - 分析慢查询日志

### 日志查看

```bash
# 查看实时日志
tail -f logs/dms-liteflow.log

# 查看错误日志
grep ERROR logs/dms-liteflow.log

# 查看指定时间日志
sed -n '/2024-01-01 10:00/,/2024-01-01 11:00/p' logs/dms-liteflow.log
```

## 备份恢复

### 数据库备份

```bash
# 备份数据库
mysqldump -uroot -proot dms_liteflow > backup_$(date +%Y%m%d).sql

# 恢复数据库
mysql -uroot -proot dms_liteflow < backup_20240101.sql
```

### 配置备份

定期备份 Nacos 配置：

```bash
# 导出所有配置
curl -X GET "http://nacos:8848/nacos/v1/cs/configs?export=true&dataId=&group=&tenant=" \
  -o nacos_config_$(date +%Y%m%d).zip
```

## 升级指南

### 滚动升级

```bash
# 1. 备份当前版本
kubectl get deployment dms-liteflow -o yaml > dms-liteflow-backup.yaml

# 2. 更新镜像
kubectl set image deployment/dms-liteflow \
  dms-liteflow=dms-liteflow:1.0.1

# 3. 监控升级状态
kubectl rollout status deployment/dms-liteflow

# 4. 如有问题，快速回滚
kubectl rollout undo deployment/dms-liteflow
```

## 安全加固

### 1. 网络安全

- 使用 HTTPS 协议
- 配置防火墙规则
- 限制数据库访问

### 2. 应用安全

- 启用 Spring Security
- 配置 CORS 白名单
- 实施请求限流

### 3. 数据安全

- 定期备份数据
- 加密敏感配置
- 实施审计日志
