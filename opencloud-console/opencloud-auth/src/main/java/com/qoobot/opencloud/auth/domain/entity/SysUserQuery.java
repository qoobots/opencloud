package com.qoobot.opencloud.auth.domain.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * sys_user 查询实体（auth 模块专用，只含登录所需字段）
 *
 * <p>单体架构直接共享 system 模块的 sys_user 表，此处仅读取，不做写操作。
 */
@Data
@TableName("sys_user")
@Schema(description = "系统用户（auth 查询视图）")
public class SysUserQuery implements Serializable {

    @TableId
    @Schema(description = "用户 ID（Long）")
    private Long id;

    @Schema(description = "登录用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    /**
     * 状态：0=正常，1=禁用
     * 注意：禁用状态由 auth_user_credential.status 字段管理，
     *       此处 status 为 sys_user 维度的账号开关（可选检查）。
     */
    @Schema(description = "账号状态：0=正常，1=禁用")
    private Integer status;

    /**
     * 逻辑删除标记（0=未删除）
     */
    @Schema(description = "是否删除：0=否，1=是")
    private Integer deleted;
}
