package com.dms.liteflow.infrastructure.liteflow.component.business;

import com.yomahub.liteflow.annotation.LiteflowComponent;
import com.yomahub.liteflow.core.NodeComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * 发送短信组件
 */
@Slf4j
@LiteflowComponent("sendSMS")
public class SendSMSComponent extends NodeComponent {

    @Override
    public void process() {
        log.info("Sending SMS notification");
        // TODO: 调用领域服务执行业务逻辑
    }
}
