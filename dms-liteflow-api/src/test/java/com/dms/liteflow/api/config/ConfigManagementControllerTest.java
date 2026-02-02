package com.dms.liteflow.api.config;

import com.dms.liteflow.infrastructure.liteflow.FlowConfigService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ConfigManagementController 单元测试
 */
@WebMvcTest(ConfigManagementController.class)
class ConfigManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FlowConfigService flowConfigService;

    @Test
    void testRefreshAllConfigs_Success() throws Exception {
        // Setup
        doNothing().when(flowConfigService).clearAllCache();

        // Execute & Verify
        mockMvc.perform(post("/api/admin/config/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());

        verify(flowConfigService, times(1)).clearAllCache();
    }

    @Test
    void testRefreshAllConfigs_Failure() throws Exception {
        // Setup
        doThrow(new RuntimeException("Database error")).when(flowConfigService).clearAllCache();

        // Execute & Verify
        mockMvc.perform(post("/api/admin/config/refresh")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false));

        verify(flowConfigService, times(1)).clearAllCache();
    }

    @Test
    void testRefreshTenantConfig_Success() throws Exception {
        // Setup
        when(flowConfigService.hasPublishedConfig(1L)).thenReturn(true);
        doNothing().when(flowConfigService).refreshConfig(1L);

        // Execute & Verify
        mockMvc.perform(post("/api/admin/config/refresh/1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.tenantId").value(1));

        verify(flowConfigService, times(1)).hasPublishedConfig(1L);
        verify(flowConfigService, times(1)).refreshConfig(1L);
    }

    @Test
    void testRefreshTenantConfig_NotFound() throws Exception {
        // Setup
        when(flowConfigService.hasPublishedConfig(999L)).thenReturn(false);

        // Execute & Verify
        mockMvc.perform(post("/api/admin/config/refresh/999")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));

        verify(flowConfigService, times(1)).hasPublishedConfig(999L);
        verify(flowConfigService, never()).refreshConfig(anyLong());
    }

    @Test
    void testGetConfigStatus_WithoutTenant() throws Exception {
        // Execute & Verify
        mockMvc.perform(get("/api/admin/config/status")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.configSource").value("database"))
                .andExpect(jsonPath("$.configType").value("dynamic"))
                .andExpect(jsonPath("$.autoRefreshEnabled").value(true));
    }

    @Test
    void testGetConfigStatus_WithTenant() throws Exception {
        // Setup
        when(flowConfigService.hasPublishedConfig(1L)).thenReturn(true);

        // Execute & Verify
        mockMvc.perform(get("/api/admin/config/status?tenantId=1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(1))
                .andExpect(jsonPath("$.hasPublishedConfig").value(true));

        verify(flowConfigService, times(1)).hasPublishedConfig(1L);
    }
}
