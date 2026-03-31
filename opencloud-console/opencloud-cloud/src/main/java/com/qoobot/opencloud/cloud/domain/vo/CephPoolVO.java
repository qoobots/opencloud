package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * Ceph 存储池 VO
 */
@Data
public class CephPoolVO {

    private Integer poolId;
    private String name;
    private Integer pgNum;
    private Integer pgPNum;
    private Integer size;
    private Integer minSize;
    private String crushRule;
    private String type;
    private Long storedBytes;
    private Long storedRawBytes;
    private Long maxBytes;
    private Double usagePercent;
    private Map<String, String> options;
}
