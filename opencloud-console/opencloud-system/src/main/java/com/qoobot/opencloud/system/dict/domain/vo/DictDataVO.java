package com.qoobot.opencloud.system.dict.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 字典数据 VO
 */
@Data
@Schema(description = "字典数据")
public class DictDataVO {

    @Schema(description = "字典数据 ID")
    private Long id;

    @Schema(description = "所属字典类型编码")
    private String dictType;

    @Schema(description = "显示名称")
    private String dictLabel;

    @Schema(description = "字典值")
    private String dictValue;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "是否默认")
    private Boolean isDefault;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
