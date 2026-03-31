package com.qoobot.opencloud.monitor.notify.dispatch;

import com.qoobot.opencloud.monitor.alert.record.domain.entity.MonitorAlertRecord;

/**
 * 通知分发服务接口
 * <p>
 * 根据告警记录和绑定的通知渠道列表，异步发送告警通知；支持最多 3 次重试。
 * </p>
 */
public interface NotifyDispatchService {

    /**
     * 异步发送告警触发通知
     *
     * @param record         告警记录
     * @param channelIdsJson 绑定的通知渠道 ID 列表（JSON 数组字符串，如 "[1,2,3]"）
     */
    void sendAsync(MonitorAlertRecord record, String channelIdsJson);

    /**
     * 异步发送告警恢复通知
     *
     * @param record         告警记录（status=RESOLVED）
     * @param channelIdsJson 绑定的通知渠道 ID 列表（JSON 数组字符串）
     */
    void sendResolvedAsync(MonitorAlertRecord record, String channelIdsJson);
}
