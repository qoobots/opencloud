package com.qoobot.opencloud.system.dept.domain.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 编辑部门 DTO
 */
@Data
@Schema(description = "编辑部门请求")
public class DeptUpdateDTO {

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "负责人姓名")
    private String leader;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "联系邮箱")
    private String email;

    @Schema(description = "同级排序")
    private Integer sort;

    @Schema(description = "状态：ACTIVE / DISABLED")
    private String status;
}
