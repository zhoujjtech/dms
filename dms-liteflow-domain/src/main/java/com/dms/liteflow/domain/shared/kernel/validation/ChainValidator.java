package com.dms.liteflow.domain.shared.kernel.validation;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.ruleconfig.aggregate.RuleComponent;
import com.dms.liteflow.domain.ruleconfig.repository.RuleComponentRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 流程链验证器
 * <p>
 * 验证流程链配置的正确性
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChainValidator {

    private final RuleComponentRepository ruleComponentRepository;

    // LiteFlow EL 表达式中的组件ID模式
    private static final Pattern COMPONENT_ID_PATTERN = Pattern.compile("\\b[a-zA-Z][a-zA-Z0-9_]*\\b");

    /**
     * 验证流程链配置
     *
     * @param chain 流程链
     * @return 验证结果
     */
    public ValidationResult validate(FlowChain chain) {
        log.debug("Validating flow chain: {}", chain.getChainName());

        ValidationResult result = ValidationResult.success();

        // 1. 验证 EL 表达式语法
        validateELExpression(chain, result);

        // 2. 验证组件存在性
        validateComponentExistence(chain, result);

        // 3. 检测循环依赖
        detectCircularDependencies(chain, result);

        return result;
    }

    /**
     * 验证 EL 表达式语法
     */
    private void validateELExpression(FlowChain chain, ValidationResult result) {
        String chainCode = chain.getChainCode();
        if (chainCode == null || chainCode.trim().isEmpty()) {
            result.addError("chainCode", "Chain code cannot be empty");
            return;
        }

        // 基本语法检查
        if (!hasBalancedBrackets(chainCode)) {
            result.addError("chainCode", "Unbalanced brackets in chain code");
        }

        // 检查非法字符
        if (hasInvalidCharacters(chainCode)) {
            result.addError("chainCode", "Invalid characters in chain code");
        }
    }

    /**
     * 验证组件存在性
     */
    private void validateComponentExistence(FlowChain chain, ValidationResult result) {
        Set<String> componentIds = extractComponentIds(chain.getChainCode());
        TenantId tenantId = chain.getTenantId();

        // 获取租户的所有已发布组件
        List<RuleComponent> publishedComponents = ruleComponentRepository.findByTenantIdAndStatus(
                tenantId,
                com.dms.liteflow.domain.shared.kernel.valueobject.ComponentStatus.PUBLISHED
        );

        Set<String> existingComponentIds = new HashSet<>();
        for (RuleComponent component : publishedComponents) {
            existingComponentIds.add(component.getComponentId().getValue());
        }

        // 检查每个组件是否存在
        for (String componentId : componentIds) {
            if (!existingComponentIds.contains(componentId)) {
                result.addError("chainCode",
                        String.format("Component '%s' does not exist or is not published", componentId));
            }
        }
    }

    /**
     * 检测循环依赖
     */
    private void detectCircularDependencies(FlowChain chain, ValidationResult result) {
        // TODO: 实现循环依赖检测
        // 这需要分析子流程引用关系，构建依赖图，然后检测环
        log.debug("Circular dependency detection not yet implemented for chain: {}", chain.getChainName());
    }

    /**
     * 提取 EL 表达式中的组件ID
     */
    private Set<String> extractComponentIds(String chainCode) {
        Set<String> componentIds = new HashSet<>();
        Matcher matcher = COMPONENT_ID_PATTERN.matcher(chainCode);

        while (matcher.find()) {
            String candidate = matcher.group();
            // 过滤掉 LiteFlow 关键字
            if (!isLiteFlowKeyword(candidate)) {
                componentIds.add(candidate);
            }
        }

        return componentIds;
    }

    /**
     * 检查是否是 LiteFlow 关键字
     */
    private boolean isLiteFlowKeyword(String word) {
        Set<String> keywords = Set.of(
                "THEN", "WHEN", "IF", "ELSE", "SWITCH", "CASE",
                "DEFAULT", "FOR", "WHILE", "BREAK", "PRE", "FINALLY",
                "AND", "OR", "NOT", "node", "any"
        );
        return keywords.contains(word.toUpperCase());
    }

    /**
     * 检查括号是否平衡
     */
    private boolean hasBalancedBrackets(String str) {
        Stack<Character> stack = new Stack<>();

        for (char c : str.toCharArray()) {
            if (c == '(' || c == '{' || c == '[') {
                stack.push(c);
            } else if (c == ')' || c == '}' || c == ']') {
                if (stack.isEmpty()) {
                    return false;
                }
                char top = stack.pop();
                if (!isMatchingPair(top, c)) {
                    return false;
                }
            }
        }

        return stack.isEmpty();
    }

    private boolean isMatchingPair(char opening, char closing) {
        return (opening == '(' && closing == ')') ||
                (opening == '{' && closing == '}') ||
                (opening == '[' && closing == ']');
    }

    /**
     * 检查非法字符
     */
    private boolean hasInvalidCharacters(String chainCode) {
        // 允许字母、数字、下划线、空格和特殊符号
        // 这里做基本检查，具体规则可根据实际需求调整
        return false;
    }
}
