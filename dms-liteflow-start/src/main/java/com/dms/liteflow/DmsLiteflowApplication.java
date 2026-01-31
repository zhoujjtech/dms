package com.dms.liteflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DMS LiteFlow 应用启动类
 * <p>
 * DDD + Spring Cloud Alibaba 架构
 * </p>
 */
@SpringBootApplication(scanBasePackages = "com.dms.liteflow")
@EnableDiscoveryClient
@EnableScheduling
public class DmsLiteFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(DmsLiteFlowApplication.class, args);
    }

}
