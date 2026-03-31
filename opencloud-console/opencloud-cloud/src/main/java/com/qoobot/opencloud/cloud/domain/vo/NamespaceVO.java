package com.qoobot.opencloud.cloud.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Namespace VO
 */
@Data
public class NamespaceVO {

    private String name;
    private String status;
    private Map<String, String> labels;
    private Map<String, String> annotations;
    private LocalDateTime creationTimestamp;
}
