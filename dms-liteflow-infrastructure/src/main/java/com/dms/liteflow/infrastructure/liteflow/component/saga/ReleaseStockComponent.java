package com.dms.liteflow.infrastructure.liteflow.component.saga;

import com.dms.liteflow.infrastructure.liteflow.component.saga.annotation.CompensationFor;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 释放库存组件（补偿组件）
 * 释放之前预留的库存
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component("releaseStock")
@CompensationFor("reserveStock")
public class ReleaseStockComponent extends NodeComponent {

    @Override
    public void process() {
        // 获取原始步骤的输出数据
        Map<String, Object> originalOutput = this.getContext().getData("originalOutput");
        String reservationId = this.getContext().getData("reserveStock.reservationId");

        log.info("Releasing stock reservation: {}", reservationId);

        try {
            // 幂等性检查
            if (checkReservationReleased(reservationId)) {
                log.info("Stock reservation already released: {}", reservationId);
                return;
            }

            // 模拟库存释放逻辑
            // 1. 查询库存预留记录
            // 2. 恢复库存数量
            // 3. 标记预留记录为已释放

            releaseStockInDatabase(reservationId);

            log.info("Stock released successfully: reservationId={}", reservationId);

        } catch (Exception e) {
            log.error("Failed to release stock reservation: {}", reservationId, e);
            // 补偿失败不抛出异常，继续执行后续补偿
        }
    }

    /**
     * 检查预留是否已释放（幂等性检查）
     */
    private boolean checkReservationReleased(String reservationId) {
        if (reservationId == null) {
            return true;
        }
        // 实际实现中，应该查询数据库检查预留状态
        // 这里简化处理
        return false;
    }

    /**
     * 在数据库中释放库存
     */
    private void releaseStockInDatabase(String reservationId) {
        // 实际实现中，应该：
        // 1. UPDATE stock_reservation SET status = 'RELEASED' WHERE reservation_id = ?
        // 2. UPDATE product_stock SET available_quantity = available_quantity + reserved_quantity
        //    WHERE product_id IN (SELECT product_id FROM stock_reservation WHERE reservation_id = ?)

        log.debug("Executing: release stock for reservation: {}", reservationId);
    }
}
