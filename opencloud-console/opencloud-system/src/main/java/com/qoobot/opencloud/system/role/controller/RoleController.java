package com.qoobot.opencloud.system.role.controller;

import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.log.annotation.SysLog;
import com.qoobot.opencloud.system.role.domain.dto.RoleCreateDTO;
import com.qoobot.opencloud.system.role.domain.dto.RoleMenuDTO;
import com.qoobot.opencloud.system.role.domain.dto.RoleUpdateDTO;
import com.qoobot.opencloud.system.role.domain.vo.RoleOptionVO;
import com.qoobot.opencloud.system.role.domain.vo.RoleVO;
import com.qoobot.opencloud.system.role.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理接口
 */
@Tag(name = "角色管理")
@RestController
@RequestMapping("/api/system/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @Operation(summary = "角色分页列表")
    @GetMapping("/page")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<PageResult<RoleVO>> pageRoles(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(roleService.pageRoles(current, size, keyword));
    }

    @Operation(summary = "角色全量列表（下拉用）")
    @GetMapping
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<RoleOptionVO>> getOptions() {
        return R.ok(roleService.getOptions());
    }

    @Operation(summary = "角色详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<RoleVO> getRoleById(@PathVariable Long id) {
        return R.ok(roleService.getRoleById(id));
    }

    @Operation(summary = "新增角色")
    @PostMapping
    @PreAuthorize("hasAuthority('system:role:add')")
    @SysLog(module = "角色管理", action = "新增角色")
    public R<Void> createRole(@Valid @RequestBody RoleCreateDTO dto) {
        roleService.createRole(dto);
        return R.ok();
    }

    @Operation(summary = "编辑角色")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:edit')")
    @SysLog(module = "角色管理", action = "编辑角色")
    public R<Void> updateRole(@PathVariable Long id, @Valid @RequestBody RoleUpdateDTO dto) {
        roleService.updateRole(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:role:delete')")
    @SysLog(module = "角色管理", action = "删除角色")
    public R<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return R.ok();
    }

    @Operation(summary = "分配菜单权限")
    @PutMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:assignMenu')")
    @SysLog(module = "角色管理", action = "分配菜单权限")
    public R<Void> assignMenus(@PathVariable Long id, @RequestBody RoleMenuDTO dto) {
        roleService.assignMenus(id, dto);
        return R.ok();
    }

    @Operation(summary = "获取角色已分配菜单 ID 列表")
    @GetMapping("/{id}/menus")
    @PreAuthorize("hasAuthority('system:role:list')")
    public R<List<Long>> getMenuIds(@PathVariable Long id) {
        return R.ok(roleService.getMenuIds(id));
    }
}
