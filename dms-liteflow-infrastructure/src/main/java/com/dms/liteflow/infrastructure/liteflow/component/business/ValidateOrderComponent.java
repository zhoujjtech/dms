package com.dms.liteflow.infrastructure.liteflow.component.business;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 订单验证组件
 * <p>
 * 基础设施层 - LiteFlow 组件实现
 * </p>
 */
@Slf4j
@LiteflowComponent("validateOrder")
public class ValidateOrderComponent extends NodeComponent {

    @Override
    public void process() {
        // 获取请求数据
        Object requestData = this.getRequestData();
        log.info("Validating order: {}", requestData);

        // TODO: 调用领域服务执行业务逻辑
        // 目前作为示例，简单验证
        if (requestData == null) {
            throw new IllegalArgumentException("Order data cannot be null");
        }

        log.info("Order validation completed");
    }
}
