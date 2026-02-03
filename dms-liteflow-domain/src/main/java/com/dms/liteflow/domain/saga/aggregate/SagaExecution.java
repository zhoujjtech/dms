package com.dms.liteflow.domain.saga.aggregate;

import com.dms.liteflow.domain.saga.entity.StepExecution;
import com.dms.liteflow.domain.saga.valueobject.SagaExecutionId;
import com.dms.liteflow.domain.saga.valueobject.SagaStatus;
import com.dms.liteflow.domain.saga.valueobject.StepId;
import com.dms.liteflow.domain.shared.kernel.valueobject.ChainId;
import com.dms.liteflow.domain.shared.kernel.valueobject.TenantId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Saga 执行实例聚合根
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecution {

    /**
     * 主键ID（数据库自增）
     */
    private Long id;

    /**
     * 执行ID（全局唯一）
     */
    private SagaExecutionId executionId;

    /**
     * 租户ID
     */
    private TenantId tenantId;

    /**
     * 流程链名称
     */
    private String chainName;

    /**
     * 执行状态
     */
    private SagaStatus status;

    /**
     * 当前步骤索引
     */
    private Integer currentStepIndex;

    /**
     * 失败原因
     */
    private String failureReason;

    /**
     * 输入数据（JSON）
     */
    private Map<String, Object> inputData;

    /**
     * 输出数据（JSON）
     */
    private Map<String, Object> outputData;

    /**
     * 执行栈（用于补偿）
     */
    private List<StepExecution> executionStack;

    /**
     * 开始时间
     */
    private LocalDateTime startedAt;

    /**
     * 完成时间
     */
    private LocalDateTime completedAt;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 步骤执行记录（按步骤顺序）
     */
    @Builder.Default
    private List<StepExecution> steps = new ArrayList<>();

    /**
     * 开始执行
     */
    public void start() {
        if (this.status != SagaStatus.PENDING) {
            throw new IllegalStateException("Cannot start saga in status: " + this.status);
        }
        this.status = SagaStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 完成执行
     */
    public void complete() {
        if (this.status != SagaStatus.RUNNING) {
            throw new IllegalStateException("Cannot complete saga in status: " + this.status);
        }
        this.status = SagaStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 执行失败
     */
    public void fail(String reason) {
        if (this.status != SagaStatus.RUNNING) {
            throw new IllegalStateException("Cannot fail saga in status: " + this.status);
        }
        this.status = SagaStatus.FAILED;
        this.failureReason = reason;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 开始补偿
     */
    public void startCompensating() {
        if (this.status != SagaStatus.FAILED && this.status != SagaStatus.MANUAL_INTERVENTION) {
            throw new IllegalStateException("Cannot start compensating in status: " + this.status);
        }
        this.status = SagaStatus.COMPENSATING;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 补偿完成
     */
    public void compensateComplete() {
        if (this.status != SagaStatus.COMPENSATING) {
            throw new IllegalStateException("Cannot complete compensation in status: " + this.status);
        }
        this.status = SagaStatus.COMPENSATED;
        this.completedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 标记为需要人工介入
     */
    public void markManualIntervention(String reason) {
        if (this.status != SagaStatus.FAILED && this.status != SagaStatus.RUNNING) {
            throw new IllegalStateException("Cannot mark manual intervention in status: " + this.status);
        }
        this.status = SagaStatus.MANUAL_INTERVENTION;
        this.failureReason = reason;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 添加步骤到执行栈
     */
    public void pushToStack(StepExecution step) {
        if (this.executionStack == null) {
            this.executionStack = new ArrayList<>();
        }
        this.executionStack.add(step);
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 从执行栈弹出步骤（LIFO）
     */
    public StepExecution popFromStack() {
        if (this.executionStack == null || this.executionStack.isEmpty()) {
            return null;
        }
        this.updatedAt = LocalDateTime.now();
        return this.executionStack.remove(this.executionStack.size() - 1);
    }

    /**
     * 获取执行栈大小
     */
    public int getStackSize() {
        return this.executionStack != null ? this.executionStack.size() : 0;
    }

    /**
     * 添加步骤
     */
    public void addStep(StepExecution step) {
        if (this.steps == null) {
            this.steps = new ArrayList<>();
        }
        this.steps.add(step);
        this.currentStepIndex = this.steps.size() - 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 获取指定步骤
     */
    public StepExecution getStep(StepId stepId) {
        if (this.steps == null) {
            return null;
        }
        return this.steps.stream()
                .filter(step -> step.getStepId().equals(stepId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 获取所有需要补偿的步骤
     */
    public List<StepExecution> getCompensatableSteps() {
        if (this.executionStack == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(this.executionStack);
    }

    /**
     * 检查是否可以补偿
     */
    public boolean canCompensate() {
        return this.status == SagaStatus.FAILED || this.status == SagaStatus.MANUAL_INTERVENTION;
    }

    /**
     * 检查是否已完成（成功或已补偿）
     */
    public boolean isTerminated() {
        return this.status == SagaStatus.COMPLETED ||
                this.status == SagaStatus.COMPENSATED ||
                this.status == SagaStatus.MANUAL_INTERVENTION;
    }

    /**
     * 获取执行时长（毫秒）
     */
    public Long getDurationMs() {
        if (this.startedAt == null) {
            return 0L;
        }
        LocalDateTime end = this.completedAt != null ? this.completedAt : LocalDateTime.now();
        return java.time.Duration.between(this.startedAt, end).toMillis();
    }

    /**
     * 设置输入数据
     */
    public void setInputData(Map<String, Object> inputData) {
        this.inputData = inputData != null ? new HashMap<>(inputData) : new HashMap<>();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 设置输出数据
     */
    public void setOutputData(Map<String, Object> outputData) {
        this.outputData = outputData != null ? new HashMap<>(outputData) : new HashMap<>();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 创建新的 SagaExecution 实例
     */
    public static SagaExecution create(TenantId tenantId, String chainName, Map<String, Object> inputData) {
        return SagaExecution.builder()
                .executionId(SagaExecutionId.generate())
                .tenantId(tenantId)
                .chainName(chainName)
                .status(SagaStatus.PENDING)
                .currentStepIndex(0)
                .inputData(inputData != null ? new HashMap<>(inputData) : new HashMap<>())
                .executionStack(new ArrayList<>())
                .steps(new ArrayList<>())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .version(1)
                .build();
    }
}
