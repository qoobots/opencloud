package com.qoobot.opencloud.system.dict.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 字典类型 VO
 */
@Data
@Schema(description = "字典类型")
public class DictTypeVO {

    @Schema(description = "字典类型 ID")
    private Long id;

    @Schema(description = "字典类型编码")
    private String dictType;

    @Schema(description = "字典类型名称")
    private String dictName;

    @Schema(description = "是否系统内置")
    private Boolean isSystem;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
