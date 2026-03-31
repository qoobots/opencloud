package com.qoobot.opencloud.monitor.alert.rule.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.common.util.TenantContext;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleCreateDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleQueryDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.dto.AlertRuleStatusDTO;
import com.qoobot.opencloud.monitor.alert.rule.domain.entity.MonitorAlertRule;
import com.qoobot.opencloud.monitor.alert.rule.domain.vo.AlertRuleVO;
import com.qoobot.opencloud.monitor.alert.rule.mapper.AlertRuleMapper;
import com.qoobot.opencloud.monitor.alert.rule.service.AlertRuleService;
import com.qoobot.opencloud.monitor.exception.MonitorException;
import com.qoobot.opencloud.monitor.metrics.client.PrometheusClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 告警规则服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleMapper alertRuleMapper;
    private final PrometheusClient prometheusClient;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<AlertRuleVO> listRules(AlertRuleQueryDTO queryDTO) {
        Page<MonitorAlertRule> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        String tenantId = TenantContext.get();

        IPage<MonitorAlertRule> entityPage = alertRuleMapper.selectPageVO(page, tenantId, queryDTO);

        return entityPage.convert(this::convertToVO);
    }

    @Override
    public AlertRuleVO getRuleById(Long id) {
        MonitorAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || rule.getDeleted() == 1) {
            throw MonitorException.ruleNotFound();
        }
        // 租户隔离检查
        if (!TenantContext.get().equals(rule.getTenantId())) {
            throw MonitorException.ruleNotFound();
        }
        return convertToVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRuleVO createRule(AlertRuleCreateDTO createDTO) {
        String tenantId = TenantContext.get();

        // 检查规则名是否重复
        MonitorAlertRule existing = alertRuleMapper.findByRuleName(createDTO.getRuleName(), tenantId);
        if (existing != null) {
            throw MonitorException.ruleNameDuplicate();
        }

        // 校验 PromQL 语法
        try {
            prometheusClient.validateExpr(createDTO.getPromqlExpr());
        } catch (MonitorException e) {
            if (e.getCode() == 3002) {
                throw e;
            }
            // Prometheus 不可达时记录日志但不阻塞
            log.warn("Prometheus 不可达，跳过 PromQL 校验");
        }

        // 创建规则
        MonitorAlertRule rule = new MonitorAlertRule();
        BeanUtils.copyProperties(createDTO, rule);
        rule.setTenantId(tenantId);
        rule.setStatus("ENABLED");

        // 转换通知渠道 ID 列表为 JSON
        if (createDTO.getNotifyChannelIds() != null && !createDTO.getNotifyChannelIds().isEmpty()) {
            try {
                rule.setNotifyChannelIds(objectMapper.writeValueAsString(createDTO.getNotifyChannelIds()));
            } catch (JsonProcessingException e) {
                rule.setNotifyChannelIds("[]");
            }
        } else {
            rule.setNotifyChannelIds("[]");
        }

        alertRuleMapper.insert(rule);

        // TODO: 同步至 AlertManager
        syncToAlertManager();

        return convertToVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertRuleVO updateRule(Long id, AlertRuleCreateDTO updateDTO) {
        MonitorAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || rule.getDeleted() == 1) {
            throw MonitorException.ruleNotFound();
        }

        // 租户隔离检查
        String tenantId = TenantContext.get();
        if (!tenantId.equals(rule.getTenantId())) {
            throw MonitorException.ruleNotFound();
        }

        // 检查规则名是否重复（排除自己）
        if (!rule.getRuleName().equals(updateDTO.getRuleName())) {
            MonitorAlertRule existing = alertRuleMapper.findByRuleName(updateDTO.getRuleName(), tenantId);
            if (existing != null) {
                throw MonitorException.ruleNameDuplicate();
            }
        }

        // 校验 PromQL 语法
        try {
            prometheusClient.validateExpr(updateDTO.getPromqlExpr());
        } catch (MonitorException e) {
            if (e.getCode() == 3002) {
                throw e;
            }
            log.warn("Prometheus 不可达，跳过 PromQL 校验");
        }

        // 更新规则
        BeanUtils.copyProperties(updateDTO, rule);

        // 转换通知渠道 ID 列表为 JSON
        if (updateDTO.getNotifyChannelIds() != null && !updateDTO.getNotifyChannelIds().isEmpty()) {
            try {
                rule.setNotifyChannelIds(objectMapper.writeValueAsString(updateDTO.getNotifyChannelIds()));
            } catch (JsonProcessingException e) {
                rule.setNotifyChannelIds("[]");
            }
        } else {
            rule.setNotifyChannelIds("[]");
        }

        alertRuleMapper.updateById(rule);

        // TODO: 同步至 AlertManager
        syncToAlertManager();

        return convertToVO(rule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRule(Long id) {
        MonitorAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || rule.getDeleted() == 1) {
            throw MonitorException.ruleNotFound();
        }

        // 租户隔离检查
        if (!TenantContext.get().equals(rule.getTenantId())) {
            throw MonitorException.ruleNotFound();
        }

        alertRuleMapper.deleteById(id);

        // TODO: 同步至 AlertManager
        syncToAlertManager();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRuleStatus(Long id, AlertRuleStatusDTO statusDTO) {
        MonitorAlertRule rule = alertRuleMapper.selectById(id);
        if (rule == null || rule.getDeleted() == 1) {
            throw MonitorException.ruleNotFound();
        }

        // 租户隔离检查
        if (!TenantContext.get().equals(rule.getTenantId())) {
            throw MonitorException.ruleNotFound();
        }

        rule.setStatus(statusDTO.getStatus());
        alertRuleMapper.updateById(rule);

        // TODO: 同步至 AlertManager
        syncToAlertManager();
    }

    /**
     * 同步至 AlertManager
     */
    private void syncToAlertManager() {
        // TODO: 实现 AlertManager 配置刷新
        log.info("同步告警规则至 AlertManager");
    }

    /**
     * 转换为 VO
     */
    private AlertRuleVO convertToVO(MonitorAlertRule rule) {
        AlertRuleVO vo = new AlertRuleVO();
        BeanUtils.copyProperties(rule, vo);

        // 解析通知渠道 ID 列表
        if (rule.getNotifyChannelIds() != null && !rule.getNotifyChannelIds().isEmpty()) {
            try {
                List<Long> channelIds = objectMapper.readValue(rule.getNotifyChannelIds(),
                        new TypeReference<List<Long>>() {});
                vo.setNotifyChannelIds(channelIds);
            } catch (JsonProcessingException e) {
                vo.setNotifyChannelIds(List.of());
            }
        } else {
            vo.setNotifyChannelIds(List.of());
        }

        return vo;
    }
}
