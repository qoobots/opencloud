package com.qoobot.opencloud.system.dept.controller;

import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.system.dept.domain.dto.DeptCreateDTO;
import com.qoobot.opencloud.system.dept.domain.dto.DeptUpdateDTO;
import com.qoobot.opencloud.system.dept.domain.vo.DeptOptionVO;
import com.qoobot.opencloud.system.dept.domain.vo.DeptTreeVO;
import com.qoobot.opencloud.system.dept.service.DeptService;
import com.qoobot.opencloud.system.log.annotation.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理接口
 */
@Tag(name = "部门管理")
@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @Operation(summary = "部门树（全量）")
    @GetMapping
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<DeptTreeVO>> getDeptTree() {
        return R.ok(deptService.getDeptTree());
    }

    @Operation(summary = "部门下拉选项")
    @GetMapping("/options")
    @PreAuthorize("hasAuthority('system:dept:list')")
    public R<List<DeptOptionVO>> getOptions() {
        return R.ok(deptService.getOptions());
    }

    @Operation(summary = "新增部门")
    @PostMapping
    @PreAuthorize("hasAuthority('system:dept:add')")
    @SysLog(module = "部门管理", action = "新增部门")
    public R<Void> createDept(@Valid @RequestBody DeptCreateDTO dto) {
        deptService.createDept(dto);
        return R.ok();
    }

    @Operation(summary = "编辑部门")
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:edit')")
    @SysLog(module = "部门管理", action = "编辑部门")
    public R<Void> updateDept(@PathVariable Long id, @Valid @RequestBody DeptUpdateDTO dto) {
        deptService.updateDept(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除部门")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('system:dept:delete')")
    @SysLog(module = "部门管理", action = "删除部门")
    public R<Void> deleteDept(@PathVariable Long id) {
        deptService.deleteDept(id);
        return R.ok();
    }
}
