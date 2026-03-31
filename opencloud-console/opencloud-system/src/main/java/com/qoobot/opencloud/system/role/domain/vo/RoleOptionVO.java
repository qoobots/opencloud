package com.qoobot.opencloud.system.role.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 角色下拉选项 VO
 */
@Data
@Schema(description = "角色下拉选项")
public class RoleOptionVO {

    @Schema(description = "角色 ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;
}
