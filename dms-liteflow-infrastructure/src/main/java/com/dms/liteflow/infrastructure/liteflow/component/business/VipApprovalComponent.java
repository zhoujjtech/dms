package com.dms.liteflow.infrastructure.liteflow.component.business;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * VIP审批组件
 */
@Slf4j
@LiteflowComponent("vipApproval")
public class VipApprovalComponent extends NodeComponent {

    @Override
    public void process() {
        log.info("Processing VIP approval");
        // TODO: 调用领域服务执行业务逻辑
    }
}
