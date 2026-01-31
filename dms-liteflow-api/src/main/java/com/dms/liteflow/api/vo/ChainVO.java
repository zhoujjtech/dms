package com.dms.liteflow.api.vo;

import com.dms.liteflow.domain.flowexec.aggregate.FlowChain;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 流程链 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChainVO {
    private Long id;
    private Long tenantId;
    private String chainName;
    private String chainCode;
    private String description;
    private String configType;
    private String status;
    private Integer currentVersion;
    private Boolean transactional;
    private Integer transactionTimeout;
    private String transactionPropagation;

    public static ChainVO fromDomain(FlowChain chain) {
        return ChainVO.builder()
                .id(chain.getId())
                .tenantId(chain.getTenantId().getValue())
                .chainName(chain.getChainName())
                .chainCode(chain.getChainCode())
                .description(chain.getDescription())
                .configType(chain.getConfigType())
                .status(chain.getStatus().getCode())
                .currentVersion(chain.getCurrentVersion())
                .transactional(chain.getTransactional())
                .transactionTimeout(chain.getTransactionTimeout())
                .transactionPropagation(chain.getTransactionPropagation())
                .build();
    }
}
