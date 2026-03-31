package com.qoobot.opencloud.cloud.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 卷操作 DTO
 */
@Data
public class VolumeActionDTO {

    @NotBlank(message = "操作类型不能为空")
    @Pattern(regexp = "^(attach|detach|extend)$", message = "不支持的操作类型")
    private String action;

    private String instanceId;

    private String device;

    @Min(value = 1, message = "新大小必须大于当前大小")
    private Integer newSize;
}
