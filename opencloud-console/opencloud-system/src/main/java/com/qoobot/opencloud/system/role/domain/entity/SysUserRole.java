package com.qoobot.opencloud.system.role.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户-角色关联表
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_user_role")
@Schema(description = "用户-角色关联")
public class SysUserRole implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "用户 ID")
    private String userId;

    @Schema(description = "角色 ID")
    private Long roleId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
