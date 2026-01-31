package com.dms.liteflow.infrastructure.liteflow.component.condition;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeBooleanComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * VIP用户判断组件
 * <p>
 * 条件组件 - 返回布尔值
 * </p>
 */
@Slf4j
@LiteflowComponent("isVIPUser")
public class IsVIPUserComponent extends NodeBooleanComponent {

    @Override
    public boolean processBoolean() throws Exception {
        Object requestData = this.getRequestData();
        log.info("Checking if user is VIP: {}", requestData);

        // TODO: 调用领域服务执行业务逻辑
        // 目前作为示例，返回 false
        boolean isVIP = false;

        log.info("User is VIP: {}", isVIP);
        return isVIP;
    }
}
