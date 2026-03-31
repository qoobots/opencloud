package com.qoobot.opencloud.system.user.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统用户表（不含认证凭证，认证凭证由 auth 模块维护）
 */
@Data
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser implements Serializable {

    @Schema(description = "用户 ID（UUID）")
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @Schema(description = "所属租户 ID")
    private String tenantId;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "登录账号（同租户内唯一）")
    private String username;

    @Schema(description = "显示昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "是否内置账号（不可删除禁用）")
    private Boolean isBuiltin;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除（0=未删，1=已删）")
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
