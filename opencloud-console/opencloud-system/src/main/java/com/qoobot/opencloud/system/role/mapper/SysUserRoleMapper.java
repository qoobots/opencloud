package com.qoobot.opencloud.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.system.role.domain.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户-角色关联 Mapper
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * 根据用户 ID 删除关联数据
     */
    int deleteByUserId(@Param("userId") String userId);

    /**
     * 根据角色 ID 查询用户 ID 列表
     */
    List<String> selectUserIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询用户已分配角色数量（删除角色前校验）
     */
    int countByRoleId(@Param("roleId") Long roleId);
}
