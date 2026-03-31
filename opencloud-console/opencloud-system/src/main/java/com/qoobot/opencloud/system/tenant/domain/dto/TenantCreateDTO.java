package com.qoobot.opencloud.system.tenant.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 新增租户 DTO
 */
@Data
@Schema(description = "新增租户请求")
public class TenantCreateDTO {

    @NotBlank(message = "租户编码不能为空")
    @Schema(description = "租户编码（全局唯一）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantCode;

    @NotBlank(message = "租户名称不能为空")
    @Schema(description = "租户名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tenantName;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Schema(description = "状态：ACTIVE / DISABLED", defaultValue = "ACTIVE")
    private String status;

    @Schema(description = "有效期（NULL=永久有效）")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;
}
