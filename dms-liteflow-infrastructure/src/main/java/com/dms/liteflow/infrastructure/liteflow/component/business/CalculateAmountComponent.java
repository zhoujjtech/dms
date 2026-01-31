package com.dms.liteflow.infrastructure.liteflow.component.business;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 金额计算组件
 */
@Slf4j
@LiteflowComponent("calculateAmount")
public class CalculateAmountComponent extends NodeComponent {

    @Override
    public void process() {
        Object requestData = this.getRequestData();
        log.info("Calculating amount for order: {}", requestData);

        // TODO: 调用领域服务执行业务逻辑

        log.info("Amount calculation completed");
    }
}
