package com.qoobot.opencloud.system.menu.service;

import com.qoobot.opencloud.system.menu.domain.dto.MenuCreateDTO;
import com.qoobot.opencloud.system.menu.domain.dto.MenuUpdateDTO;
import com.qoobot.opencloud.system.menu.domain.vo.MenuTreeVO;

import java.util.List;

/**
 * 菜单管理 Service
 */
public interface MenuService {

    /** 全量菜单树（管理页，含按钮节点） */
    List<MenuTreeVO> getFullMenuTree();

    /** 当前用户有权限的菜单树（不含按钮节点，type != 2） */
    List<MenuTreeVO> getCurrentUserMenuTree();

    /** 新增菜单/目录/按钮 */
    void createMenu(MenuCreateDTO dto);

    /** 编辑菜单 */
    void updateMenu(Long id, MenuUpdateDTO dto);

    /** 删除菜单 */
    void deleteMenu(Long id);
}
