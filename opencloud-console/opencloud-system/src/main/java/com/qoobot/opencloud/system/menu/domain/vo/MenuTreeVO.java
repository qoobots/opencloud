package com.qoobot.opencloud.system.menu.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 菜单树节点 VO（实现 TreeNode 接口）
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "菜单树节点")
public class MenuTreeVO {

    @Schema(description = "菜单 ID")
    private Long id;

    @Schema(description = "父节点 ID")
    private Long parentId;

    @Schema(description = "菜单名称")
    private String menuName;

    @Schema(description = "类型：0=目录 1=菜单 2=按钮")
    private Integer menuType;

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

    @Schema(description = "子节点")
    private List<MenuTreeVO> children;

    @Override
    public int getSort() {
        return sort != null ? sort : 0;
    }
}
