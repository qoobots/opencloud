package com.qoobot.opencloud.system.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户分页查询条件 DTO
 */
@Data
@Schema(description = "用户分页查询条件")
public class UserQueryDTO {

    @Schema(description = "用户名（模糊）")
    private String username;

    @Schema(description = "昵称（模糊）")
    private String nickname;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "部门 ID")
    private Long deptId;
}
