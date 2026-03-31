package com.qoobot.opencloud.system.log.service;

import com.alibaba.excel.EasyExcel;
import com.qoobot.opencloud.system.common.util.PageResult;
import com.qoobot.opencloud.system.config.TenantContext;
import com.qoobot.opencloud.system.log.domain.dto.LoginLogQueryDTO;
import com.qoobot.opencloud.system.log.domain.dto.OperationLogQueryDTO;
import com.qoobot.opencloud.system.log.domain.entity.SysOperationLog;
import com.qoobot.opencloud.system.log.domain.vo.LoginLogVO;
import com.qoobot.opencloud.system.log.domain.vo.OperationLogVO;
import com.qoobot.opencloud.system.log.mapper.AuthLoginLogMapper;
import com.qoobot.opencloud.system.log.mapper.SysOperationLogMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 日志查询 Service（操作日志 + 登录日志）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LogQueryService {

    private final SysOperationLogMapper operationLogMapper;
    private final AuthLoginLogMapper    loginLogMapper;

    // ── 操作日志 ──────────────────────────────────────────────

    public PageResult<OperationLogVO> pageOperationLogs(long current, long size, OperationLogQueryDTO query) {
        String tenantId = TenantContext.getTenantId();
        Page<OperationLogVO> page = new Page<>(current, size);
        return PageResult.of(operationLogMapper.selectPageVO(page, query, tenantId));
    }

    public OperationLogVO getOperationLogById(Long id) {
        SysOperationLog entity = operationLogMapper.selectById(id);
        if (entity == null) return null;
        OperationLogVO vo = new OperationLogVO();
        org.springframework.beans.BeanUtils.copyProperties(entity, vo);
        return vo;
    }

    public void exportOperationLogs(OperationLogQueryDTO query, HttpServletResponse response) throws IOException {
        String tenantId = TenantContext.getTenantId();
        List<OperationLogVO> list = operationLogMapper.selectForExport(query, tenantId);
        writeExcel(response, list, OperationLogVO.class, "操作日志");
    }

    // ── 登录日志 ──────────────────────────────────────────────

    public PageResult<LoginLogVO> pageLoginLogs(long current, long size, LoginLogQueryDTO query) {
        String tenantId = TenantContext.getTenantId();
        Page<LoginLogVO> page = new Page<>(current, size);
        return PageResult.of(loginLogMapper.selectPageVO(page, query, tenantId));
    }

    public void exportLoginLogs(LoginLogQueryDTO query, HttpServletResponse response) throws IOException {
        String tenantId = TenantContext.getTenantId();
        List<LoginLogVO> list = loginLogMapper.selectForExport(query, tenantId);
        writeExcel(response, list, LoginLogVO.class, "登录日志");
    }

    // ── Excel 导出工具 ───────────────────────────────────────

    private <T> void writeExcel(HttpServletResponse response, List<T> data,
                                Class<T> clazz, String fileName) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String encodedName = URLEncoder.encode(fileName + ".xlsx", StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + encodedName);
        EasyExcel.write(response.getOutputStream(), clazz).sheet(fileName).doWrite(data);
    }
}
