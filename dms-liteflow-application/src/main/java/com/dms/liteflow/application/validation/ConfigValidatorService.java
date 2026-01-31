package com.dms.liteflow.application.validation;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.flowexec.repository.FlowChainRepository;
import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.validation.ChainValidator;
import com.dms.liteflow.domain.shared.kernel.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 配置验证服务
 * <p>
 * 提供配置验证功能
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigValidatorService {

    private final ChainValidator chainValidator;
    private final FlowChainRepository flowChainRepository;
    private final RuleComponentRepository ruleComponentRepository;

    /**
     * 验证流程链配置
     *
     * @param tenantId 租户ID
     * @param chainId  流程链ID
     * @return 验证结果
     */
    public ValidationResult validateChain(Long tenantId, Long chainId) {
        log.info("Validating chain: {} for tenant: {}", chainId, tenantId);

        FlowChain chain = flowChainRepository.findById(
                com.dms.liteflow.domain.shared.kernel.valueobject.ChainId.of(chainId)
        ).orElse(null);

        if (chain == null) {
            ValidationResult result = ValidationResult.failure();
            result.addError("chainId", "Flow chain not found: " + chainId);
            return result;
        }

        if (!chain.getTenantId().getValue().equals(tenantId)) {
            ValidationResult result = ValidationResult.failure();
            result.addError("tenantId", "Chain does not belong to tenant: " + tenantId);
            return result;
        }

        return chainValidator.validate(chain);
    }

    /**
     * 验证组件配置
     *
     * @param tenantId    租户ID
     * @param componentId 组件ID
     * @return 验证结果
     */
    public ValidationResult validateComponent(Long tenantId, String componentId) {
        log.info("Validating component: {} for tenant: {}", componentId, tenantId);

        ValidationResult result = ValidationResult.success();

        RuleComponent component = ruleComponentRepository.findByComponentId(
                com.dms.liteflow.domain.shared.kernel.valueobject.ComponentId.of(componentId)
        ).orElse(null);

        if (component == null) {
            result.addError("componentId", "Component not found: " + componentId);
            return result;
        }

        if (!component.getTenantId().getValue().equals(tenantId)) {
            result.addError("tenantId", "Component does not belong to tenant: " + tenantId);
            return result;
        }

        // 验证组件内容
        if (component.getContent() == null || component.getContent().trim().isEmpty()) {
            result.addError("content", "Component content cannot be empty");
        }

        return result;
    }

    /**
     * 验证租户的所有配置
     *
     * @param tenantId 租户ID
     * @return 验证结果
     */
    public ValidationResult validateTenantConfig(Long tenantId) {
        log.info("Validating all configurations for tenant: {}", tenantId);

        ValidationResult overallResult = ValidationResult.success();

        // 验证所有组件
        List<RuleComponent> components = ruleComponentRepository.findByTenantId(
                com.dms.liteflow.domain.shared.kernel.valueobject.TenantId.of(tenantId)
        );

        for (RuleComponent component : components) {
            ValidationResult componentResult = validateComponent(tenantId, component.getComponentId().getValue());
            overallResult.merge(componentResult);
        }

        // 验证所有流程链
        List<FlowChain> chains = flowChainRepository.findByTenantId(
                com.dms.liteflow.domain.shared.kernel.valueobject.TenantId.of(tenantId)
        );

        for (FlowChain chain : chains) {
            ValidationResult chainResult = chainValidator.validate(chain);
            overallResult.merge(chainResult);
        }

        return overallResult;
    }

    /**
     * 批量验证流程链
     *
     * @param tenantId 租户ID
     * @return 验证结果
     */
    public ValidationResult validateAllChains(Long tenantId) {
        log.info("Validating all chains for tenant: {}", tenantId);

        ValidationResult overallResult = ValidationResult.success();

        List<FlowChain> chains = flowChainRepository.findByTenantId(
                com.dms.liteflow.domain.shared.kernel.valueobject.TenantId.of(tenantId)
        );

        for (FlowChain chain : chains) {
            ValidationResult chainResult = chainValidator.validate(chain);
            overallResult.merge(chainResult);
        }

        return overallResult;
    }
}
