package com.qoobot.opencloud.system.menu.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 新增菜单/目录/按钮 DTO
 */
@Data
@Schema(description = "新增菜单请求")
public class MenuCreateDTO {

    @Schema(description = "父节点 ID（0=顶层）", defaultValue = "0")
    private Long parentId = 0L;

    @NotBlank(message = "菜单名称不能为空")
    @Schema(description = "菜单/按钮名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String menuName;

    @NotNull(message = "菜单类型不能为空")
    @Schema(description = "类型：0=目录 1=菜单 2=按钮", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer menuType;

    @Schema(description = "前端路由 path（目录/菜单有值）")
    private String path;

    @Schema(description = "前端组件路径（菜单有值）")
    private String component;

    @Schema(description = "菜单图标")
    private String icon;

    @Schema(description = "权限标识（按钮节点必填，如 system:user:add）")
    private String permission;

    @Schema(description = "是否在侧边栏显示", defaultValue = "true")
    private Boolean visible = true;

    @Schema(description = "同级排序", defaultValue = "0")
    private Integer sort = 0;

    @Schema(description = "备注")
    private String remark;
}
