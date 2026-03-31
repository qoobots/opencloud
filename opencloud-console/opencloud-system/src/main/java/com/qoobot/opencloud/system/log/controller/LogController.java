package com.qoobot.opencloud.system.log.controller;

import com.qoobot.opencloud.common.result.R;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.log.domain.dto.LoginLogQueryDTO;
import com.qoobot.opencloud.system.log.domain.dto.OperationLogQueryDTO;
import com.qoobot.opencloud.system.log.domain.vo.LoginLogVO;
import com.qoobot.opencloud.system.log.domain.vo.OperationLogVO;
import com.qoobot.opencloud.system.log.service.LogQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * 日志查询接口（操作日志 + 登录日志）
 */
@Tag(name = "日志管理")
@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
public class LogController {

    private final LogQueryService logQueryService;

    // ── 操作日志 ──────────────────────────────────────────────

    @Operation(summary = "操作日志分页查询")
    @GetMapping("/operation")
    @PreAuthorize("hasAuthority('system:log:operation')")
    public R<PageResult<OperationLogVO>> pageOperationLogs(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            OperationLogQueryDTO query) {
        return R.ok(logQueryService.pageOperationLogs(current, size, query));
    }

    @Operation(summary = "操作日志详情")
    @GetMapping("/operation/{id}")
    @PreAuthorize("hasAuthority('system:log:operation')")
    public R<OperationLogVO> getOperationLogById(@PathVariable Long id) {
        return R.ok(logQueryService.getOperationLogById(id));
    }

    @Operation(summary = "操作日志导出 Excel")
    @GetMapping("/operation/export")
    @PreAuthorize("hasAuthority('system:log:operation')")
    public void exportOperationLogs(OperationLogQueryDTO query, HttpServletResponse response) throws IOException {
        logQueryService.exportOperationLogs(query, response);
    }

    // ── 登录日志 ──────────────────────────────────────────────

    @Operation(summary = "登录日志分页查询")
    @GetMapping("/login")
    @PreAuthorize("hasAuthority('system:log:login')")
    public R<PageResult<LoginLogVO>> pageLoginLogs(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "10") long size,
            LoginLogQueryDTO query) {
        return R.ok(logQueryService.pageLoginLogs(current, size, query));
    }

    @Operation(summary = "登录日志导出 Excel")
    @GetMapping("/login/export")
    @PreAuthorize("hasAuthority('system:log:login')")
    public void exportLoginLogs(LoginLogQueryDTO query, HttpServletResponse response) throws IOException {
        logQueryService.exportLoginLogs(query, response);
    }
}
