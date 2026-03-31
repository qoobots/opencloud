package com.qoobot.opencloud.monitor.notify.channel.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.qoobot.opencloud.monitor.notify.channel.domain.dto.NotifyChannelCreateDTO;
import com.qoobot.opencloud.monitor.notify.channel.domain.dto.NotifyChannelQueryDTO;
import com.qoobot.opencloud.monitor.notify.channel.domain.vo.NotifyChannelVO;

import java.util.Map;

/**
 * 通知渠道服务接口
 */
public interface NotifyChannelService {

    IPage<NotifyChannelVO> listChannels(NotifyChannelQueryDTO queryDTO);

    NotifyChannelVO getChannelById(Long id);

    NotifyChannelVO createChannel(NotifyChannelCreateDTO createDTO);

    NotifyChannelVO updateChannel(Long id, NotifyChannelCreateDTO updateDTO);

    void deleteChannel(Long id);

    Map<String, Object> testChannel(Long id);
}
