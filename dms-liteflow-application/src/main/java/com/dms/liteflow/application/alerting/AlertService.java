package com.dms.liteflow.application.alerting;

import com.dms.liteflow.application.monitoring.MonitoringQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 告警服务
 * <p>
 * 提供失败率监控和告警发送功能
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final MonitoringQueryService monitoringQueryService;
    private final JavaMailSender mailSender;

    // 告警频率控制（防止告警轰炸）
    private final Map<String, Long> lastAlertTime = new ConcurrentHashMap<>();
    private static final long ALERT_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5);

    /**
     * 检查并发送告警
     *
     * @param tenantId    租户ID
     * @param chainId     流程链ID
     * @param failureRate 失败率阈值
     */
    public void checkAndSendFailureRateAlert(Long tenantId, Long chainId, double failureRate) {
        String alertKey = String.format("failure_%d_%d", tenantId, chainId);

        // 检查告警频率
        if (!shouldSendAlert(alertKey)) {
            log.debug("Alert skipped due to frequency control: {}", alertKey);
            return;
        }

        LocalDateTime endTime = LocalDateTime.now();
        LocalDateTime startTime = endTime.minusMinutes(15);

        double currentFailureRate = 100.0 - monitoringQueryService.calculateSuccessRate(
                tenantId,
                chainId,
                startTime,
                endTime
        );

        if (currentFailureRate >= failureRate) {
            log.warn("High failure rate detected for chain {}: {}%", chainId, currentFailureRate);
            sendFailureRateAlert(tenantId, chainId, currentFailureRate);
            lastAlertTime.put(alertKey, System.currentTimeMillis());
        }
    }

    /**
     * 发送邮件告警
     *
     * @param tenantId       租户ID
     * @param chainId        流程链ID
     * @param failureRate    失败率
     */
    public void sendFailureRateAlert(Long tenantId, Long chainId, double failureRate) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("admin@example.com"); // TODO: 从配置读取
            message.setSubject(String.format("[ALERT] High Failure Rate for Chain %d", chainId));
            message.setText(String.format(
                    "Alert: High failure rate detected\n" +
                            "Tenant ID: %d\n" +
                            "Chain ID: %d\n" +
                            "Failure Rate: %.2f%%\n" +
                            "Time: %s",
                    tenantId, chainId, failureRate, LocalDateTime.now()
            ));

            mailSender.send(message);
            log.info("Failure rate alert sent for chain: {}", chainId);
        } catch (Exception e) {
            log.error("Failed to send failure rate alert", e);
        }
    }

    /**
     * 发送错误告警
     *
     * @param tenantId     租户ID
     * @param errorMessage 错误信息
     */
    public void sendErrorAlert(Long tenantId, String errorMessage) {
        log.error("System error for tenant {}: {}", tenantId, errorMessage);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo("admin@example.com");
            message.setSubject(String.format("[ERROR] System Error for Tenant %d", tenantId));
            message.setText(String.format(
                    "System error detected\n" +
                            "Tenant ID: %d\n" +
                            "Error: %s\n" +
                            "Time: %s",
                    tenantId, errorMessage, LocalDateTime.now()
            ));

            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send error alert", e);
        }
    }

    /**
     * 检查是否应该发送告警（频率控制）
     */
    private boolean shouldSendAlert(String alertKey) {
        Long lastTime = lastAlertTime.get(alertKey);
        if (lastTime == null) {
            return true;
        }
        return System.currentTimeMillis() - lastTime >= ALERT_INTERVAL_MS;
    }

    /**
     * 清除告警频率记录
     */
    public void clearAlertFrequency(String alertKey) {
        lastAlertTime.remove(alertKey);
    }
}
