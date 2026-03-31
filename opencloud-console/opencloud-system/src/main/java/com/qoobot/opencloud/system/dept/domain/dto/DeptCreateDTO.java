package com.qoobot.opencloud.system.dept.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 新增部门 DTO
 */
@Data
@Schema(description = "新增部门请求")
public class DeptCreateDTO {

    @Schema(description = "父部门 ID（0=根节点）", defaultValue = "0")
    private Long parentId = 0L;

    @NotBlank(message = "部门名称不能为空")
    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String deptName;

    @Schema(description = "负责人姓名")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "联系邮箱")
    private String email;

    @Schema(description = "同级排序", defaultValue = "0")
    private Integer sort = 0;

    @Schema(description = "状态：ACTIVE / DISABLED", defaultValue = "ACTIVE")
    private String status = "ACTIVE";
}
