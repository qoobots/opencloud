package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Secret VO
 */
@Data
public class SecretVO {

    private String name;
    private String namespace;
    private String type;
    private Map<String, String> data;
    private Map<String, String> stringData;
    private Boolean immutable;
    private LocalDateTime creationTimestamp;
}
