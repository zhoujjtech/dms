package com.dms.liteflow.component.base;

import com.yomahub.liteflow.core.NodeComponent;
import com.dms.liteflow.model.vo.ContextBean;
import lombok.extern.slf4j.Slf4j;

/**
 * 规则组件基础抽象类
 * 所有业务规则组件应继承此类
 */
@Slf4j
public abstract class BaseComponent extends NodeComponent {

    /**
     * 获取流程上下文数据
     *
     * @return 上下文数据对象
     */
    protected ContextBean getContextBean() {
        return this.getContextBean(ContextBean.class);
    }

    /**
     * 向上下文设置数据
     *
     * @param key   键
     * @param value 值
     */
    protected void setContextData(String key, Object value) {
        ContextBean context = getContextBean();
        context.setData(key, value);
    }

    /**
     * 从上下文获取数据
     *
     * @param key 键
     * @return 值
     */
    protected Object getContextData(String key) {
        ContextBean context = getContextBean();
        return context.getData(key);
    }

    /**
     * 组件执行前置处理
     */
    @Override
    public void beforeProcess() {
        log.info("Component [{}] started execution", this.getComponentId());
    }

    /**
     * 组件执行后置处理
     */
    @Override
    public void afterProcess() {
        log.info("Component [{}] completed execution", this.getComponentId());
    }

    /**
     * 异常处理
     *
     * @param e 异常对象
     */
    @Override
    public void onError(Exception e) {
        log.error("Component [{}] execution error: {}", this.getComponentId(), e.getMessage(), e);
    }
}
