package com.qoobot.opencloud.auth.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.qoobot.opencloud.common.entity.BaseEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser extends BaseEntity {

    @Schema(description = "用户名（登录名）")
    private String username;

    @Schema(description = "密码（BCrypt 加密）")
    private String password;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "状态：0=正常，1=禁用")
    private Integer status;

    @Schema(description = "备注")
    private String remark;
}
