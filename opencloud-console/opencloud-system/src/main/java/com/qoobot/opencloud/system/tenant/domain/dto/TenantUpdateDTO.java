package com.qoobot.opencloud.system.tenant.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 编辑租户 DTO
 */
@Data
@Schema(description = "编辑租户请求")
public class TenantUpdateDTO {

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Schema(description = "有效期（NULL=永久有效）")
    private LocalDateTime expireTime;

    @Schema(description = "备注")
    private String remark;
}
