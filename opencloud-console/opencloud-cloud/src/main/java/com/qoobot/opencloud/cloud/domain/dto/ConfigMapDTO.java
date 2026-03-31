package com.qoobot.opencloud.cloud.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * ConfigMap DTO
 */
@Data
public class ConfigMapDTO {

    @NotBlank(message = "集群 ID 不能为空")
    private String clusterId;

    @NotBlank(message = "命名空间不能为空")
    private String namespace;

    @NotBlank(message = "名称不能为空")
    private String name;

    private Map<String, String> data;

    private Map<String, String> binaryData;

    private Boolean immutable;
}
