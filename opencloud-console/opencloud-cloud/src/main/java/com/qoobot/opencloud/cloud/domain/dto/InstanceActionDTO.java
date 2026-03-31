package com.qoobot.opencloud.cloud.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 云主机操作 DTO
 */
@Data
public class InstanceActionDTO {

    @NotBlank(message = "操作类型不能为空")
    @Pattern(regexp = "^(start|stop|reboot|hardReboot|pause|unpause|suspend|resume)$",
            message = "不支持的操作类型")
    private String action;

    private String type = "SOFT";
}
