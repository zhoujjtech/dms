package com.dms.liteflow.infrastructure.liteflow.component.business;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 创建订单组件
 */
@Slf4j
@LiteflowComponent("createOrder")
public class CreateOrderComponent extends NodeComponent {

    @Override
    public void process() {
        Object requestData = this.getRequestData();
        log.info("Creating order: {}", requestData);

        // TODO: 调用领域服务执行业务逻辑

        String orderId = "ORD-" + System.currentTimeMillis();
        log.info("Order created successfully: {}", orderId);
    }
}
