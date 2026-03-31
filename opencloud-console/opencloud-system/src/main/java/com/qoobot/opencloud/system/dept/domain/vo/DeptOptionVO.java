package com.qoobot.opencloud.system.dept.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 部门下拉选项 VO
 */
@Data
@Schema(description = "部门下拉选项")
public class DeptOptionVO {

    @Schema(description = "部门 ID")
    private Long id;

    @Schema(description = "部门名称")
    private String deptName;
}
