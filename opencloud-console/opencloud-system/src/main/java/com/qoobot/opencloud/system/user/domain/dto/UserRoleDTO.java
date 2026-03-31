package com.qoobot.opencloud.system.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 分配用户角色 DTO
 */
@Data
@Schema(description = "分配用户角色请求")
public class UserRoleDTO {

    @Schema(description = "角色 ID 列表（传空列表表示清除所有角色）")
    private List<Long> roleIds;
}
