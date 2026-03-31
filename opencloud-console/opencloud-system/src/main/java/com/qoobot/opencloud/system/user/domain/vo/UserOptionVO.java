package com.qoobot.opencloud.system.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户下拉选项 VO
 */
@Data
@Schema(description = "用户下拉选项")
public class UserOptionVO {

    @Schema(description = "用户 ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;
}
