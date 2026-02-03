package com.dms.liteflow.infrastructure.liteflow.component.saga;

import com.dms.liteflow.domain.saga.valueobject.ActionType;
import com.dms.liteflow.infrastructure.liteflow.component.saga.annotation.FailureRule;
import com.dms.liteflow.infrastructure.liteflow.component.saga.annotation.SagaMetadata;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 订单验证组件
 * 验证订单信息的有效性
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component("validateOrder")
@SagaMetadata(
        needsCompensation = false, // 验证不需要补偿
        defaultFailureStrategy = ActionType.MANUAL, // 验证失败需要人工处理
        timeoutMs = 10000,
        failureRules = {
                @FailureRule(
                        condition = "INVALID_PRODUCT",
                        action = ActionType.MANUAL,
                        retryCount = 0
                ),
                @FailureRule(
                        condition = "INSUFFICIENT_STOCK",
                        action = ActionType.AUTO_COMPENSATE,
                        retryCount = 0
                )
        }
)
public class ValidateOrderComponent extends NodeComponent {

    @Override
    public void process() {
        String productId = this.getContext().getData("productId");
        BigDecimal amount = this.getContext().getData("amount");
        String customerId = this.getContext().getData("customerId");

        log.info("Validating order: product={}, amount={}, customer={}",
                productId, amount, customerId);

        try {
            // 验证规则
            validateProduct(productId);
            validateAmount(amount);
            validateCustomer(customerId);

            // 验证通过，设置标志
            this.getContext().setData("validateOrder.valid", true);
            this.getContext().setData("validateOrder.validatedAt", System.currentTimeMillis());

            log.info("Order validation passed");

        } catch (Exception e) {
            log.error("Order validation failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * 验证商品
     */
    private void validateProduct(String productId) {
        if (productId == null || productId.isEmpty()) {
            throw new RuntimeException("INVALID_PRODUCT: Product ID is required");
        }

        // 模拟查询商品是否存在
        if ("INVALID_001".equals(productId)) {
            throw new RuntimeException("INVALID_PRODUCT: Product not found: " + productId);
        }

        log.debug("Product validation passed: {}", productId);
    }

    /**
     * 验证金额
     */
    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("INVALID_AMOUNT: Amount must be greater than 0");
        }

        if (amount.compareTo(new BigDecimal("1000000")) > 0) {
            throw new RuntimeException("INVALID_AMOUNT: Amount exceeds maximum limit");
        }

        log.debug("Amount validation passed: {}", amount);
    }

    /**
     * 验证客户
     */
    private void validateCustomer(String customerId) {
        if (customerId == null || customerId.isEmpty()) {
            throw new RuntimeException("INVALID_CUSTOMER: Customer ID is required");
        }

        log.debug("Customer validation passed: {}", customerId);
    }
}
