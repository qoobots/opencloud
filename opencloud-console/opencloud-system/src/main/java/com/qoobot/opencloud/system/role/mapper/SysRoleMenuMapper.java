package com.qoobot.opencloud.system.role.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.system.role.domain.entity.SysRoleMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 角色-菜单关联 Mapper
 */
@Mapper
public interface SysRoleMenuMapper extends BaseMapper<SysRoleMenu> {

    /**
     * 查询角色已分配的菜单 ID 列表
     */
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    /**
     * 根据角色 ID 删除关联数据
     */
    int deleteByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询角色下的用户 ID 列表（角色菜单变更时批量清缓存用）
     */
    List<String> selectUserIdsByRoleId(@Param("roleId") Long roleId);
}
