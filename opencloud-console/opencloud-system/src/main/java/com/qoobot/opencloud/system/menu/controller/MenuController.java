package com.qoobot.opencloud.system.menu.controller;

import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.system.log.annotation.SysLog;
import com.qoobot.opencloud.system.menu.domain.dto.MenuCreateDTO;
import com.qoobot.opencloud.system.menu.domain.dto.MenuUpdateDTO;
import com.qoobot.opencloud.system.menu.domain.vo.MenuTreeVO;
import com.qoobot.opencloud.system.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理接口
 */
@Tag(name = "菜单管理")
@RestController
@RequestMapping("/api/system/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @Operation(summary = "全量菜单树（管理页，含按钮节点）")
    @GetMapping
    @PreAuthorize("hasAuthority('system:menu:list')")
    public R<List<MenuTreeVO>> getFullMenuTree() {
        return R.ok(menuService.getFullMenuTree());
    }

    @Operation(summary = "当前用户有权限的菜单树（不含按钮）")
    @GetMapping("/current")
    public R<List<MenuTreeVO>> getCurrentUserMenuTree() {
        return R.ok(menuService.getCurrentUserMenuTree());
    }

    @Operation(summary = "新增菜单/目录/按钮")
    @PostMapping
    @PreAuthorize("hasAuthority('system:menu:add')")
    @SysLog(module = "菜单管理", action = "新增菜单")
    public R<Void> createMenu(@Valid @RequestBody MenuCreateDTO dto) {
        menuService.createMenu(dto);
        return R.ok();
    }

    @Operation(summary = "编辑菜单")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:edit')")
    @SysLog(module = "菜单管理", action = "编辑菜单")
    public R<Void> updateMenu(@PathVariable Long id, @Valid @RequestBody MenuUpdateDTO dto) {
        menuService.updateMenu(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除菜单")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:menu:delete')")
    @SysLog(module = "菜单管理", action = "删除菜单")
    public R<Void> deleteMenu(@PathVariable Long id) {
        menuService.deleteMenu(id);
        return R.ok();
    }
}
