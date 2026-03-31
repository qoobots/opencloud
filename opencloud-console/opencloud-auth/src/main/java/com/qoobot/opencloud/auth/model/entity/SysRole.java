package com.qoobot.opencloud.auth.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qoobot.opencloud.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 角色实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
@Schema(description = "系统角色")
public class SysRole extends BaseEntity {

    @Schema(description = "角色名称")
    private String roleName;

    @Schema(description = "角色编码，如 ROLE_ADMIN")
    private String roleCode;

    @Schema(description = "角色描述")
    private String description;

    @Schema(description = "状态：0=正常，1=禁用")
    private Integer status;

    @Schema(description = "排序")
    private Integer sort;
}
