package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ConfigMap VO
 */
@Data
public class ConfigMapVO {

    private String name;
    private String namespace;
    private Map<String, String> data;
    private Map<String, String> binaryData;
    private Boolean immutable;
    private LocalDateTime creationTimestamp;
}
