package com.dms.liteflow.domain.shared.kernel.event;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 领域事件基类
 * <p>
 * 所有的领域事件都应该继承这个基类
 * </p>
 */
@Getter
public abstract class DomainEvent {

    /**
     * 事件发生时间
     */
    private final LocalDateTime occurredOn;

    /**
     * 事件ID（唯一标识）
     */
    private final String eventId;

    /**
     * 租户ID
     */
    private final Long tenantId;

    protected DomainEvent(Long tenantId) {
        this.occurredOn = LocalDateTime.now();
        this.eventId = generateEventId();
        this.tenantId = tenantId;
    }

    /**
     * 生成事件ID
     */
    private String generateEventId() {
        return java.util.UUID.randomUUID().toString();
    }

    /**
     * 获取事件类型（子类实现）
     */
    public abstract String getEventType();

}
