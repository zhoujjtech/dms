package com.dms.liteflow.infrastructure.liteflow.config;

import com.dms.liteflow.infrastructure.interceptor.TenantContext;
import com.dms.liteflow.infrastructure.persistence.entity.FlowChainEntity;
import com.dms.liteflow.infrastructure.persistence.entity.RuleComponentEntity;
import com.dms.liteflow.infrastructure.persistence.mapper.FlowChainMapper;
import com.dms.liteflow.infrastructure.persistence.mapper.RuleComponentMapper;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * CustomJdbcRuleSource 单元测试
 */
@ExtendWith(MockitoExtension.class)
class CustomJdbcRuleSourceTest {

    @Mock
    private FlowChainMapper flowChainMapper;

    @Mock
    private RuleComponentMapper ruleComponentMapper;

    private CustomJdbcRuleSource customJdbcRuleSource;

    @BeforeEach
    void setUp() {
        customJdbcRuleSource = new CustomJdbcRuleSource(
                flowChainMapper,
                ruleComponentMapper,
                "dms-liteflow"
        );
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void testGetChainConfigs_WithValidTenant() {
        // Setup
        TenantContext.setTenantId(TenantId.of(1L));

        FlowChainEntity entity = new FlowChainEntity();
        entity.setChainName("testChain");
        entity.setChainCode("THEN(a, b, c)");
        entity.setNamespace("default");

        when(flowChainMapper.selectPublishedChainsForLiteFlow(any(), any()))
                .thenReturn(List.of(entity));

        // Execute
        List<CustomJdbcRuleSource.ChainConfig> configs = customJdbcRuleSource.getChainConfigs();

        // Verify
        assertNotNull(configs);
        assertEquals(1, configs.size());
        assertEquals("testChain", configs.get(0).getChainName());
        assertEquals("THEN(a, b, c)", configs.get(0).getChainCode());

        verifyMock();
    }

    @Test
    void testGetChainConfigs_WithNullTenant() {
        // Setup - no tenant context
        FlowChainEntity entity = new FlowChainEntity();
        entity.setChainName("testChain");
        entity.setChainCode("THEN(a, b, c)");

        when(flowChainMapper.selectPublishedChainsForLiteFlow(any(), any()))
                .thenReturn(List.of(entity));

        // Execute
        List<CustomJdbcRuleSource.ChainConfig> configs = customJdbcRuleSource.getChainConfigs();

        // Verify - should use default tenant ID 1
        assertNotNull(configs);
        assertEquals(1, configs.size());

        verifyMock();
    }

    @Test
    void testGetScriptConfigs() {
        // Setup
        TenantContext.setTenantId(TenantId.of(1L));

        RuleComponentEntity entity = new RuleComponentEntity();
        entity.setComponentId("script1");
        entity.setContent("return true;");
        entity.setComponentType("if_script");
        entity.setLanguage("groovy");

        when(ruleComponentMapper.selectPublishedComponentsForLiteFlow(any(), any()))
                .thenReturn(List.of(entity));

        // Execute
        List<CustomJdbcRuleSource.ScriptConfig> configs = customJdbcRuleSource.getScriptConfigs();

        // Verify
        assertNotNull(configs);
        assertEquals(1, configs.size());
        assertEquals("script1", configs.get(0).getScriptId());
        assertEquals("return true;", configs.get(0).getScriptData());
        assertEquals("if_script", configs.get(0).getScriptType());
        assertEquals("groovy", configs.get(0).getLanguage());

        verifyMock();
    }

    @Test
    void testGetChainConfigs_WithException() {
        // Setup
        TenantContext.setTenantId(TenantId.of(1L));
        when(flowChainMapper.selectPublishedChainsForLiteFlow(any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // Execute - should not throw exception
        List<CustomJdbcRuleSource.ChainConfig> configs = customJdbcRuleSource.getChainConfigs();

        // Verify
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }

    @Test
    void testGetScriptConfigs_WithException() {
        // Setup
        TenantContext.setTenantId(TenantId.of(1L));
        when(ruleComponentMapper.selectPublishedComponentsForLiteFlow(any(), any()))
                .thenThrow(new RuntimeException("Database error"));

        // Execute - should not throw exception
        List<CustomJdbcRuleSource.ScriptConfig> configs = customJdbcRuleSource.getScriptConfigs();

        // Verify
        assertNotNull(configs);
        assertTrue(configs.isEmpty());
    }

    private void verifyMock() {
        // Additional verification can be added here if needed
    }
}
