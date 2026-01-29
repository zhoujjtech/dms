package com.dms.liteflow.repository;

import com.dms.liteflow.model.entity.RuleComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * 规则组件 Repository
 */
@Repository
public interface RuleComponentRepository extends JpaRepository<RuleComponent, Long> {

    /**
     * 根据组件ID查询
     */
    RuleComponent findByComponentId(String componentId);

    /**
     * 根据状态查询组件列表
     */
    java.util.List<RuleComponent> findByStatus(Integer status);

    /**
     * 根据组件类型查询组件列表
     */
    java.util.List<RuleComponent> findByComponentType(String componentType);

    /**
     * 检查组件ID是否存在
     */
    boolean existsByComponentId(String componentId);
}
