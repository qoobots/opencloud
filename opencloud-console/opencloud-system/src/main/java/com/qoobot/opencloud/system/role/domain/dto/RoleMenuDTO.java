package com.qoobot.opencloud.system.role.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分配角色菜单权限 DTO
 */
@Data
@Schema(description = "分配菜单权限请求")
public class RoleMenuDTO {

    @Schema(description = "菜单 ID 列表（传空列表表示清除所有权限）")
    private List<Long> menuIds;
}
