package com.dms.liteflow.application.component;

import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus;
import com.dms.liteflow.domain.shared.kernel.valueobject.ComponentType;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 规则组件应用服务
 * <p>
 * 应用层：编排业务逻辑，协调领域模型
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RuleComponentApplicationService {

    private final RuleComponentRepository ruleComponentRepository;

    /**
     * 创建规则组件
     */
    @Transactional
    public RuleComponent createComponent(
            Long tenantId,
            String componentId,
            String componentName,
            String description,
            String componentType,
            String content
    ) {
        log.info("Creating component: {} for tenant: {}", componentId, tenantId);

        // 构建领域对象
        RuleComponent component = RuleComponent.builder()
                .tenantId(TenantId.of(tenantId))
                .componentId(ComponentId.of(componentId))
                .componentName(componentName)
                .description(description)
                .componentType(ComponentType.fromCode(componentType))
                .content(content)
                .status(ComponentStatus.DRAFT)
                .build();

        // 保存
        return ruleComponentRepository.save(component);
    }

    /**
     * 发布组件
     */
    @Transactional
    public void publishComponent(Long tenantId, String componentId) {
        log.info("Publishing component: {} for tenant: {}", componentId, tenantId);

        RuleComponent component = ruleComponentRepository.findByComponentId(ComponentId.of(componentId))
                .orElseThrow(() -> new IllegalArgumentException("Component not found: " + componentId));

        // 验证租户
        if (!component.getTenantId().getValue().equals(tenantId)) {
            throw new IllegalArgumentException("Component does not belong to tenant: " + tenantId);
        }

        component.publish();
        ruleComponentRepository.save(component);
    }

    /**
     * 查询租户的所有组件
     */
    public List<RuleComponent> getComponentsByTenant(Long tenantId) {
        return ruleComponentRepository.findByTenantId(TenantId.of(tenantId));
    }

    /**
     * 查询已发布的组件
     */
    public List<RuleComponent> getPublishedComponents(Long tenantId) {
        return ruleComponentRepository.findByTenantIdAndStatus(
                TenantId.of(tenantId),
                ComponentStatus.PUBLISHED
        );
    }

    /**
     * 删除组件
     */
    @Transactional
    public void deleteComponent(String componentId) {
        log.info("Deleting component: {}", componentId);

        RuleComponent component = ruleComponentRepository.findByComponentId(ComponentId.of(componentId))
                .orElseThrow(() -> new IllegalArgumentException("Component not found: " + componentId));

        if (component.getStatus() == ComponentStatus.PUBLISHED) {
            throw new IllegalStateException("Cannot delete published component. Archive it first.");
        }

        ruleComponentRepository.deleteByComponentId(ComponentId.of(componentId));
    }
}
