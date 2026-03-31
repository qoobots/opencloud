package com.qoobot.opencloud.auth.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qoobot.opencloud.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单/权限实体（树形结构）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
@Schema(description = "菜单/权限")
public class SysMenu extends BaseEntity {

    @Schema(description = "父级 ID（0=根节点）")
    private Long parentId;

    @Schema(description = "菜单/权限名称")
    private String menuName;

    @Schema(description = "类型：M=目录，C=菜单，B=按钮/权限")
    private String menuType;

    @Schema(description = "路由路径（前端）")
    private String path;

    @Schema(description = "组件路径（前端）")
    private String component;

    @Schema(description = "权限标识，如 system:user:list")
    private String permission;

    @Schema(description = "图标")
    private String icon;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否隐藏：0=显示，1=隐藏")
    private Integer hidden;

    @Schema(description = "状态：0=正常，1=禁用")
    private Integer status;
}
