package com.dms.liteflow.infrastructure.liteflow.component.saga;

import com.dms.liteflow.domain.saga.valueobject.SagaMetadata;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 支付组件（Saga 示例）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@LiteflowComponent("payment")
@SagaMetadata(
    compensateComponent = "refundPayment",
    needsCompensation = true,
    defaultFailureStrategy = SagaMetadata.ActionType.AUTO_COMPENSATE,
    timeoutMs = 10000
)
public class PaymentComponent extends NodeComponent {

    @Override
    public void process() {
        Object requestData = this.getRequestData();
        log.info("Processing payment: {}", requestData);

        // 模拟支付处理
        String paymentId = "PAY-" + System.currentTimeMillis();
        this.getContext().setData("paymentId", paymentId);
        this.getContext().setData("outputData", paymentId);

        // 模拟不同类型的失败
        double random = Math.random();
        if (random < 0.15) {  // 15% 失败率
            if (random < 0.05) {
                throw new RuntimeException("INSUFFICIENT_FUNDS");
            } else if (random < 0.10) {
                throw new RuntimeException("PAYMENT_TIMEOUT");
            } else {
                throw new RuntimeException("RISK_CHECK_FAILED");
            }
        }

        log.info("Payment processed successfully: {}", paymentId);
    }
}
