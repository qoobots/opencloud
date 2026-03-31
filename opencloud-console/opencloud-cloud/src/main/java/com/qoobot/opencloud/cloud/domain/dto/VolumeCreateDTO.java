package com.qoobot.opencloud.cloud.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 卷创建 DTO
 */
@Data
public class VolumeCreateDTO {

    @NotBlank(message = "集群 ID 不能为空")
    private String clusterId;

    private String name;

    private String description;

    @NotNull(message = "卷大小不能为空")
    @Min(value = 1, message = "卷大小最小 1GB")
    private Integer size;

    private String volumeType;

    private String snapshotId;

    private String imageId;

    private Boolean multiattach = false;

    private Map<String, String> metadata;
}
