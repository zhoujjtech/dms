package com.dms.liteflow.infrastructure.liteflow.component.business;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 库存检查组件
 */
@Slf4j
@LiteflowComponent("checkStock")
public class CheckStockComponent extends NodeComponent {

    @Override
    public void process() {
        Object requestData = this.getRequestData();
        log.info("Checking stock for order: {}", requestData);

        // TODO: 调用领域服务执行业务逻辑

        log.info("Stock check completed");
    }
}
