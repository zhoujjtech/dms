package com.dms.liteflow.infrastructure.liteflow.component.saga;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 取消订单组件（补偿组件）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@LiteflowComponent("cancelOrder")
@CompensationFor("createOrder")
public class CancelOrderComponent extends NodeComponent {

    @Override
    public void process() {
        // 从上下文获取原始订单数据
        String orderId = this.getContext().getData("createOrder.orderId");
        log.info("Cancelling order: {}", orderId);

        if (orderId == null || orderId.isEmpty()) {
            log.warn("No orderId found, skipping cancellation");
            return;
        }

        // 幂等性检查：如果订单已经取消，直接返回
        boolean isAlreadyCancelled = checkOrderCancelled(orderId);
        if (isAlreadyCancelled) {
            log.info("Order already cancelled: {}", orderId);
            return;
        }

        // 执行取消操作
        cancelOrderInDatabase(orderId);

        log.info("Order cancelled successfully: {}", orderId);
    }

    /**
     * 检查订单是否已取消（幂等性）
     */
    private boolean checkOrderCancelled(String orderId) {
        // TODO: 从数据库查询订单状态
        // 这里简化处理：假设未取消
        return false;
    }

    /**
     * 在数据库中取消订单
     */
    private void cancelOrderInDatabase(String orderId) {
        // TODO: 调用订单服务取消订单
        log.info("Order cancelled in database: {}", orderId);
    }
}
