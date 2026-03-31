package com.qoobot.opencloud.system.menu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 编辑菜单 DTO
 */
@Data
@Schema(description = "编辑菜单请求")
public class MenuUpdateDTO {

    @Schema(description = "菜单/按钮名称")
    private String menuName;

    @Schema(description = "前端路由 path")
    private String path;

    @Schema(description = "前端组件路径")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "权限标识")
    private String permission;

    @Schema(description = "是否在侧边栏显示")
    private Boolean visible;

    @Schema(description = "同级排序")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;
}
