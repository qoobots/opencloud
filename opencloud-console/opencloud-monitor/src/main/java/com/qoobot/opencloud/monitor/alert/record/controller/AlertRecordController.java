package com.qoobot.opencloud.monitor.alert.record.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qoobot.opencloud.common.domain.R;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertAckDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertRecordQueryDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertStatsQueryDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertRecordDetailVO;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertRecordVO;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertStatsVO;
import com.qoobot.opencloud.monitor.alert.record.service.AlertRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 告警记录控制器
 */
@RestController
@RequestMapping("/api/monitor/alerts/records")
@RequiredArgsConstructor
@Validated
public class AlertRecordController {

    private final AlertRecordService alertRecordService;

    /**
     * 分页查询告警记录
     */
    @GetMapping
    @PreAuthorize("hasAuthority('monitor:alert:record:list')")
    public R<IPage<AlertRecordVO>> list(AlertRecordQueryDTO queryDTO) {
        return R.ok(alertRecordService.listRecords(queryDTO));
    }

    /**
     * 获取告警记录详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('monitor:alert:record:list')")
    public R<AlertRecordDetailVO> getById(@PathVariable Long id) {
        return R.ok(alertRecordService.getRecordById(id));
    }

    /**
     * 确认告警
     */
    @PutMapping("/{id}/ack")
    @PreAuthorize("hasAuthority('monitor:alert:record:ack')")
    public R<Void> ack(@PathVariable Long id, @RequestBody AlertAckDTO ackDTO) {
        alertRecordService.ackRecord(id, ackDTO);
        return R.ok();
    }

    /**
     * 告警统计
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('monitor:alert:record:list')")
    public R<AlertStatsVO> stats(AlertStatsQueryDTO queryDTO) {
        return R.ok(alertRecordService.getStats(queryDTO));
    }
}
