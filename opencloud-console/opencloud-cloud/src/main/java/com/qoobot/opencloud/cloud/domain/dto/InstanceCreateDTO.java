package com.qoobot.opencloud.cloud.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 云主机创建 DTO
 */
@Data
public class InstanceCreateDTO {

    @NotBlank(message = "云主机名称不能为空")
    @Size(min = 2, max = 255, message = "名称长度 2-255 字符")
    private String name;

    @NotBlank(message = "集群 ID 不能为空")
    private String clusterId;

    @NotBlank(message = "规格 ID 不能为空")
    private String flavorId;

    private String imageId;

    private Boolean bootFromVolume = false;

    @Min(value = 1, message = "卷大小最小 1GB")
    private Integer volumeSize;

    private String volumeType;

    @NotNull(message = "网络配置不能为空")
    @Size(min = 1, message = "至少指定一个网络")
    private List<String> networkIds;

    private List<String> securityGroupIds;

    private String keyPairName;

    private String userData;

    @Min(value = 1, message = "创建数量最小 1")
    @Max(value = 10, message = "创建数量最大 10")
    private Integer count = 1;

    private String description;
}
