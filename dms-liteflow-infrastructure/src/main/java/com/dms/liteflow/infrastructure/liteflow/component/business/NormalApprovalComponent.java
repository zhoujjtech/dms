package com.dms.liteflow.infrastructure.liteflow.component.business;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 普通审批组件
 */
@Slf4j
@LiteflowComponent("normalApproval")
public class NormalApprovalComponent extends NodeComponent {

    @Override
    public void process() {
        log.info("Processing normal approval");
        // TODO: 调用领域服务执行业务逻辑
    }
}
