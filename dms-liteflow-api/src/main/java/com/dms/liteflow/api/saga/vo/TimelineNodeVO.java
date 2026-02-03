package com.dms.liteflow.api.saga.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 时间线节点 VO
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineNodeVO {

    /**
     * 步骤ID
     */
    private String stepId;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 节点类型
     */
    private String nodeType;  // BUSINESS, COMPENSATION, CONDITION

    /**
     * 状态
     */
    private String status;

    /**
     * 是否补偿
     */
    private Boolean compensated;

    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime endTime;

    /**
     * 持续时间（毫秒）
     */
    private Long durationMs;

    /**
     * 错误信息
     */
    private String errorMessage;
}
