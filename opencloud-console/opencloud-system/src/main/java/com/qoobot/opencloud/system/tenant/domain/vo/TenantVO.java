package com.qoobot.opencloud.system.tenant.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 租户列表项 VO
 */
@Data
@Schema(description = "租户列表项")
public class TenantVO {

    @Schema(description = "租户 ID")
    private String id;

    @Schema(description = "租户编码")
    private String tenantCode;

    @Schema(description = "租户名称")
    private String tenantName;

    @Schema(description = "联系人姓名")
    private String contactName;

    @Schema(description = "联系人邮箱")
    private String contactEmail;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "是否默认租户")
    private Boolean isDefault;

    @Schema(description = "有效期")
    private LocalDateTime expireTime;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
