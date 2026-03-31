package com.qoobot.opencloud.system.profile.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 修改个人信息 DTO
 */
@Data
@Schema(description = "修改个人信息请求")
public class ProfileUpdateDTO {

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;
}
