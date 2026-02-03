package com.dms.liteflow.infrastructure.liteflow.component.saga;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 退款组件（支付补偿组件）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@LiteflowComponent("refundPayment")
@CompensationFor("payment")
public class RefundPaymentComponent extends NodeComponent {

    @Override
    public void process() {
        // 从上下文获取原始支付数据
        String paymentId = this.getContext().getData("payment.paymentId");
        log.info("Refunding payment: {}", paymentId);

        if (paymentId == null || paymentId.isEmpty()) {
            log.warn("No paymentId found, skipping refund");
            return;
        }

        // 幂等性检查
        boolean isAlreadyRefunded = checkPaymentRefunded(paymentId);
        if (isAlreadyRefunded) {
            log.info("Payment already refunded: {}", paymentId);
            return;
        }

        // 执行退款
        refundPayment(paymentId);

        log.info("Payment refunded successfully: {}", paymentId);
    }

    private boolean checkPaymentRefunded(String paymentId) {
        // TODO: 查询支付状态
        return false;
    }

    private void refundPayment(String paymentId) {
        // TODO: 调用支付服务退款
        log.info("Refunded in payment system: {}", paymentId);
    }
}
