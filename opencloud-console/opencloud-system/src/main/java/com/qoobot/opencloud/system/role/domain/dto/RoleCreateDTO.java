package com.qoobot.opencloud.system.role.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 新增角色 DTO
 */
@Data
@Schema(description = "新增角色请求")
public class RoleCreateDTO {

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 64)
    @Schema(description = "角色名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleName;

    @NotBlank(message = "角色编码不能为空")
    @Size(max = 64)
    @Schema(description = "角色编码（如 ROLE_ADMIN）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roleCode;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态：ACTIVE / DISABLED", defaultValue = "ACTIVE")
    private String status;

    @Schema(description = "排序", defaultValue = "0")
    private Integer sort;
}
