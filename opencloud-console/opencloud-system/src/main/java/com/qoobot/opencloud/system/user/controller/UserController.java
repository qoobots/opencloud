package com.qoobot.opencloud.system.user.controller;

import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.log.annotation.SysLog;
import com.qoobot.opencloud.system.user.domain.dto.*;
import com.qoobot.opencloud.system.user.domain.vo.UserDetailVO;
import com.qoobot.opencloud.system.user.domain.vo.UserOptionVO;
import com.qoobot.opencloud.system.user.domain.vo.UserVO;
import com.qoobot.opencloud.system.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户管理接口
 */
@Tag(name = "用户管理")
@RestController
@RequestMapping("/api/system/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "用户分页列表")
    @GetMapping
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<PageResult<UserVO>> pageUsers(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            UserQueryDTO query) {
        return R.ok(userService.pageUsers(current, size, query));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<UserDetailVO> getUserById(@PathVariable String id) {
        return R.ok(userService.getUserById(id));
    }

    @Operation(summary = "用户下拉选项")
    @GetMapping("/options")
    @PreAuthorize("hasAuthority('system:user:list')")
    public R<List<UserOptionVO>> getOptions() {
        return R.ok(userService.getOptions());
    }

    @Operation(summary = "新增用户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:user:add')")
    @SysLog(module = "用户管理", action = "新增用户")
    public R<Void> createUser(@Valid @RequestBody UserCreateDTO dto) {
        userService.createUser(dto);
        return R.ok();
    }

    @Operation(summary = "编辑用户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @SysLog(module = "用户管理", action = "编辑用户")
    public R<Void> updateUser(@PathVariable String id, @Valid @RequestBody UserUpdateDTO dto) {
        userService.updateUser(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除用户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:user:delete')")
    @SysLog(module = "用户管理", action = "删除用户")
    public R<Void> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return R.ok();
    }

    @Operation(summary = "切换用户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:user:edit')")
    @SysLog(module = "用户管理", action = "切换用户状态")
    public R<Void> updateStatus(@PathVariable String id, @Valid @RequestBody UserStatusDTO dto) {
        userService.updateStatus(id, dto);
        return R.ok();
    }

    @Operation(summary = "重置用户密码")
    @PutMapping("/{id}/password")
    @PreAuthorize("hasAuthority('system:user:resetPassword')")
    @SysLog(module = "用户管理", action = "重置用户密码")
    public R<Void> resetPassword(@PathVariable String id, @Valid @RequestBody UserResetPasswordDTO dto) {
        userService.resetPassword(id, dto);
        return R.ok();
    }

    @Operation(summary = "分配用户角色")
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('system:user:assignRole')")
    @SysLog(module = "用户管理", action = "分配用户角色")
    public R<Void> assignRoles(@PathVariable String id, @RequestBody UserRoleDTO dto) {
        userService.assignRoles(id, dto);
        return R.ok();
    }
}
