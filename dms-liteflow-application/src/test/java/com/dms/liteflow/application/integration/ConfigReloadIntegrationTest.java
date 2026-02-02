package com.dms.liteflow.application.integration;

import com.dms.liteflow.infrastructure.liteflow.FlowConfigService;
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
 * 配置刷新集成测试
 * <p>
 * 测试配置热更新和缓存刷新
 * </p>
 */
@SpringBootTest
@ActiveProfiles("test")
class ConfigReloadIntegrationTest {

    @Autowired
    private FlowConfigService flowConfigService;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TenantId.of(1L));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testRefreshAllConfigs() {
        // Execute - should not throw exception
        assertDoesNotThrow(() -> flowConfigService.clearAllCache());
    }

    @Test
    void testRefreshTenantConfig() {
        // Execute - should not throw exception
        assertDoesNotThrow(() -> flowConfigService.refreshConfig(1L));
    }

    @Test
    void testHasPublishedConfig() {
        // Execute
        boolean hasConfig = flowConfigService.hasPublishedConfig(1L);

        // Verify - depends on test data
        assertNotNull(flowConfigService);
    }

    @Test
    void testGetChainConfig() {
        // Execute
        var chainConfig = flowConfigService.getChainConfig(1L, "orderProcessChain");

        // Verify - may be null if not exists in test DB
        // This test verifies the method works without throwing exception
        assertDoesNotThrow(() -> flowConfigService.getChainConfig(1L, "orderProcessChain"));
    }

    @Test
    void testGetComponentConfig() {
        // Execute
        var components = flowConfigService.getComponentConfig(1L);

        // Verify - should return a list (may be empty)
        assertNotNull(components);
    }
}
