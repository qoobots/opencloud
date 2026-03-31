package com.qoobot.opencloud.system.dict.controller;

import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.dict.domain.dto.DictDataDTO;
import com.qoobot.opencloud.system.dict.domain.dto.DictTypeDTO;
import com.qoobot.opencloud.system.dict.domain.vo.DictDataVO;
import com.qoobot.opencloud.system.dict.domain.vo.DictTypeVO;
import com.qoobot.opencloud.system.dict.service.DictService;
import com.qoobot.opencloud.system.log.annotation.SysLog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理接口
 */
@Tag(name = "字典管理")
@RestController
@RequestMapping("/api/system/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    // ── 字典类型 ──────────────────────────────────────────────

    @Operation(summary = "字典类型分页列表")
    @GetMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public R<PageResult<DictTypeVO>> pageDictTypes(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            @RequestParam(required = false) String keyword) {
        return R.ok(dictService.pageDictTypes(current, size, keyword));
    }

    @Operation(summary = "新增字典类型")
    @PostMapping("/types")
    @PreAuthorize("hasAuthority('system:dict:add')")
    @SysLog(module = "字典管理", action = "新增字典类型")
    public R<Void> createDictType(@Valid @RequestBody DictTypeDTO dto) {
        dictService.createDictType(dto);
        return R.ok();
    }

    @Operation(summary = "编辑字典类型")
    @PutMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    @SysLog(module = "字典管理", action = "编辑字典类型")
    public R<Void> updateDictType(@PathVariable Long id, @Valid @RequestBody DictTypeDTO dto) {
        dictService.updateDictType(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除字典类型")
    @DeleteMapping("/types/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    @SysLog(module = "字典管理", action = "删除字典类型")
    public R<Void> deleteDictType(@PathVariable Long id) {
        dictService.deleteDictType(id);
        return R.ok();
    }

    // ── 字典数据 ──────────────────────────────────────────────

    @Operation(summary = "字典数据列表（按 type 筛选）")
    @GetMapping("/data")
    @PreAuthorize("hasAuthority('system:dict:list')")
    public R<List<DictDataVO>> listDictData(@RequestParam String dictType) {
        return R.ok(dictService.listDictData(dictType));
    }

    @Operation(summary = "新增字典数据")
    @PostMapping("/data")
    @PreAuthorize("hasAuthority('system:dict:add')")
    @SysLog(module = "字典管理", action = "新增字典数据")
    public R<Void> createDictData(@Valid @RequestBody DictDataDTO dto) {
        dictService.createDictData(dto);
        return R.ok();
    }

    @Operation(summary = "编辑字典数据")
    @PutMapping("/data/{id}")
    @PreAuthorize("hasAuthority('system:dict:edit')")
    @SysLog(module = "字典管理", action = "编辑字典数据")
    public R<Void> updateDictData(@PathVariable Long id, @Valid @RequestBody DictDataDTO dto) {
        dictService.updateDictData(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除字典数据")
    @DeleteMapping("/data/{id}")
    @PreAuthorize("hasAuthority('system:dict:delete')")
    @SysLog(module = "字典管理", action = "删除字典数据")
    public R<Void> deleteDictData(@PathVariable Long id) {
        dictService.deleteDictData(id);
        return R.ok();
    }
}
