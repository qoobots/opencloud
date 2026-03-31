package com.qoobot.opencloud.system.tenant.controller;

import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.log.annotation.SysLog;
import com.qoobot.opencloud.system.tenant.domain.dto.TenantCreateDTO;
import com.qoobot.opencloud.system.tenant.domain.dto.TenantUpdateDTO;
import com.qoobot.opencloud.system.tenant.domain.vo.TenantVO;
import com.qoobot.opencloud.system.tenant.service.TenantService;
import com.qoobot.opencloud.system.user.domain.dto.UserStatusDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理接口（仅超级管理员）
 */
@Tag(name = "租户管理")
@RestController
@RequestMapping("/api/system/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    @Operation(summary = "租户分页列表（仅 SUPER_ADMIN）")
    @GetMapping
    @PreAuthorize("hasAuthority('system:tenant:list')")
    public R<PageResult<TenantVO>> pageTenants(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(tenantService.pageTenants(current, size, keyword));
    }

    @Operation(summary = "租户详情")
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('system:tenant:list')")
    public R<TenantVO> getTenantById(@PathVariable String id) {
        return R.ok(tenantService.getTenantById(id));
    }

    @Operation(summary = "租户下拉选项")
    @GetMapping("/options")
    @PreAuthorize("hasAuthority('system:tenant:list')")
    public R<List<TenantVO>> getOptions() {
        return R.ok(tenantService.getOptions());
    }

    @Operation(summary = "新增租户")
    @PostMapping
    @PreAuthorize("hasAuthority('system:tenant:add')")
    @SysLog(module = "租户管理", action = "新增租户")
    public R<Void> createTenant(@Valid @RequestBody TenantCreateDTO dto) {
        tenantService.createTenant(dto);
        return R.ok();
    }

    @Operation(summary = "编辑租户")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:tenant:edit')")
    @SysLog(module = "租户管理", action = "编辑租户")
    public R<Void> updateTenant(@PathVariable String id, @Valid @RequestBody TenantUpdateDTO dto) {
        tenantService.updateTenant(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除租户")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:tenant:delete')")
    @SysLog(module = "租户管理", action = "删除租户")
    public R<Void> deleteTenant(@PathVariable String id) {
        tenantService.deleteTenant(id);
        return R.ok();
    }

    @Operation(summary = "切换租户状态")
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('system:tenant:edit')")
    @SysLog(module = "租户管理", action = "切换租户状态")
    public R<Void> updateStatus(@PathVariable String id, @Valid @RequestBody UserStatusDTO dto) {
        tenantService.updateStatus(id, dto);
        return R.ok();
    }
}
