package com.dms.liteflow.application.tenant;

import com.dms.liteflow.domain.tenant.aggregate.Tenant;
import com.dms.liteflow.domain.tenant.repository.TenantRepository;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 租户应用服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TenantApplicationService {

    private final TenantRepository tenantRepository;

    /**
     * 创建租户
     */
    @Transactional
    public Tenant createTenant(
            String tenantCode,
            String tenantName,
            Integer maxChains,
            Integer maxComponents
    ) {
        log.info("Creating tenant: {}", tenantCode);

        // 检查租户代码是否已存在
        if (tenantRepository.existsByTenantCode(tenantCode)) {
            throw new IllegalArgumentException("Tenant code already exists: " + tenantCode);
        }

        Tenant tenant = Tenant.builder()
                .tenantCode(tenantCode)
                .tenantName(tenantName)
                .status("ACTIVE")
                .maxChains(maxChains != null ? maxChains : 100)
                .maxComponents(maxComponents != null ? maxComponents : 1000)
                .executorCached(true)
                .build();

        return tenantRepository.save(tenant);
    }

    /**
     * 更新租户
     */
    @Transactional
    public Tenant updateTenant(
            Long id,
            String tenantName,
            Integer maxChains,
            Integer maxComponents
    ) {
        log.info("Updating tenant: {}", id);

        Tenant tenant = tenantRepository.findById(TenantId.of(id))
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        if (tenantName != null) {
            tenant.setTenantName(tenantName);
        }
        if (maxChains != null) {
            tenant.setMaxChains(maxChains);
        }
        if (maxComponents != null) {
            tenant.setMaxComponents(maxComponents);
        }

        return tenantRepository.save(tenant);
    }

    /**
     * 激活租户
     */
    @Transactional
    public void activateTenant(Long id) {
        log.info("Activating tenant: {}", id);

        Tenant tenant = tenantRepository.findById(TenantId.of(id))
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        tenant.setStatus("ACTIVE");
        tenantRepository.save(tenant);
    }

    /**
     * 停用租户
     */
    @Transactional
    public void deactivateTenant(Long id) {
        log.info("Deactivating tenant: {}", id);

        Tenant tenant = tenantRepository.findById(TenantId.of(id))
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));

        tenant.setStatus("INACTIVE");
        tenantRepository.save(tenant);
    }

    /**
     * 检查租户配额
     */
    public boolean checkQuota(Long tenantId) {
        Tenant tenant = tenantRepository.findById(TenantId.of(tenantId))
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantId));

        // TODO: 获取当前链和组件数量并检查
        // 这里简化处理，实际需要从 repository 查询当前数量
        return true;
    }

    /**
     * 查询所有租户
     */
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    /**
     * 根据代码查询租户
     */
    public Tenant getTenantByCode(String tenantCode) {
        return tenantRepository.findByTenantCode(tenantCode)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + tenantCode));
    }
}
