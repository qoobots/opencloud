package com.qoobot.opencloud.monitor.notify.log.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qoobot.opencloud.common.domain.R;
import com.qoobot.opencloud.common.util.TenantContext;
import com.qoobot.opencloud.monitor.notify.log.domain.entity.MonitorNotifyLog;
import com.qoobot.opencloud.monitor.notify.log.mapper.NotifyLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 通知日志控制器
 */
@RestController
@RequestMapping("/api/monitor/notify/logs")
@RequiredArgsConstructor
public class NotifyLogController {

    private final NotifyLogMapper notifyLogMapper;

    @GetMapping
    @PreAuthorize("hasAuthority('monitor:notify:channel:list')")
    public R<IPage<MonitorNotifyLog>> list(
            @RequestParam(required = false) Long alertRecordId,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beginTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {

        LambdaQueryWrapper<MonitorNotifyLog> wrapper = new LambdaQueryWrapper<MonitorNotifyLog>()
                .eq(MonitorNotifyLog::getTenantId, TenantContext.get());

        if (alertRecordId != null) wrapper.eq(MonitorNotifyLog::getAlertRecordId, alertRecordId);
        if (channelId != null) wrapper.eq(MonitorNotifyLog::getChannelId, channelId);
        if (status != null) wrapper.eq(MonitorNotifyLog::getStatus, status);
        if (beginTime != null) wrapper.ge(MonitorNotifyLog::getSentAt, beginTime);
        if (endTime != null) wrapper.le(MonitorNotifyLog::getSentAt, endTime);
        wrapper.orderByDesc(MonitorNotifyLog::getSentAt);

        return R.ok(notifyLogMapper.selectPage(new Page<>(page, size), wrapper));
    }
}
