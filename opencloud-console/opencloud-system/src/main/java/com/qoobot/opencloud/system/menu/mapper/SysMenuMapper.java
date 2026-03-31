package com.qoobot.opencloud.system.menu.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.system.menu.domain.entity.SysMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 菜单 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询用户有权限的菜单（用于当前用户菜单树）
     * 路径：sys_user_role -> sys_role_menu -> sys_menu
     */
    List<SysMenu> selectMenusByUserId(@Param("userId") String userId);

    /**
     * 查询子菜单数量（删除前校验）
     */
    int countChildren(@Param("parentId") Long parentId);
}
