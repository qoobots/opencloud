package com.qoobot.opencloud.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 所有实体类的基类 —— 包含公共字段
 */
@Data
public abstract class BaseEntity implements Serializable {

    @Schema(description = "主键 ID")
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "创建人 ID")
    @TableField(fill = FieldFill.INSERT)
    private Long createBy;

    @Schema(description = "更新人 ID")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateBy;

    @Schema(description = "是否删除（逻辑删除：0=否，1=是）")
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
