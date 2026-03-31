package com.qoobot.opencloud.system.role.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑角色 DTO
 */
@Data
@Schema(description = "编辑角色请求")
public class RoleUpdateDTO {

    @Size(max = 64)
    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "排序")
    private Integer sort;
}
