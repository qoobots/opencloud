package com.qoobot.opencloud.monitor.notify.channel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.monitor.notify.channel.domain.entity.MonitorNotifyChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知渠道 Mapper
 */
@Mapper
public interface NotifyChannelMapper extends BaseMapper<MonitorNotifyChannel> {
}
