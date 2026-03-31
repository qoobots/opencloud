package com.qoobot.opencloud.monitor.notify.sender;

/**
 * 通知发送器策略接口
 */
public interface NotifySender {

    /**
     * 支持的渠道类型
     */
    String supportType();

    /**
     * 发送通知
     */
    void send(NotifyContext context) throws Exception;
}
