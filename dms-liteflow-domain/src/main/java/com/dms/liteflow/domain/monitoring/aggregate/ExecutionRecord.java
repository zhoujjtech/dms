package com.dms.liteflow.domain.monitoring.aggregate;

import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 执行记录聚合根
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionRecord {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 租户ID
     */
    private TenantId tenantId;

    /**
     * 流程链ID
     */
    private ChainId chainId;

    /**
     * 组件ID（可为空）
     */
    private String componentId;

    /**
     * 执行ID
     */
    private String chainExecutionId;

    /**
     * 执行耗时(ms)
     */
    private Long executeTime;

    /**
     * 状态 (SUCCESS/FAILURE)
     */
    private String status;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 标记为成功
     */
    public void markAsSuccess() {
        this.status = "SUCCESS";
        this.errorMessage = null;
    }

    /**
     * 标记为失败
     */
    public void markAsFailure(String error) {
        this.status = "FAILURE";
        this.errorMessage = error;
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return "SUCCESS".equals(this.status);
    }

    /**
     * 检查是否失败
     */
    public boolean isFailure() {
        return "FAILURE".equals(this.status);
    }
}
