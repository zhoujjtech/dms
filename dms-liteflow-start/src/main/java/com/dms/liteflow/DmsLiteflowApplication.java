package com.dms.liteflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * DMS LiteFlow 应用启动类
 * <p>
 * DDD + Spring Cloud Alibaba 架构
 * 使用 ElasticJob 实现分布式调度（替代 @Scheduled）
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.dms.liteflow")
@EnableDiscoveryClient
@EnableAsync
public class DmsLiteFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DmsLiteFlowApplication.class, args);
    }

}
