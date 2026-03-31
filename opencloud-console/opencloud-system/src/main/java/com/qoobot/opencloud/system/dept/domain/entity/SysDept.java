package com.qoobot.opencloud.system.dept.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 部门表
 */
@Data
@TableName("sys_dept")
@Schema(description = "系统部门")
public class SysDept implements Serializable {

    @Schema(description = "部门 ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属租户 ID")
    private String tenantId;

    @Schema(description = "父部门 ID（0=根节点）")
    private Long parentId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "负责人姓名")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "联系邮箱")
    private String email;

    @Schema(description = "同级排序")
    private Integer sort;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @Schema(description = "逻辑删除")
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
