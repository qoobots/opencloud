package com.qoobot.opencloud.system.user.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 编辑用户基本信息 DTO
 */
@Data
@Schema(description = "编辑用户请求")
public class UserUpdateDTO {

    @Size(max = 64, message = "昵称最长 64 字符")
    @Schema(description = "显示昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "所属部门 ID")
    private Long deptId;

    @Schema(description = "排序")
    private Integer sort;
}
