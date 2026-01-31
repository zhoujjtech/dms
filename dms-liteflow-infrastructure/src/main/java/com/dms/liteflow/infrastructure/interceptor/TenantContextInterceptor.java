package com.dms.liteflow.infrastructure.interceptor;

import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 租户上下文拦截器
 * <p>
 * 从请求中提取租户ID并设置到上下文中
 * </p>
 */
@Slf4j
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT_PARAM = "tenantId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从 header 或参数中获取租户ID
        String tenantIdStr = request.getHeader(TENANT_HEADER);
        if (tenantIdStr == null) {
            tenantIdStr = request.getParameter(TENANT_PARAM);
        }

        if (tenantIdStr != null) {
            try {
                Long tenantId = Long.parseLong(tenantIdStr);
                TenantContext.setTenantId(TenantId.of(tenantId));
                log.debug("Set tenant context: {}", tenantId);
            } catch (NumberFormatException e) {
                log.warn("Invalid tenant ID: {}", tenantIdStr);
            }
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 清除租户上下文
        TenantContext.clear();
    }
}
