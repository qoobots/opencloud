package com.qoobot.opencloud.system.dict.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典数据 DTO（新增/编辑共用）
 */
@Data
@Schema(description = "字典数据请求")
public class DictDataDTO {

    @NotBlank(message = "字典类型编码不能为空")
    @Schema(description = "所属字典类型编码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictType;

    @NotBlank(message = "字典标签不能为空")
    @Schema(description = "显示名称（如「正常」）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Schema(description = "字典值（如「ACTIVE」）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictValue;

    @Schema(description = "排序", defaultValue = "0")
    private Integer sort;

    @Schema(description = "是否默认选中", defaultValue = "false")
    private Boolean isDefault;

    @Schema(description = "状态：ACTIVE / DISABLED", defaultValue = "ACTIVE")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
