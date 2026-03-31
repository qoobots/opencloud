package com.qoobot.opencloud.cloud.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 快照创建 DTO
 */
@Data
public class SnapshotCreateDTO {

    @NotBlank(message = "集群 ID 不能为空")
    private String clusterId;

    @NotBlank(message = "卷 ID 不能为空")
    private String volumeId;

    private String name;

    private String description;
}
