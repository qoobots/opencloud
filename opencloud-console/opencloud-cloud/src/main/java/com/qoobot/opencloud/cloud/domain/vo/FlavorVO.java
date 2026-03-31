package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.util.Map;

/**
 * 规格 VO
 */
@Data
public class FlavorVO {

    private String id;
    private String name;
    private Integer vcpus;
    private Integer ram;
    private Integer disk;
    private Integer ephemeral;
    private Integer swap;
    private Float rxTxFactor;
    private Boolean isPublic;
    private Map<String, String> extraSpecs;
}
