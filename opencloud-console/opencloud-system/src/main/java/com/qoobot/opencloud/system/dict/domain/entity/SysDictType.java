package com.qoobot.opencloud.system.dict.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典类型表
 */
@Data
@TableName("sys_dict_type")
@Schema(description = "字典类型")
public class SysDictType implements Serializable {

    @Schema(description = "字典类型 ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "字典类型编码（如 sys_user_status）")
    private String dictType;

    @Schema(description = "字典类型名称（如「用户状态」）")
    private String dictName;

    @Schema(description = "是否系统内置（不可删除）")
    private Boolean isSystem;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;

    @Schema(description = "备注")
    private String remark;

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
