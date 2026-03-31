package com.qoobot.opencloud.system.dict.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典数据表
 */
@Data
@TableName("sys_dict_data")
@Schema(description = "字典数据")
public class SysDictData implements Serializable {

    @Schema(description = "字典数据 ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "所属字典类型编码，关联 sys_dict_type.dict_type")
    private String dictType;

    @Schema(description = "显示名称（如「正常」）")
    private String dictLabel;

    @Schema(description = "字典值（如「ACTIVE」）")
    private String dictValue;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否默认选中")
    private Boolean isDefault;

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
