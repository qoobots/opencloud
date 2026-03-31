package com.qoobot.opencloud.cloud.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Deployment 扩缩容 DTO
 */
@Data
public class ScaleDeploymentDTO {

    @NotBlank(message = "集群 ID 不能为空")
    private String clusterId;

    @NotBlank(message = "命名空间不能为空")
    private String namespace;

    @NotBlank(message = "Deployment 名称不能为空")
    private String name;

    @NotNull(message = "副本数不能为空")
    @Min(value = 0, message = "副本数不能小于 0")
    private Integer replicas;
}
