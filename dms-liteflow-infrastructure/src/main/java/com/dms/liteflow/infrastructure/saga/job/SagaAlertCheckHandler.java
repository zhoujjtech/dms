package com.dms.liteflow.infrastructure.saga.job;

import com.dms.liteflow.application.saga.monitoring.SagaAlertService;
import com.dms.liteflow.application.saga.monitoring.SagaAlertService.Alert;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Saga 告警检查 Handler
 * 定期检查 Saga 监控指标并触发告警
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaAlertCheckHandler {

    private final SagaAlertService alertService;

    /**
     * 检查并触发告警
     * 执行频率：每10分钟执行一次
     */
    @XxlJob("sagaAlertCheckHandler")
    public void checkAlerts() {
        log.info("Starting Saga alert check job...");

        try {
            List<Alert> alerts = alertService.checkAndTriggerAlerts();

            if (alerts.isEmpty()) {
                log.info("No alerts triggered");
                XxlJobHelper.handleSuccess("No alerts triggered");
            } else {
                log.info("Triggered {} alerts", alerts.size());

                // 构建告警摘要
                StringBuilder summary = new StringBuilder("Alerts triggered:\n");
                for (Alert alert : alerts) {
                    summary.append(String.format("- [%s] %s (severity: %s)\n",
                            alert.getType(), alert.getMessage(), alert.getSeverity()));
                }

                XxlJobHelper.handleSuccess(summary.toString());
            }

        } catch (Exception e) {
            log.error("Saga alert check job failed", e);
            XxlJobHelper.handleFail(e.getMessage());
        }
    }
}
