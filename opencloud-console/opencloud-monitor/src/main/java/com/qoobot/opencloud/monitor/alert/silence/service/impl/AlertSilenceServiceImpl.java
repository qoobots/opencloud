package com.qoobot.opencloud.monitor.alert.silence.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.common.util.SecurityUtils;
import com.qoobot.opencloud.common.util.TenantContext;
import com.qoobot.opencloud.monitor.alert.silence.domain.dto.AlertSilenceCreateDTO;
import com.qoobot.opencloud.monitor.alert.silence.domain.dto.AlertSilenceQueryDTO;
import com.qoobot.opencloud.monitor.alert.silence.domain.entity.MonitorAlertSilence;
import com.qoobot.opencloud.monitor.alert.silence.domain.vo.AlertSilenceVO;
import com.qoobot.opencloud.monitor.alert.silence.mapper.AlertSilenceMapper;
import com.qoobot.opencloud.monitor.alert.silence.service.AlertSilenceService;
import com.qoobot.opencloud.monitor.exception.MonitorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警静默服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertSilenceServiceImpl implements AlertSilenceService {

    private final AlertSilenceMapper alertSilenceMapper;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<AlertSilenceVO> listSilences(AlertSilenceQueryDTO queryDTO) {
        // 将过期的静默自动标记
        alertSilenceMapper.expireOutdated(LocalDateTime.now());

        LambdaQueryWrapper<MonitorAlertSilence> wrapper = new LambdaQueryWrapper<MonitorAlertSilence>()
                .eq(MonitorAlertSilence::getTenantId, TenantContext.get());

        if (StringUtils.hasText(queryDTO.getSilenceName())) {
            wrapper.like(MonitorAlertSilence::getSilenceName, queryDTO.getSilenceName());
        }
        if (StringUtils.hasText(queryDTO.getStatus())) {
            wrapper.eq(MonitorAlertSilence::getStatus, queryDTO.getStatus());
        }
        wrapper.orderByDesc(MonitorAlertSilence::getCreatedAt);

        Page<MonitorAlertSilence> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<MonitorAlertSilence> entityPage = alertSilenceMapper.selectPage(page, wrapper);
        return entityPage.convert(this::convertToVO);
    }

    @Override
    public AlertSilenceVO getSilenceById(Long id) {
        MonitorAlertSilence silence = getAndVerify(id);
        return convertToVO(silence);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertSilenceVO createSilence(AlertSilenceCreateDTO createDTO) {
        validateTime(createDTO.getStartAt(), createDTO.getEndAt());

        MonitorAlertSilence silence = new MonitorAlertSilence();
        silence.setTenantId(TenantContext.get());
        silence.setSilenceName(createDTO.getSilenceName());
        silence.setDescription(createDTO.getDescription());
        silence.setMatchLabels(createDTO.getMatchLabels());
        silence.setStartAt(createDTO.getStartAt());
        silence.setEndAt(createDTO.getEndAt());
        silence.setStatus("ACTIVE");
        silence.setCreatedBy(SecurityUtils.getCurrentUserId());

        alertSilenceMapper.insert(silence);
        log.info("创建告警静默: id={}, name={}, tenantId={}",
                silence.getId(), silence.getSilenceName(), silence.getTenantId());
        return convertToVO(silence);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlertSilenceVO updateSilence(Long id, AlertSilenceCreateDTO updateDTO) {
        MonitorAlertSilence silence = getAndVerify(id);

        if ("EXPIRED".equals(silence.getStatus())) {
            throw MonitorException.of("SILENCE_EXPIRED", "已过期的静默规则不可编辑");
        }

        validateTime(updateDTO.getStartAt(), updateDTO.getEndAt());

        silence.setSilenceName(updateDTO.getSilenceName());
        silence.setDescription(updateDTO.getDescription());
        silence.setMatchLabels(updateDTO.getMatchLabels());
        silence.setStartAt(updateDTO.getStartAt());
        silence.setEndAt(updateDTO.getEndAt());
        silence.setUpdatedBy(SecurityUtils.getCurrentUserId());

        alertSilenceMapper.updateById(silence);
        return convertToVO(silence);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void stopSilence(Long id) {
        MonitorAlertSilence silence = getAndVerify(id);

        if ("EXPIRED".equals(silence.getStatus())) {
            throw MonitorException.of("SILENCE_ALREADY_EXPIRED", "静默规则已经是过期状态");
        }

        silence.setStatus("EXPIRED");
        silence.setUpdatedBy(SecurityUtils.getCurrentUserId());
        alertSilenceMapper.updateById(silence);
        log.info("手动停止告警静默: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSilence(Long id) {
        MonitorAlertSilence silence = getAndVerify(id);

        if ("ACTIVE".equals(silence.getStatus())) {
            throw MonitorException.of("SILENCE_ACTIVE", "生效中的静默规则不可删除，请先停止");
        }

        alertSilenceMapper.deleteById(id);
        log.info("删除告警静默: id={}", id);
    }

    @Override
    public boolean isAlertSilenced(Map<String, String> labels) {
        String tenantId = TenantContext.get();
        if (tenantId == null) {
            return false;
        }

        List<MonitorAlertSilence> activeSilences =
                alertSilenceMapper.findActiveSilences(tenantId, LocalDateTime.now());

        for (MonitorAlertSilence silence : activeSilences) {
            if (matchesSilence(silence, labels)) {
                log.debug("告警命中静默规则: silenceId={}, silenceName={}",
                        silence.getId(), silence.getSilenceName());
                return true;
            }
        }
        return false;
    }

    // ==================== 私有方法 ====================

    /**
     * 检查 labels 是否满足静默规则的所有匹配条件（AND 语义）
     */
    private boolean matchesSilence(MonitorAlertSilence silence, Map<String, String> labels) {
        if (!StringUtils.hasText(silence.getMatchLabels())) {
            return false;
        }
        try {
            List<Map<String, String>> matchers = objectMapper.readValue(
                    silence.getMatchLabels(), new TypeReference<>() {});

            if (matchers == null || matchers.isEmpty()) {
                return false;
            }

            // 所有 matcher 均需满足（AND）
            for (Map<String, String> matcher : matchers) {
                String key = matcher.get("key");
                String value = matcher.get("value");
                if (key == null || value == null) {
                    continue;
                }
                String labelVal = labels.get(key);
                if (!value.equals(labelVal)) {
                    return false;
                }
            }
            return true;
        } catch (JsonProcessingException e) {
            log.warn("解析静默规则 matchLabels 失败: silenceId={}, error={}",
                    silence.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * 校验时间参数合法性
     */
    private void validateTime(LocalDateTime startAt, LocalDateTime endAt) {
        if (endAt.isBefore(startAt) || endAt.isEqual(startAt)) {
            throw MonitorException.of("INVALID_SILENCE_TIME", "静默结束时间必须晚于开始时间");
        }
    }

    /**
     * 查询并验证租户归属
     */
    private MonitorAlertSilence getAndVerify(Long id) {
        MonitorAlertSilence silence = alertSilenceMapper.selectById(id);
        if (silence == null) {
            throw MonitorException.of("SILENCE_NOT_FOUND", "静默规则不存在");
        }
        if (!TenantContext.get().equals(silence.getTenantId())) {
            throw MonitorException.of("SILENCE_NOT_FOUND", "静默规则不存在");
        }
        return silence;
    }

    /**
     * 实体转 VO
     */
    private AlertSilenceVO convertToVO(MonitorAlertSilence silence) {
        AlertSilenceVO vo = new AlertSilenceVO();
        BeanUtils.copyProperties(silence, vo);
        return vo;
    }
}
