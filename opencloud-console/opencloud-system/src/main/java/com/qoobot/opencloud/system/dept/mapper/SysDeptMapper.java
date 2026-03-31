package com.qoobot.opencloud.system.dept.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.system.dept.domain.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 部门 Mapper
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {

    /**
     * 查询子部门数量（删除前校验）
     */
    int countChildren(@Param("parentId") Long parentId);

    /**
     * 查询部门下的用户数量（删除前校验）
     */
    int countUsersByDeptId(@Param("deptId") Long deptId);

    /**
     * 查询当前节点到根节点的层级深度
     */
    int countDepth(@Param("deptId") Long deptId);
}
