package com.qoobot.opencloud.system.role.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色列表项 VO
 */
@Data
@Schema(description = "角色列表项")
public class RoleVO {

    @Schema(description = "角色 ID")
    private Long id;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码")
    private String roleCode;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "是否内置")
    private Boolean isBuiltin;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
