package com.qoobot.opencloud.monitor.notify.log.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qoobot.opencloud.monitor.notify.log.domain.entity.MonitorNotifyLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知日志 Mapper
 */
@Mapper
public interface NotifyLogMapper extends BaseMapper<MonitorNotifyLog> {
}
