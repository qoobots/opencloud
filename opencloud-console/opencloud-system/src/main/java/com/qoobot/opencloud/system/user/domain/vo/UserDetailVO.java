package com.qoobot.opencloud.system.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户详情 VO（含角色列表）
 */
@Data
@Schema(description = "用户详情")
public class UserDetailVO {

    @Schema(description = "用户 ID")
    private String id;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否内置")
    private Boolean isBuiltin;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "已分配角色 ID 列表")
    private List<Long> roleIds;

    @Schema(description = "已分配角色名称列表")
    private List<String> roleNames;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}
