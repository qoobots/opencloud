package com.qoobot.opencloud.system.dict.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 字典类型 DTO（新增/编辑共用）
 */
@Data
@Schema(description = "字典类型请求")
public class DictTypeDTO {

    @NotBlank(message = "字典类型编码不能为空")
    @Schema(description = "字典类型编码（如 sys_user_status）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictType;

    @NotBlank(message = "字典类型名称不能为空")
    @Schema(description = "字典类型名称（如「用户状态」）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String dictName;

    @Schema(description = "状态：ACTIVE / DISABLED", defaultValue = "ACTIVE")
    private String status;

    @Schema(description = "备注")
    private String remark;
}
