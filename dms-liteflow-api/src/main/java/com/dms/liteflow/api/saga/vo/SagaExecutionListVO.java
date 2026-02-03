package com.dms.liteflow.api.saga.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Saga 执行列表 VO
 *
 * @author DMS
 * @since 2026-02-03
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SagaExecutionListVO {

    /**
     * 总数
     */
    private Long total;

    /**
     * 当前页码
     */
    private Long page;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 执行列表
     */
    private List<SagaExecutionListItemVO> items;
}
