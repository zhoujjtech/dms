package com.dms.liteflow.infrastructure.persistence.repository;

import com.dms.liteflow.domain.flowexec.entity.FlowSubChain;
import com.dms.liteflow.domain.flowexec.repository.FlowSubChainRepository;
import com.dms.liteflow.infrastructure.persistence.mapper.FlowSubChainMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 子流程仓储实现
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class FlowSubChainRepositoryImpl implements FlowSubChainRepository {

    private final FlowSubChainMapper flowSubChainMapper;

    @Override
    public FlowSubChain save(FlowSubChain subChain) {
        if (subChain.getId() == null) {
            flowSubChainMapper.insert(subChain);
        } else {
            flowSubChainMapper.updateById(subChain);
        }
        return subChain;
    }

    @Override
    public Optional<FlowSubChain> findById(Long id) {
        return Optional.ofNullable(flowSubChainMapper.selectById(id));
    }

    @Override
    public List<FlowSubChain> findByTenantId(com.dms.liteflow.domain.shared.kernel.valueobject.TenantId tenantId) {
        return flowSubChainMapper.selectByTenantId(tenantId.getValue());
    }

    @Override
    public Optional<FlowSubChain> findByTenantIdAndName(
            com.dms.liteflow.domain.shared.kernel.valueobject.TenantId tenantId,
            String subChainName
    ) {
        return Optional.ofNullable(flowSubChainMapper.selectByTenantIdAndName(
                tenantId.getValue(),
                subChainName
        ));
    }

    @Override
    public List<FlowSubChain> findByParentChainId(Long parentChainId) {
        return flowSubChainMapper.selectByParentChainId(parentChainId);
    }

    @Override
    public void deleteById(Long id) {
        flowSubChainMapper.deleteById(id);
    }
}
