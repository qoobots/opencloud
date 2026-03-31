package com.qoobot.opencloud.system.menu.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 菜单/权限表
 * menu_type: 0=目录 1=菜单 2=按钮
 */
@Data
@TableName("sys_menu")
@Schema(description = "系统菜单/权限")
public class SysMenu implements Serializable {

    @Schema(description = "菜单 ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "父节点 ID（0=顶层）")
    private Long parentId;

    @Schema(description = "菜单/按钮名称")
    private String menuName;

    @Schema(description = "类型：0=目录 1=菜单 2=按钮")
    private Integer menuType;

    @Schema(description = "前端路由 path")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "权限标识（按钮节点必填，如 system:user:add）")
    private String permission;

    @Schema(description = "是否在侧边栏显示")
    private Boolean visible;

    @Schema(description = "同级排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

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
