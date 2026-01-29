package com.dms.liteflow.model.vo;

import lombok.Data;
import java.util.HashMap;
import java.util.Map;

/**
 * 流程上下文数据对象
 * 用于在组件之间传递数据
 */
@Data
public class ContextBean {

    /**
     * 上下文数据存储
     */
    private Map<String, Object> dataMap = new HashMap<>();

    /**
     * 设置数据
     *
     * @param key   键
     * @param value 值
     */
    public void setData(String key, Object value) {
        dataMap.put(key, value);
    }

    /**
     * 获取数据
     *
     * @param key 键
     * @return 值
     */
    public Object getData(String key) {
        return dataMap.get(key);
    }

    /**
     * 获取数据（带类型转换）
     *
     * @param key  键
     * @param clazz 目标类型
     * @param <T>  泛型类型
     * @return 值
     */
    public <T> T getData(String key, Class<T> clazz) {
        Object value = dataMap.get(key);
        if (value == null) {
            return null;
        }
        return clazz.cast(value);
    }

    /**
     * 移除数据
     *
     * @param key 键
     * @return 被移除的值
     */
    public Object removeData(String key) {
        return dataMap.remove(key);
    }

    /**
     * 清空所有数据
     */
    public void clear() {
        dataMap.clear();
    }

    /**
     * 检查是否包含指定键
     *
     * @param key 键
     * @return 是否包含
     */
    public boolean containsKey(String key) {
        return dataMap.containsKey(key);
    }
}
