package com.dms.liteflow.infrastructure.liteflow.component.saga;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

import java.util.UUID;

/**
 * 库存检查组件（只读，无需补偿）
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@LiteflowComponent("checkStock")
@SagaMetadata(
    needsCompensation = false
)
public class CheckStockComponent extends NodeComponent {

    @Override
    public void process() {
        Object requestData = this.getRequestData();
        log.info("Checking stock: {}", requestData);

        // 模拟库存检查
        boolean inStock = true;  // 简化处理

        this.getContext().setData("stockAvailable", inStock);

        if (!inStock) {
            throw new RuntimeException("OUT_OF_STOCK");
        }

        log.info("Stock check completed: inStock={}", inStock);
    }
}
