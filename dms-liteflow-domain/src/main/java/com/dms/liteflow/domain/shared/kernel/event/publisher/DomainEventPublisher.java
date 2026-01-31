package com.dms.liteflow.domain.shared.kernel.event.publisher;

import com.dms.liteflow.domain.shared.kernel.event.DomainEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 领域事件发布器
 * <p>
 * 负责发布领域事件到 Spring 事件总线
 * </p>
 */
@Slf4j
@Component
public class DomainEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public DomainEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * 发布领域事件
     *
     * @param event 领域事件
     */
    public void publish(DomainEvent event) {
        try {
            log.debug("Publishing domain event: {}", event.getEventType());
            applicationEventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.error("Failed to publish domain event: {}", event.getEventType(), e);
            throw new DomainEventPublishException("Failed to publish domain event: " + event.getEventType(), e);
        }
    }

    /**
     * 批量发布领域事件
     *
     * @param events 领域事件列表
     */
    public void publishBatch(java.util.List<DomainEvent> events) {
        events.forEach(this::publish);
    }

    /**
     * 领域事件发布异常
     */
    public static class DomainEventPublishException extends RuntimeException {

        public DomainEventPublishException(String message) {
            super(message);
        }

        public DomainEventPublishException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
