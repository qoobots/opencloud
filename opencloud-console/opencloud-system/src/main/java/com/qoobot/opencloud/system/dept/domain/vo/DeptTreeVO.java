package com.qoobot.opencloud.system.dept.domain.vo;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.qoobot.opencloud.system.common.util.TreeNode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 部门树节点 VO
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "部门树节点")
public class DeptTreeVO implements TreeNode<DeptTreeVO> {

    @Schema(description = "部门 ID")
    private Long id;

    @Schema(description = "父部门 ID")
    private Long parentId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "负责人")
    private String leader;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "同级排序")
    private Integer sort;

    @Schema(description = "子部门")
    private List<DeptTreeVO> children;

    @Override
    public int getSort() {
        return sort != null ? sort : 0;
    }
}
