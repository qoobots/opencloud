package com.qoobot.opencloud.monitor.websocket.service;

import com.qoobot.opencloud.monitor.alert.record.domain.entity.MonitorAlertRecord;

/**
 * 告警推送服务接口
 */
public interface AlertPushService {

    /**
     * 广播告警触发事件
     */
    void broadcastAlertFiring(MonitorAlertRecord record);

    /**
     * 广播告警解除事件
     */
    void broadcastAlertResolved(MonitorAlertRecord record);

    /**
     * 广播自定义消息
     */
    void broadcast(String message);
}
