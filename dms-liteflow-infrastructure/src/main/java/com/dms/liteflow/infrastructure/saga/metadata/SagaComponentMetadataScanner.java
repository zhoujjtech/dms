package com.dms.liteflow.infrastructure.saga.metadata;

import com.dms.liteflow.domain.saga.entity.SagaComponentMetadata;
import com.dms.liteflow.domain.saga.repository.SagaComponentMetadataRepository;
import com.dms.liteflow.domain.saga.valueobject.ActionType;
import com.dms.liteflow.domain.saga.valueobject.FailureRule;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.dms.liteflow.infrastructure.saga.annotation.CompensationFor;
import com.dms.liteflow.infrastructure.saga.annotation.SagaMetadata;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowFlow;
import com.yomahub.liteflow.flow.element.Node;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Saga 组件元数据扫描器
 * 在应用启动时扫描所有带 @SagaMetadata 和 @CompensationFor 注解的组件
 * 并将元数据加载到数据库
 *
 * @author DMS
 * @since 2026-02-03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaComponentMetadataScanner implements CommandLineRunner {

    private final FlowExecutor flowExecutor;
    private final SagaComponentMetadataRepository metadataRepository;

    @Override
    public void run(String... args) {
        log.info("Starting Saga component metadata scanning...");

        try {
            // 获取默认租户ID（系统租户）
            TenantId systemTenantId = TenantId.of(1L);
            TenantContext.setTenantId(systemTenantId);

            // 扫描并保存元数据
            int scannedCount = scanAndSaveMetadata(systemTenantId);

            log.info("Saga component metadata scanning completed. Total components scanned: {}", scannedCount);

        } catch (Exception e) {
            log.error("Failed to scan Saga component metadata", e);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * 扫描并保存组件元数据
     */
    private int scanAndSaveMetadata(TenantId tenantId) {
        int count = 0;

        // 获取所有流程
        Map<String, LiteflowFlow> flowMap = flowExecutor.getFlowMap();
        if (flowMap == null || flowMap.isEmpty()) {
            log.warn("No flows found in FlowExecutor");
            return 0;
        }

        // 遍历所有流程，收集组件
        for (Map.Entry<String, LiteflowFlow> entry : flowMap.entrySet()) {
            String chainName = entry.getKey();
            LiteflowFlow flow = entry.getValue();

            // 获取流程中的所有节点
            Map<String, Node> nodeMap = flow.getNodeMap();
            if (nodeMap == null || nodeMap.isEmpty()) {
                continue;
            }

            for (Map.Entry<String, Node> nodeEntry : nodeMap.entrySet()) {
                String componentName = nodeEntry.getKey();
                Node node = nodeEntry.getValue();

                // 获取节点实例
                Object nodeInstance = node.getInstance();
                if (nodeInstance == null) {
                    continue;
                }

                // 检查是否有 @SagaMetadata 注解
                SagaMetadata sagaMetadata = nodeInstance.getClass()
                        .getAnnotation(SagaMetadata.class);

                // 检查是否有 @CompensationFor 注解
                CompensationFor compensationFor = nodeInstance.getClass()
                        .getAnnotation(CompensationFor.class);

                if (sagaMetadata != null || compensationFor != null) {
                    // 构建并保存元数据
                    SagaComponentMetadata metadata = buildMetadata(
                            tenantId,
                            componentName,
                            sagaMetadata,
                            compensationFor
                    );

                    metadataRepository.save(metadata);
                    count++;

                    log.debug("Scanned component: {}, hasSagaMetadata: {}, hasCompensationFor: {}",
                            componentName, sagaMetadata != null, compensationFor != null);
                }
            }
        }

        return count;
    }

    /**
     * 构建组件元数据
     */
    private SagaComponentMetadata buildMetadata(
            TenantId tenantId,
            String componentName,
            SagaMetadata sagaMetadata,
            CompensationFor compensationFor) {

        SagaComponentMetadata.SagaComponentMetadataBuilder builder = SagaComponentMetadata.builder()
                .tenantId(tenantId)
                .componentName(componentName);

        if (sagaMetadata != null) {
            builder.compensateComponent(sagaMetadata.compensateComponent())
                    .needsCompensation(sagaMetadata.needsCompensation())
                    .defaultFailureStrategy(sagaMetadata.defaultFailureStrategy())
                    .timeoutMs(sagaMetadata.timeoutMs())
                    .failureRules(parseFailureRules(sagaMetadata.failureRules()));
        }

        if (compensationFor != null) {
            builder.isCompensationComponent(true)
                    .originalComponentName(compensationFor.value());
        } else {
            builder.isCompensationComponent(false);
        }

        return builder.build();
    }

    /**
     * 解析失败规则
     */
    private List<FailureRule> parseFailureRules(SagaMetadata.FailureRule[] rules) {
        if (rules == null || rules.length == 0) {
            return new ArrayList<>();
        }

        return List.of(rules).stream()
                .map(rule -> FailureRule.builder()
                        .condition(rule.condition())
                        .action(rule.action())
                        .retryCount(rule.retryCount())
                        .build())
                .collect(Collectors.toList());
    }
}
