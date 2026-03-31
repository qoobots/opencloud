package com.qoobot.opencloud.system.role.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统角色表
 */
@Data
@TableName("sys_role")
@Schema(description = "系统角色")
public class SysRole implements Serializable {

    @Schema(description = "角色 ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属租户 ID")
    private String tenantId;

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码（如 SUPER_ADMIN / ROLE_ADMIN）")
    private String roleCode;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "是否内置角色（不可删除/修改 code）")
    private Boolean isBuiltin;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除")
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
