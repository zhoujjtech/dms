package com.dms.liteflow.infrastructure.liteflow.component.saga;

import com.dms.liteflow.domain.saga.valueobject.ActionType;
import com.dms.liteflow.infrastructure.liteflow.component.saga.annotation.SagaMetadata;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 预留库存组件
 * 扣减库存，为订单预留商品
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component("reserveStock")
@SagaMetadata(
        compensateComponent = "releaseStock",
        needsCompensation = true,
        defaultFailureStrategy = ActionType.AUTO_COMPENSATE,
        timeoutMs = 30000
)
public class ReserveStockComponent extends NodeComponent {

    @Override
    public void process() {
        String orderId = this.getContext().getData("createOrder.orderId");
        String productId = this.getContext().getData("productId");
        Integer quantity = this.getContext().getData("quantity");

        log.info("Reserving stock for order: {}, product: {}, quantity: {}",
                orderId, productId, quantity);

        try {
            // 模拟库存预留逻辑
            // 1. 检查库存是否充足
            // 2. 扣减库存
            // 3. 创建库存预留记录

            // 模拟可能的失败
            if (quantity != null && quantity > 1000) {
                throw new RuntimeException("Insufficient stock for product: " + productId);
            }

            // 模拟库存预留成功
            String reservationId = "RES-" + System.currentTimeMillis();

            Map<String, Object> result = new HashMap<>();
            result.put("reservationId", reservationId);
            result.put("productId", productId);
            result.put("reservedQuantity", quantity);
            result.put("status", "RESERVED");

            // 将结果保存到上下文
            this.getContext().setData("reserveStock.reservationId", reservationId);
            this.getContext().setData("reserveStock.result", result);

            log.info("Stock reserved successfully: reservationId={}", reservationId);

        } catch (Exception e) {
            log.error("Failed to reserve stock for order: {}", orderId, e);
            throw e;
        }
    }
}
