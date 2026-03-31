package com.qoobot.opencloud.auth.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户角色关联表
 */
@Data
@TableName("sys_user_role")
@Schema(description = "用户角色关联")
public class SysUserRole {

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "角色 ID")
    private Long roleId;
}
