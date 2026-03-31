package com.qoobot.opencloud.system.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户列表项 VO
 */
@Data
@Schema(description = "用户列表项")
public class UserVO {

    @Schema(description = "用户 ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号（脱敏）")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "是否内置账号")
    private Boolean isBuiltin;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
