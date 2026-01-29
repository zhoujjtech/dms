package com.dms.liteflow.repository;

import com.dms.liteflow.model.entity.FlowChain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 流程链 Repository
 */
@Repository
public interface FlowChainRepository extends JpaRepository<FlowChain, Long> {

    /**
     * 根据流程代码查询
     */
    FlowChain findByChainCode(String chainCode);

    /**
     * 根据状态查询流程链列表
     */
    List<FlowChain> findByStatus(Integer status);

    /**
     * 根据配置类型查询流程链列表
     */
    List<FlowChain> findByConfigType(String configType);

    /**
     * 检查流程代码是否存在
     */
    boolean existsByChainCode(String chainCode);

    /**
     * 查询未删除的流程链
     */
    List<FlowChain> findByStatusAndDeletedAtIsNull(Integer status);
}
