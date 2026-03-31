package com.qoobot.opencloud.system.menu.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qoobot.opencloud.system.common.util.TreeBuilder;
import com.qoobot.opencloud.system.exception.SystemException;
import com.qoobot.opencloud.system.menu.domain.dto.MenuCreateDTO;
import com.qoobot.opencloud.system.menu.domain.dto.MenuUpdateDTO;
import com.qoobot.opencloud.system.menu.domain.entity.SysMenu;
import com.qoobot.opencloud.system.menu.domain.vo.MenuTreeVO;
import com.qoobot.opencloud.system.menu.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 菜单管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class MenuServiceImpl implements MenuService {

    private final SysMenuMapper menuMapper;

    @Override
    public List<MenuTreeVO> getFullMenuTree() {
        List<SysMenu> all = menuMapper.selectList(
                new LambdaQueryWrapper<SysMenu>()
                        .eq(SysMenu::getDeleted, 0)
                        .orderByAsc(SysMenu::getSort)
        );
        List<MenuTreeVO> nodes = all.stream().map(this::toTreeVO).toList();
        return TreeBuilder.build(nodes, 0L);
    }

    @Override
    public List<MenuTreeVO> getCurrentUserMenuTree() {
        String userId = getCurrentUserId();
        if (userId == null) return List.of();

        List<SysMenu> menus = menuMapper.selectMenusByUserId(userId);
        // 排除按钮节点（type == 2）
        List<MenuTreeVO> nodes = menus.stream()
                .filter(m -> m.getMenuType() != 2)
                .map(this::toTreeVO)
                .toList();
        return TreeBuilder.build(nodes, 0L);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createMenu(MenuCreateDTO dto) {
        // 按钮类型不允许添加子节点（parentId 对应的节点 type 不能是 2）
        if (dto.getParentId() != null && dto.getParentId() > 0) {
            SysMenu parent = menuMapper.selectById(dto.getParentId());
            if (parent != null && parent.getMenuType() == 2) {
                throw SystemException.buttonNoChildren();
            }
        }

        // 校验权限标识唯一性
        if (dto.getPermission() != null && !dto.getPermission().isBlank()) {
            long count = menuMapper.selectCount(
                    new LambdaQueryWrapper<SysMenu>()
                            .eq(SysMenu::getPermission, dto.getPermission())
                            .eq(SysMenu::getDeleted, 0)
            );
            if (count > 0) throw SystemException.permissionExists();
        }

        SysMenu menu = new SysMenu();
        BeanUtils.copyProperties(dto, menu);
        menuMapper.insert(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Long id, MenuUpdateDTO dto) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) throw SystemException.menuNotFound();

        BeanUtils.copyProperties(dto, menu, "id");
        menuMapper.updateById(menu);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long id) {
        SysMenu menu = menuMapper.selectById(id);
        if (menu == null) throw SystemException.menuNotFound();

        // 校验无子节点
        int childCount = menuMapper.countChildren(id);
        if (childCount > 0) throw SystemException.menuHasChildren();

        menuMapper.deleteById(id);
    }

    private MenuTreeVO toTreeVO(SysMenu m) {
        MenuTreeVO vo = new MenuTreeVO();
        BeanUtils.copyProperties(m, vo);
        return vo;
    }

    private String getCurrentUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof String uid) return uid;
        return null;
    }
}
