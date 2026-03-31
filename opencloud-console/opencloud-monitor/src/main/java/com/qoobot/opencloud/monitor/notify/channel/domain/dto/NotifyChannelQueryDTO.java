package com.qoobot.opencloud.monitor.notify.channel.domain.dto;

import com.qoobot.opencloud.common.domain.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知渠道查询 DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class NotifyChannelQueryDTO extends PageQuery {

    private String channelName;

    private String channelType;
}
