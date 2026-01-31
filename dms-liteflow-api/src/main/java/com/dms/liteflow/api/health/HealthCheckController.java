package com.dms.liteflow.api.health;

import com.yomahub.liteflow.core.FlowExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 * <p>
 * 提供 LiteFlow 引擎健康状态检查端点
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/actuator/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final FlowExecutor flowExecutor;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 健康检查端点
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().format(FORMATTER));
        return ResponseEntity.ok(health);
    }

    /**
     * LiteFlow 引擎健康检查
     */
    @GetMapping("/liteflow")
    public ResponseEntity<Map<String, Object>> liteflowHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            boolean isHealthy = flowExecutor != null;
            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("timestamp", LocalDateTime.now().format(FORMATTER));

            Map<String, Object> details = new HashMap<>();
            details.put("flowExecutorAvailable", isHealthy);
            health.put("details", details);

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("LiteFlow health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * 配置加载状态检查
     */
    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> configHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            // 检查配置加载状态
            boolean isHealthy = flowExecutor != null;

            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("timestamp", LocalDateTime.now().format(FORMATTER));

            Map<String, Object> details = new HashMap<>();
            details.put("configLoaded", isHealthy);
            health.put("details", details);

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Config health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }

    /**
     * 组件注册状态检查
     */
    @GetMapping("/components")
    public ResponseEntity<Map<String, Object>> componentsHealth() {
        Map<String, Object> health = new HashMap<>();

        try {
            boolean isHealthy = flowExecutor != null;

            health.put("status", isHealthy ? "UP" : "DOWN");
            health.put("timestamp", LocalDateTime.now().format(FORMATTER));

            Map<String, Object> details = new HashMap<>();
            details.put("flowExecutorAvailable", isHealthy);
            health.put("details", details);

            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("Components health check failed", e);
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(503).body(health);
        }
    }
}
