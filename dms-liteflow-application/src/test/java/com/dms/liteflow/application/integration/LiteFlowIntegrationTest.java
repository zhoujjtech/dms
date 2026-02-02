package com.dms.liteflow.application.integration;

import com.dms.liteflow.domain.dto.ExecutionRequestDTO;
import com.dms.liteflow.domain.vo.ExecutionResponseVO;
import com.dms.liteflow.application.execution.ExecutionService;
import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LiteFlow 集成测试
 * <p>
 * 测试数据库配置加载和流程执行
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class LiteFlowIntegrationTest {

    @Autowired
    private ExecutionService executionService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantId.of(1L));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testExecuteFlow_FromDatabaseConfig() {
        // Setup
        ExecutionRequestDTO request = new ExecutionRequestDTO();
        request.setTenantId(1L);
        request.setChainName("orderProcessChain");
        request.setTimeoutMs(30000);
        request.setInputData("{\"orderId\": \"12345\"}");

        // Execute
        ExecutionResponseVO response = executionService.executeSync(request);

        // Verify
        assertNotNull(response);
        assertEquals("COMPLETED", response.getStatus());
        assertNotNull(response.getExecutionId());
    }

    @Test
    void testMultiTenantConfigIsolation() {
        // Setup - Tenant 1
        TenantContext.setTenantId(TenantId.of(1L));
        ExecutionRequestDTO request1 = new ExecutionRequestDTO();
        request1.setTenantId(1L);
        request1.setChainName("orderProcessChain");
        request1.setInputData("{}");

        // Execute Tenant 1
        ExecutionResponseVO response1 = executionService.executeSync(request1);
        assertNotNull(response1);

        // Setup - Tenant 2
        TenantContext.setTenantId(TenantId.of(2L));
        ExecutionRequestDTO request2 = new ExecutionRequestDTO();
        request2.setTenantId(2L);
        request2.setChainName("orderProcessChain");
        request2.setInputData("{}");

        // Execute Tenant 2
        ExecutionResponseVO response2 = executionService.executeSync(request2);
        assertNotNull(response2);

        // Verify - different execution IDs
        assertNotEquals(response1.getExecutionId(), response2.getExecutionId());
    }
}
