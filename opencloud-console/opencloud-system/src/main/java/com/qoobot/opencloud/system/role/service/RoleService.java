package com.qoobot.opencloud.system.role.service;

import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.role.domain.dto.RoleCreateDTO;
import com.qoobot.opencloud.system.role.domain.dto.RoleMenuDTO;
import com.qoobot.opencloud.system.role.domain.dto.RoleUpdateDTO;
import com.qoobot.opencloud.system.role.domain.vo.RoleOptionVO;
import com.qoobot.opencloud.system.role.domain.vo.RoleVO;

import java.util.List;

/**
 * 角色管理 Service
 */
public interface RoleService {

    /** 分页查询角色 */
    PageResult<RoleVO> pageRoles(long current, long size, String keyword);

    /** 查询全量角色选项（下拉用） */
    List<RoleOptionVO> getOptions();

    /** 查询角色详情 */
    RoleVO getRoleById(Long id);

    /** 新增角色 */
    void createRole(RoleCreateDTO dto);

    /** 编辑角色 */
    void updateRole(Long id, RoleUpdateDTO dto);

    /** 删除角色 */
    void deleteRole(Long id);

    /** 分配菜单权限 */
    void assignMenus(Long id, RoleMenuDTO dto);

    /** 获取角色已分配的菜单 ID 列表 */
    List<Long> getMenuIds(Long id);
}
