package com.dms.liteflow.api.monitoring;

import com.dms.liteflow.application.monitoring.MonitoringCollectorService;
import com.dms.liteflow.application.monitoring.MonitoringQueryService;
import com.dms.liteflow.domain.monitoring.aggregate.ExecutionRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 监控控制器
 * <p>
 * 提供监控数据查询和管理的API端点
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MonitoringQueryService monitoringQueryService;
    private final MonitoringCollectorService monitoringCollectorService;

    /**
     * 查询执行记录
     * GET /api/monitoring/executions/{executionId}
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<List<ExecutionRecord>> getExecutionRecords(
            @PathVariable String executionId
    ) {
        log.info("GET /api/monitoring/executions/{}", executionId);

        List<ExecutionRecord> records = monitoringQueryService.getExecutionRecords(executionId);

        return ResponseEntity.ok(records);
    }

    /**
     * 查询流程链的执行记录
     * GET /api/monitoring/chains/{chainId}/executions
     */
    @GetMapping("/chains/{chainId}/executions")
    public ResponseEntity<List<ExecutionRecord>> getChainExecutionRecords(
            @PathVariable Long chainId,
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("GET /api/monitoring/chains/{}/executions - tenantId: {}", chainId, tenantId);

        List<ExecutionRecord> records = monitoringQueryService.getChainExecutionRecords(
                tenantId, chainId, startTime, endTime
        );

        return ResponseEntity.ok(records);
    }

    /**
     * 查询租户的执行记录
     * GET /api/monitoring/tenant/executions
     */
    @GetMapping("/tenant/executions")
    public ResponseEntity<List<ExecutionRecord>> getTenantExecutionRecords(
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("GET /api/monitoring/tenant/executions - tenantId: {}", tenantId);

        List<ExecutionRecord> records = monitoringQueryService.getTenantExecutionRecords(
                tenantId, startTime, endTime
        );

        return ResponseEntity.ok(records);
    }

    /**
     * 获取执行统计信息
     * GET /api/monitoring/chains/{chainId}/stats
     */
    @GetMapping("/chains/{chainId}/stats")
    public ResponseEntity<MonitoringQueryService.ExecutionStats> getExecutionStats(
            @PathVariable Long chainId,
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("GET /api/monitoring/chains/{}/stats - tenantId: {}", chainId, tenantId);

        MonitoringQueryService.ExecutionStats stats = monitoringQueryService.getExecutionStats(
                tenantId, chainId, startTime, endTime
        );

        return ResponseEntity.ok(stats);
    }

    /**
     * 计算成功率
     * GET /api/monitoring/chains/{chainId}/success-rate
     */
    @GetMapping("/chains/{chainId}/success-rate")
    public ResponseEntity<Double> getSuccessRate(
            @PathVariable Long chainId,
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("GET /api/monitoring/chains/{}/success-rate - tenantId: {}", chainId, tenantId);

        double successRate = monitoringQueryService.calculateSuccessRate(
                tenantId, chainId, startTime, endTime
        );

        return ResponseEntity.ok(successRate);
    }

    /**
     * 计算平均执行时间
     * GET /api/monitoring/chains/{chainId}/avg-time
     */
    @GetMapping("/chains/{chainId}/avg-time")
    public ResponseEntity<Double> getAverageExecuteTime(
            @PathVariable Long chainId,
            @RequestParam Long tenantId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("GET /api/monitoring/chains/{}/avg-time - tenantId: {}", chainId, tenantId);

        double avgTime = monitoringQueryService.calculateAverageExecuteTime(
                tenantId, chainId, startTime, endTime
        );

        return ResponseEntity.ok(avgTime);
    }

    /**
     * 删除过期记录
     * DELETE /api/monitoring/expired
     */
    @DeleteMapping("/expired")
    public ResponseEntity<Integer> deleteExpiredRecords(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeTime
    ) {
        log.info("DELETE /api/monitoring/expired - beforeTime: {}", beforeTime);

        int deletedCount = monitoringQueryService.deleteExpiredRecords(beforeTime);

        return ResponseEntity.ok(deletedCount);
    }
}
