package com.dms.liteflow.infrastructure.liteflow.component.saga;

import com.dms.liteflow.domain.saga.valueobject.SagaMetadata;
import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

/**
 * 创建订单组件（Saga 示例）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@LiteflowComponent("createOrder")
@SagaMetadata(
    compensateComponent = "cancelOrder",
    needsCompensation = true,
    defaultFailureStrategy = SagaMetadata.ActionType.AUTO_COMPENSATE
)
public class CreateOrderComponent extends NodeComponent {

    @Override
    public void process() {
        Object requestData = this.getRequestData();
        log.info("Creating order: {}", requestData);

        // 模拟创建订单
        String orderId = "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);

        // 将订单ID保存到上下文，供补偿组件使用
        this.getContext().setData("orderId", orderId);
        this.getContext().setData("outputData", orderId);

        // 模拟偶尔失败
        if (Math.random() < 0.1) {  // 10% 失败率
            throw new RuntimeException("Order creation failed: Database error");
        }

        log.info("Order created successfully: {}", orderId);
    }
}
