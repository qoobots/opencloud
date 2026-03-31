package com.qoobot.opencloud.monitor.alert.record.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.common.util.SecurityUtils;
import com.qoobot.opencloud.common.util.TenantContext;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertAckDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertRecordQueryDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.dto.AlertStatsQueryDTO;
import com.qoobot.opencloud.monitor.alert.record.domain.entity.MonitorAlertRecord;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertRecordDetailVO;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertRecordVO;
import com.qoobot.opencloud.monitor.alert.record.domain.vo.AlertStatsVO;
import com.qoobot.opencloud.monitor.alert.record.mapper.AlertRecordMapper;
import com.qoobot.opencloud.monitor.alert.record.service.AlertRecordService;
import com.qoobot.opencloud.monitor.exception.MonitorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * 告警记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRecordServiceImpl implements AlertRecordService {

    private final AlertRecordMapper alertRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<AlertRecordVO> listRecords(AlertRecordQueryDTO queryDTO) {
        Page<MonitorAlertRecord> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        String tenantId = TenantContext.get();

        IPage<MonitorAlertRecord> entityPage = alertRecordMapper.selectPageVO(page, tenantId, queryDTO);

        return entityPage.convert(this::convertToVO);
    }

    @Override
    public AlertRecordDetailVO getRecordById(Long id) {
        MonitorAlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) {
            throw MonitorException.recordNotFound();
        }

        // 租户隔离检查
        if (!TenantContext.get().equals(record.getTenantId())) {
            throw MonitorException.recordNotFound();
        }

        return convertToDetailVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void ackRecord(Long id, AlertAckDTO ackDTO) {
        MonitorAlertRecord record = alertRecordMapper.selectById(id);
        if (record == null) {
            throw MonitorException.recordNotFound();
        }

        // 租户隔离检查
        if (!TenantContext.get().equals(record.getTenantId())) {
            throw MonitorException.recordNotFound();
        }

        // 检查状态
        if ("RESOLVED".equals(record.getStatus())) {
            throw MonitorException.invalidStatusTransition();
        }

        if ("ACKNOWLEDGED".equals(record.getStatus())) {
            throw MonitorException.alreadyAcked();
        }

        // 更新状态
        record.setStatus("ACKNOWLEDGED");
        record.setAckBy(SecurityUtils.getCurrentUserId());
        record.setAckAt(LocalDateTime.now());
        record.setAckNote(ackDTO.getAckNote());

        alertRecordMapper.updateById(record);
    }

    @Override
    public AlertStatsVO getStats(AlertStatsQueryDTO queryDTO) {
        String tenantId = TenantContext.get();
        LocalDateTime beginTime;

        switch (queryDTO.getPeriod()) {
            case "7d":
                beginTime = LocalDateTime.now().minusDays(7);
                break;
            case "30d":
                beginTime = LocalDateTime.now().minusDays(30);
                break;
            case "today":
            default:
                beginTime = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);
                break;
        }

        return alertRecordMapper.selectStats(tenantId, beginTime);
    }

    /**
     * 转换为 VO
     */
    private AlertRecordVO convertToVO(MonitorAlertRecord record) {
        AlertRecordVO vo = new AlertRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    /**
     * 转换为详情 VO
     */
    private AlertRecordDetailVO convertToDetailVO(MonitorAlertRecord record) {
        AlertRecordDetailVO vo = new AlertRecordDetailVO();
        BeanUtils.copyProperties(record, vo);

        // 解析 labels
        if (record.getLabels() != null && !record.getLabels().isEmpty()) {
            try {
                Map<String, Object> labels = objectMapper.readValue(record.getLabels(),
                        new TypeReference<Map<String, Object>>() {});
                vo.setLabels(labels);
            } catch (JsonProcessingException e) {
                log.warn("解析 labels 失败: {}", e.getMessage());
            }
        }

        // 解析 annotations
        if (record.getAnnotations() != null && !record.getAnnotations().isEmpty()) {
            try {
                Map<String, Object> annotations = objectMapper.readValue(record.getAnnotations(),
                        new TypeReference<Map<String, Object>>() {});
                vo.setAnnotations(annotations);
            } catch (JsonProcessingException e) {
                log.warn("解析 annotations 失败: {}", e.getMessage());
            }
        }

        // TODO: 查询通知日志
        vo.setNotifyLogs(java.util.Collections.emptyList());

        return vo;
    }
}
