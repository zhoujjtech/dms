package com.dms.liteflow.infrastructure.persistence.mapper;

import com.dms.liteflow.domain.flowexec.entity.FlowSubChain;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 子流程 Mapper
 */
public interface FlowSubChainMapper {

    int insert(FlowSubChain subChain);

    int updateById(FlowSubChain subChain);

    FlowSubChain selectById(Long id);

    List<FlowSubChain> selectByTenantId(@Param("tenantId") Long tenantId);

    FlowSubChain selectByTenantIdAndName(
            @Param("tenantId") Long tenantId,
            @Param("subChainName") String subChainName
    );

    List<FlowSubChain> selectByParentChainId(@Param("parentChainId") Long parentChainId);

    int deleteById(@Param("id") Long id);
}
