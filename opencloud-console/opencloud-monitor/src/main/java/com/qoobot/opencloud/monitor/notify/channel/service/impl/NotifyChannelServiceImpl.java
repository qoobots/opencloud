package com.qoobot.opencloud.monitor.notify.channel.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qoobot.opencloud.common.util.TenantContext;
import com.qoobot.opencloud.monitor.alert.rule.mapper.AlertRuleMapper;
import com.qoobot.opencloud.monitor.exception.MonitorException;
import com.qoobot.opencloud.monitor.notify.channel.domain.dto.NotifyChannelCreateDTO;
import com.qoobot.opencloud.monitor.notify.channel.domain.dto.NotifyChannelQueryDTO;
import com.qoobot.opencloud.monitor.notify.channel.domain.entity.MonitorNotifyChannel;
import com.qoobot.opencloud.monitor.notify.channel.domain.vo.NotifyChannelVO;
import com.qoobot.opencloud.monitor.notify.channel.mapper.NotifyChannelMapper;
import com.qoobot.opencloud.monitor.notify.channel.service.NotifyChannelService;
import com.qoobot.opencloud.monitor.notify.sender.NotifyContext;
import com.qoobot.opencloud.monitor.notify.sender.NotifySender;
import com.qoobot.opencloud.monitor.util.AesEncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 通知渠道服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyChannelServiceImpl implements NotifyChannelService {

    /** 敏感字段名称集合 */
    private static final Set<String> SENSITIVE_FIELDS = Set.of(
            "smtpPass", "secret", "authorization", "token", "password");

    private final NotifyChannelMapper notifyChannelMapper;
    private final AlertRuleMapper alertRuleMapper;
    private final List<NotifySender> notifySenders;
    private final ObjectMapper objectMapper;

    @Override
    public IPage<NotifyChannelVO> listChannels(NotifyChannelQueryDTO queryDTO) {
        LambdaQueryWrapper<MonitorNotifyChannel> wrapper = new LambdaQueryWrapper<MonitorNotifyChannel>()
                .eq(MonitorNotifyChannel::getTenantId, TenantContext.get());

        if (StringUtils.hasText(queryDTO.getChannelName())) {
            wrapper.like(MonitorNotifyChannel::getChannelName, queryDTO.getChannelName());
        }
        if (StringUtils.hasText(queryDTO.getChannelType())) {
            wrapper.eq(MonitorNotifyChannel::getChannelType, queryDTO.getChannelType());
        }
        wrapper.orderByDesc(MonitorNotifyChannel::getCreatedAt);

        Page<MonitorNotifyChannel> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        IPage<MonitorNotifyChannel> entityPage = notifyChannelMapper.selectPage(page, wrapper);

        return entityPage.convert(this::convertToVO);
    }

    @Override
    public NotifyChannelVO getChannelById(Long id) {
        MonitorNotifyChannel channel = notifyChannelMapper.selectById(id);
        if (channel == null || channel.getDeleted() == 1) {
            throw MonitorException.channelNotFound();
        }
        if (!TenantContext.get().equals(channel.getTenantId())) {
            throw MonitorException.channelNotFound();
        }
        return convertToVO(channel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotifyChannelVO createChannel(NotifyChannelCreateDTO createDTO) {
        MonitorNotifyChannel channel = new MonitorNotifyChannel();
        channel.setTenantId(TenantContext.get());
        channel.setChannelName(createDTO.getChannelName());
        channel.setChannelType(createDTO.getChannelType());
        channel.setDescription(createDTO.getDescription());

        // 加密敏感字段并序列化 config
        channel.setConfig(encryptAndSerializeConfig(createDTO.getConfig()));

        notifyChannelMapper.insert(channel);
        return convertToVO(channel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotifyChannelVO updateChannel(Long id, NotifyChannelCreateDTO updateDTO) {
        MonitorNotifyChannel channel = notifyChannelMapper.selectById(id);
        if (channel == null || channel.getDeleted() == 1) {
            throw MonitorException.channelNotFound();
        }
        if (!TenantContext.get().equals(channel.getTenantId())) {
            throw MonitorException.channelNotFound();
        }

        channel.setChannelName(updateDTO.getChannelName());
        channel.setChannelType(updateDTO.getChannelType());
        channel.setDescription(updateDTO.getDescription());
        channel.setConfig(encryptAndSerializeConfig(updateDTO.getConfig()));

        notifyChannelMapper.updateById(channel);
        return convertToVO(channel);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteChannel(Long id) {
        MonitorNotifyChannel channel = notifyChannelMapper.selectById(id);
        if (channel == null || channel.getDeleted() == 1) {
            throw MonitorException.channelNotFound();
        }
        if (!TenantContext.get().equals(channel.getTenantId())) {
            throw MonitorException.channelNotFound();
        }

        // 检查是否被告警规则引用
        int refCount = alertRuleMapper.countByChannelId(id);
        if (refCount > 0) {
            throw MonitorException.channelInUse(refCount);
        }

        notifyChannelMapper.deleteById(id);
    }

    @Override
    public Map<String, Object> testChannel(Long id) {
        MonitorNotifyChannel channel = notifyChannelMapper.selectById(id);
        if (channel == null || channel.getDeleted() == 1) {
            throw MonitorException.channelNotFound();
        }
        if (!TenantContext.get().equals(channel.getTenantId())) {
            throw MonitorException.channelNotFound();
        }

        // 找到对应的发送器
        NotifySender sender = notifySenders.stream()
                .filter(s -> s.supportType().equalsIgnoreCase(channel.getChannelType()))
                .findFirst()
                .orElse(null);

        if (sender == null) {
            return Map.of("success", false, "message", "不支持的渠道类型: " + channel.getChannelType());
        }

        // 解密 config
        Map<String, Object> decryptedConfig = decryptConfig(channel.getConfig());

        NotifyContext ctx = NotifyContext.builder()
                .channel(channel)
                .decryptedConfig(decryptedConfig)
                .isTest(true)
                .testMessage("[OpenCloud] 这是一条测试通知消息，渠道配置验证成功")
                .build();

        try {
            sender.send(ctx);
            log.info("测试通知发送成功: channelId={}, type={}", id, channel.getChannelType());
            return Map.of("success", true, "message", "发送成功");
        } catch (Exception e) {
            log.warn("测试通知发送失败: channelId={}, error={}", id, e.getMessage());
            return Map.of("success", false, "message", "发送失败: " + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /**
     * 加密敏感字段并序列化 config 为 JSON
     */
    private String encryptAndSerializeConfig(Map<String, Object> configMap) {
        if (configMap == null) {
            return "{}";
        }
        Map<String, Object> encrypted = new HashMap<>(configMap);
        for (String field : SENSITIVE_FIELDS) {
            if (encrypted.containsKey(field) && encrypted.get(field) != null) {
                String plaintext = encrypted.get(field).toString();
                if (StringUtils.hasText(plaintext) && !AesEncryptUtil.isEncrypted(plaintext)) {
                    encrypted.put(field, AesEncryptUtil.encrypt(plaintext));
                }
            }
        }
        try {
            return objectMapper.writeValueAsString(encrypted);
        } catch (JsonProcessingException e) {
            log.error("序列化 config 失败", e);
            return "{}";
        }
    }

    /**
     * 解密 config
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> decryptConfig(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> configMap = objectMapper.readValue(configJson,
                    new TypeReference<Map<String, Object>>() {});
            for (String field : SENSITIVE_FIELDS) {
                if (configMap.containsKey(field) && configMap.get(field) != null) {
                    String encrypted = configMap.get(field).toString();
                    if (StringUtils.hasText(encrypted)) {
                        try {
                            configMap.put(field, AesEncryptUtil.decrypt(encrypted));
                        } catch (Exception e) {
                            log.warn("解密字段 {} 失败，可能未加密", field);
                        }
                    }
                }
            }
            return configMap;
        } catch (JsonProcessingException e) {
            log.error("反序列化 config 失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 脱敏 config（敏感字段替换为 ****）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> desensitizeConfig(String configJson) {
        if (!StringUtils.hasText(configJson)) {
            return new HashMap<>();
        }
        try {
            Map<String, Object> configMap = objectMapper.readValue(configJson,
                    new TypeReference<Map<String, Object>>() {});
            for (String field : SENSITIVE_FIELDS) {
                if (configMap.containsKey(field) && configMap.get(field) != null) {
                    configMap.put(field, "****");
                }
            }
            return configMap;
        } catch (JsonProcessingException e) {
            return new HashMap<>();
        }
    }

    /**
     * 转换为 VO（config 脱敏）
     */
    private NotifyChannelVO convertToVO(MonitorNotifyChannel channel) {
        NotifyChannelVO vo = new NotifyChannelVO();
        BeanUtils.copyProperties(channel, vo);
        vo.setConfig(desensitizeConfig(channel.getConfig()));
        return vo;
    }
}
