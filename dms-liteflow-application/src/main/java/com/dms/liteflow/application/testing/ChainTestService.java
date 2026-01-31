package com.dms.liteflow.application.testing;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import com.dms.liteflow.domain.flowexec.repository.FlowChainRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.entity.CmpStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 流程链测试服务
 * <p>
 * 提供流程链级别的测试功能
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChainTestService {

    private final FlowExecutor flowExecutor;
    private final FlowChainRepository flowChainRepository;

    /**
     * 测试流程链
     *
     * @param tenantId  租户ID
     * @param chainName 流程链名称
     * @param inputData 输入数据
     * @return 测试结果
     */
    public ChainTestResult testChain(Long tenantId, String chainName, Map<String, Object> inputData) {
        log.info("Testing chain: {} for tenant: {}", chainName, tenantId);

        ChainTestResult result = new ChainTestResult();
        result.setChainName(chainName);
        result.setInputData(inputData);

        try {
            // 验证流程链存在
            FlowChain chain = flowChainRepository.findByTenantIdAndName(
                    TenantId.of(tenantId),
                    chainName
            ).orElseThrow(() -> new IllegalArgumentException("Flow chain not found: " + chainName));

            // 执行流程链
            long startTime = System.currentTimeMillis();

            // TODO: 使用 LiteFlow 执行流程链
            // LFEntry entry = flowExecutor.execute2Resp(chainName, inputData);

            long endTime = System.currentTimeMillis();
            result.setExecuteTime(endTime - startTime);
            result.setSuccess(true);

        } catch (Exception e) {
            log.error("Chain test failed", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    /**
     * 测试流程链并返回执行路径
     *
     * @param tenantId  租户ID
     * @param chainName 流程链名称
     * @param inputData 输入数据
     * @return 带执行路径的测试结果
     */
    public ChainTestResult testChainWithExecutionPath(Long tenantId, String chainName, Map<String, Object> inputData) {
        log.info("Testing chain with execution path: {} for tenant: {}", chainName, tenantId);

        ChainTestResult result = testChain(tenantId, chainName, inputData);

        // TODO: 获取执行路径
        // List<CmpStep> steps = flowExecutor.execute2Resp(chainName, inputData).getExecuteSteps();
        // result.setExecutionSteps(steps);

        return result;
    }

    /**
     * 流程链测试结果
     */
    @lombok.Data
    public static class ChainTestResult {
        private String chainName;
        private Map<String, Object> inputData;
        private Object outputData;
        private long executeTime;
        private boolean success;
        private String errorMessage;
        private String message;
        private List<CmpStep> executionSteps;
        private String executionPath;
    }
}
